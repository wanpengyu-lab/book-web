package org.example.bookweb;

import org.example.bookweb.entity.Notification;
import org.example.bookweb.entity.Reservation;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.mapper.BorrowRecordMapper;
import org.example.bookweb.mapper.NotificationMapper;
import org.example.bookweb.mapper.ReservationMapper;
import org.example.bookweb.service.BorrowService;
import org.example.bookweb.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v0.6 预约状态机测试（PRD v0.6 验收硬门槛）：连接真实 MySQL——事务内队首通知语义 mock 不了。
 * 覆盖：在馆书预约 40001、重复预约 40901、waiting → notified（还书事务内队首 + 站内通知）
 * → fulfilled（借出事务内闭环）。
 * 夹具 user_id 用 99997/99998/99999（库中无对应账号，无外键，仅作数据标记）；
 * 清理只改状态（markReturned / cancelled），不物理删除。
 * 注意：本测试需要真实数据库，./mvnw test 运行时必须提供 SPRING_DATASOURCE_PASSWORD。
 */
@SpringBootTest(properties = {
        "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef",
        "ADMIN_INIT_PASSWORD=test-init-pw"
})
class ReservationTest {

    private static final int BOOK_ID = 1;
    private static final int U_A = 99997;
    private static final int U_B = 99998;
    private static final int U_HOLDER = 99999;
    private static final List<Integer> FIXTURE_USERS = List.of(U_A, U_B, U_HOLDER);

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;
    @Autowired
    private BookMapper bookMapper;

    @BeforeEach
    @AfterEach
    void cleanup() {
        // 夹具借阅记录改为 returned、书恢复在馆（is_borrowed 复位）；残留 waiting/notified 预约置 cancelled（不物理删除）
        for (int u : FIXTURE_USERS) {
            borrowRecordMapper.markReturned(BOOK_ID, u);
        }
        bookMapper.setBorrowed(false, BOOK_ID);
        for (int u : FIXTURE_USERS) {
            for (Reservation r : reservationMapper.findByBookAndUser(BOOK_ID, u)) {
                if ("waiting".equals(r.getStatus()) || "notified".equals(r.getStatus())) {
                    reservationMapper.updateStatus(r.getId(), "cancelled");
                }
            }
        }
    }

    @Test
    void reserveOnAvailableBookRejected40001() {
        // 书在馆（cleanup 已恢复 is_borrowed=0）→ 预约必须 40001
        assertThrows(ReservationService.InvalidReserveException.class,
                () -> reservationService.reserve(BOOK_ID, U_A),
                "在馆书预约必须 40001");
    }

    @Test
    void reserveMissingBookRejected40401() {
        assertThrows(ReservationService.BookNotFoundException.class,
                () -> reservationService.reserve(99999999, U_A),
                "不存在的图书预约必须 40401");
    }

    @Test
    void duplicateReservationRejected40901() {
        borrowService.borrow(BOOK_ID, U_HOLDER);
        reservationService.reserve(BOOK_ID, U_A);

        assertThrows(ReservationService.DuplicateReservationException.class,
                () -> reservationService.reserve(BOOK_ID, U_A),
                "同人同书已有 waiting 预约再约必须 40901");
    }

    @Test
    void stateMachineWaitingNotifiedFulfilled() {
        // 1. 借出（在借状态才可约）
        borrowService.borrow(BOOK_ID, U_HOLDER);

        // 2. 两人先后预约：A 先、B 后（队首按 created_at/id 升序）
        reservationService.reserve(BOOK_ID, U_A);
        reservationService.reserve(BOOK_ID, U_B);
        assertEquals("waiting", statusOf(U_A));
        assertEquals("waiting", statusOf(U_B));

        // 3. 重复预约 40901
        assertThrows(ReservationService.DuplicateReservationException.class,
                () -> reservationService.reserve(BOOK_ID, U_A));

        // 4. 归还 → 同一事务内队首 A 置 notified 并生成站内通知；B 仍 waiting
        borrowService.returnBook(BOOK_ID, U_HOLDER);
        assertEquals("notified", statusOf(U_A), "队首（最早创建）必须在还书事务内置 notified");
        assertEquals("waiting", statusOf(U_B), "非队首不得提前流转");
        List<Notification> notifications = notificationMapper.findByUser(U_A, 5, 0);
        assertTrue(notifications.stream()
                        .anyMatch(n -> !n.isRead() && n.getContent().contains("三体") && n.getContent().contains("已归还")),
                "队首用户必须收到未读的预约到货站内通知");

        // 5. notified 用户借出 → 其预约置 fulfilled（borrow 事务内）
        borrowService.borrow(BOOK_ID, U_A);
        assertEquals("fulfilled", statusOf(U_A), "notified 用户借出必须 fulfilled");

        // 6. 再归还 → 队首 B 置 notified（状态机可循环推进）
        borrowService.returnBook(BOOK_ID, U_A);
        assertEquals("notified", statusOf(U_B), "下一次归还应推进新队首 B");
    }

    private String statusOf(int userId) {
        // 取该用户最新一条预约（本次运行创建）；历史运行残留的夹具行只改状态不删除，不能取 get(0)
        List<Reservation> rows = reservationMapper.findByBookAndUser(BOOK_ID, userId);
        return rows.get(rows.size() - 1).getStatus();
    }
}
