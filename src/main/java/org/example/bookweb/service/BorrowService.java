package org.example.bookweb.service;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.BorrowProperties;
import org.example.bookweb.dto.RecordPageResponse;
import org.example.bookweb.entity.BorrowRecord;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.mapper.BorrowRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借阅服务（v0.4 锁定方案，PRD 2.1/2.3）：
 * - 并发互斥只用条件更新 UPDATE book SET is_borrowed=1 WHERE id=? AND is_borrowed=0，影响行数=0 → 40901；
 *   禁止乐观锁 / SELECT FOR UPDATE；
 * - 借 / 还同一事务；due_at 由服务端按 BORROW_DAYS（默认 30 天）计算，禁止前端传入；
 * - 逾期是派生态（status='active' AND due_at &lt; NOW()），不落库、不加字段；
 * - 借阅记录只改状态，禁止物理删除。
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BorrowProperties borrowProperties;
    private final ReservationService reservationService;

    /** 图书不存在 → 40401 */
    public static class BookNotFoundException extends RuntimeException {
        public BookNotFoundException(String message) { super(message); }
    }

    /** 业务冲突（抢借失败 / 无在借记录可还）→ 40901 */
    public static class BorrowConflictException extends RuntimeException {
        public BorrowConflictException(String message) { super(message); }
    }

    /** reader 查他人记录 → 40301 */
    public static class RecordAccessDeniedException extends RuntimeException {
        public RecordAccessDeniedException(String message) { super(message); }
    }

    /** status 非法 → 40001 */
    public static class InvalidStatusException extends RuntimeException {
        public InvalidStatusException(String message) { super(message); }
    }

    /** 借出：条件更新抢锁 + 同事务插入在借记录（due_at = 借出时间 + BORROW_DAYS） */
    @Transactional
    public boolean borrow(int bookId, int userId) {
        if (bookMapper.findById(bookId) == null) {
            throw new BookNotFoundException("图书不存在");
        }
        // PRD 2.1 锁定方案：条件更新；影响行数=0（已被借走 / 并发抢锁失败）→ 40901
        int rows = bookMapper.borrowIfAvailable(bookId);
        if (rows == 0) {
            throw new BorrowConflictException("该书刚被借走");
        }
        borrowRecordMapper.insert(bookId, userId, LocalDateTime.now().plusDays(borrowProperties.getDays()));
        // v0.6 状态机闭环（同一事务）：借出人若有本人预约 → fulfilled
        reservationService.fulfillOnBorrow(bookId, userId);
        return true;
    }

    /** 归还：同事务内先改记录（行数=0 说明该用户无在借记录，禁止误放他人借出状态），再回写 is_borrowed=0；
     *  v0.6：同一事务内预约队首 waiting → notified 并生成站内通知（PRD v0.6 强制规范，禁止异步补通知） */
    @Transactional
    public boolean returnBook(int bookId, int userId) {
        int rows = borrowRecordMapper.markReturned(bookId, userId);
        if (rows == 0) {
            throw new BorrowConflictException("该书当前不在您的在借记录中，无法归还");
        }
        bookMapper.setBorrowed(false, bookId);
        reservationService.notifyQueueHead(bookId);
        return true;
    }

    /**
     * 记录查询：userId 为空查全部（仅 admin 场景）；reader 一律本人。
     * status = active / returned / overdue（overdue 为派生查询），空则不过滤。
     */
    public RecordPageResponse listRecords(Integer userId, String status, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 5;
        if (size > 100) size = 100;
        String st = normalizeStatus(status);
        int offset = (page - 1) * size;
        List<BorrowRecord> list = borrowRecordMapper.findByFilter(userId, st, size, offset);
        int total = borrowRecordMapper.countByFilter(userId, st);
        return RecordPageResponse.of(total, page, size, list);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!"active".equals(status) && !"returned".equals(status) && !"overdue".equals(status)) {
            throw new InvalidStatusException("status 仅支持 active/returned/overdue");
        }
        return status;
    }
}
