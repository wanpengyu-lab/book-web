package org.example.bookweb.service;

import lombok.RequiredArgsConstructor;
import org.example.bookweb.dto.NotificationPageResponse;
import org.example.bookweb.entity.Book;
import org.example.bookweb.entity.Notification;
import org.example.bookweb.entity.Reservation;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.mapper.NotificationMapper;
import org.example.bookweb.mapper.ReservationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 预约与站内通知服务（v0.6，PRD 2.3 锁定）：
 * - 状态机 waiting → notified → fulfilled，取消为 cancelled；
 * - 在借状态才可约；重复预约（同人同书 waiting/notified）→ 40901；在馆书预约 → 40001；
 * - notifyQueueHead / fulfillOnBorrow 由 BorrowService 在借/还事务内调用，
 *   REQUIRED 传播加入既有事务——预约通知必须在还书事务内生成（PRD v0.6 强制规范）；
 * - 通知只做站内（notification 表）。
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final NotificationMapper notificationMapper;
    private final BookMapper bookMapper;

    /** 图书不存在 → 40401 */
    public static class BookNotFoundException extends RuntimeException {
        public BookNotFoundException(String message) { super(message); }
    }

    /** 在馆书预约 → 40001 */
    public static class InvalidReserveException extends RuntimeException {
        public InvalidReserveException(String message) { super(message); }
    }

    /** 重复预约（同人同书已有 waiting/notified）→ 40901 */
    public static class DuplicateReservationException extends RuntimeException {
        public DuplicateReservationException(String message) { super(message); }
    }

    /** 预约：在借状态才可约；重复预约 40901；在馆书 40001 */
    public boolean reserve(int bookId, int userId) {
        Book book = bookMapper.findById(bookId);
        if (book == null) {
            throw new BookNotFoundException("图书不存在");
        }
        if (!book.isBorrowed()) {
            throw new InvalidReserveException("图书在馆，可直接借阅，无需预约");
        }
        if (reservationMapper.countActive(bookId, userId) > 0) {
            throw new DuplicateReservationException("您已预约本书，请勿重复预约");
        }
        return reservationMapper.insert(bookId, userId);
    }

    /**
     * 还书事务内调用（PRD v0.6）：该书有 waiting 预约 → 队首（最早创建）置 notified 并生成站内通知。
     * 无 waiting 预约时静默返回。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyQueueHead(int bookId) {
        Reservation head = reservationMapper.findFirstWaiting(bookId);
        if (head == null) {
            return;
        }
        reservationMapper.updateStatus(head.getId(), "notified");
        notificationMapper.insert(head.getUserId(), reserveAvailableContent(bookId));
    }

    /**
     * borrow 事务内调用（PRD v0.6 状态机闭环）：借出人若有本人预约（notified 或 waiting）→ fulfilled。
     * waiting 一并闭环：等待者绕过通知直接借到书时，其预约同样已满足，避免残留 waiting 造成后续误通知。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int fulfillOnBorrow(int bookId, int userId) {
        return reservationMapper.fulfillForBorrower(bookId, userId);
    }

    /** 通知列表（登录用户本人）：未读优先，同状态最新在前，分页（size 上限 100） */
    public NotificationPageResponse listNotifications(int userId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 5;
        if (size > 100) size = 100;
        int offset = (page - 1) * size;
        List<Notification> list = notificationMapper.findByUser(userId, size, offset);
        int total = notificationMapper.countByUser(userId);
        return NotificationPageResponse.of(total, page, size, list);
    }

    private String reserveAvailableContent(int bookId) {
        Book book = bookMapper.findById(bookId);
        String title = book == null ? ("#" + bookId) : book.getTitle();
        return "您预约的《" + title + "》已归还，当前可借，请及时处理";
    }
}
