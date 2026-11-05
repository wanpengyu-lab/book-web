package org.example.bookweb;

import org.example.bookweb.dto.RecordPageResponse;
import org.example.bookweb.entity.BorrowRecord;
import org.example.bookweb.entity.User;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.mapper.BorrowRecordMapper;
import org.example.bookweb.mapper.UserMapper;
import org.example.bookweb.service.AuthService;
import org.example.bookweb.service.BorrowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v0.4 验收硬门槛：并发借阅（PRD v0.4）。连接真实 MySQL——行锁语义 mock 不了。
 * 两线程（admin / reader1 两个用户）同借一书，CountDownLatch 同时放行，
 * 断言恰一成功、另一人 40901「该书刚被借走」（条件更新互斥，PRD 2.1 锁定方案）。
 * 记录清理只改状态（markReturned / setBorrowed），不物理删除。
 * 注意：本测试需要真实数据库，./mvnw test 运行时必须提供 SPRING_DATASOURCE_PASSWORD。
 */
@SpringBootTest(properties = {
        "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef",
        "ADMIN_INIT_PASSWORD=test-init-pw"
})
class ConcurrentBorrowTest {

    private static final int BOOK_ID = 1;

    @Autowired
    private BorrowService borrowService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;
    @Autowired
    private AuthService authService;

    private int adminId;
    private int readerId;

    @BeforeEach
    void setUp() {
        // admin 由启动期 AdminInitializer 保证存在（test 属性提供 ADMIN_INIT_PASSWORD）
        adminId = userMapper.findByUsername("admin").getId();
        User reader = userMapper.findByUsername("reader1");
        if (reader == null) {
            authService.register("reader1", "reader-v03-pw");
            reader = userMapper.findByUsername("reader1");
        }
        readerId = reader.getId();
        // 清理上次运行可能残留的在借状态（只改状态，不物理删除）
        borrowRecordMapper.markReturned(BOOK_ID, adminId);
        borrowRecordMapper.markReturned(BOOK_ID, readerId);
        bookMapper.setBorrowed(false, BOOK_ID);
    }

    @AfterEach
    void tearDown() {
        borrowRecordMapper.markReturned(BOOK_ID, adminId);
        borrowRecordMapper.markReturned(BOOK_ID, readerId);
        bookMapper.setBorrowed(false, BOOK_ID);
    }

    @Test
    void twoUsersBorrowSameBookExactlyOneSucceeds() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicReference<Integer> winnerUserId = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(borrowTask(start, done, success, conflict, winnerUserId, adminId));
        pool.submit(borrowTask(start, done, success, conflict, winnerUserId, readerId));
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "两个线程应在 30s 内完成");
        pool.shutdown();

        assertEquals(1, success.get(), "两人同借一书必须恰有一人成功");
        assertEquals(1, conflict.get(), "另一人必须收到 40901 冲突");

        // 胜者的记录应为 active，且 due_at ≈ 借出时间 + 30 天（PRD 验收：due_at = 借出时间 + BORROW_DAYS）
        RecordPageResponse active = borrowService.listRecords(winnerUserId.get(), "active", 1, 10);
        BorrowRecord record = active.getList().stream()
                .filter(r -> r.getBookId() == BOOK_ID)
                .findFirst()
                .orElseThrow(() -> new AssertionError("胜者应有一条该书的 active 记录"));
        LocalDateTime now = LocalDateTime.now();
        assertTrue(record.getDueAt().isAfter(now.plusDays(29).minusMinutes(5))
                        && record.getDueAt().isBefore(now.plusDays(31)),
                "due_at 应为借出时间 + 30 天，实际 " + record.getDueAt());
        assertEquals(30, record.getDueAt().toLocalDate().toEpochDay()
                - record.getBorrowedAt().toLocalDate().toEpochDay(),
                "借期应为 30 天（默认 BORROW_DAYS）");
    }

    private Runnable borrowTask(CountDownLatch start, CountDownLatch done,
                                AtomicInteger success, AtomicInteger conflict,
                                AtomicReference<Integer> winnerUserId, int userId) {
        return () -> {
            try {
                start.await();
                borrowService.borrow(BOOK_ID, userId);
                success.incrementAndGet();
                winnerUserId.set(userId);
            } catch (BorrowService.BorrowConflictException e) {
                conflict.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        };
    }
}
