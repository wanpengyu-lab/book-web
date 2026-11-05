package org.example.bookweb;

import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.example.bookweb.mapper.BorrowRecordMapper;
import org.example.bookweb.service.BorrowService;
import org.example.bookweb.service.BookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v0.6 共借推荐测试（PRD v0.6 验收）：连接真实 MySQL——共借 JOIN 统计语义 mock 不了。
 * 构造两人（夹具 99996/99997）先后借还 A、B 两书，断言 A 的 related 含 B；
 * 无共借数据的书必须退化「同分类最近入库 LIMIT 6」（不允许返回空列表）。
 * 清理只改状态（markReturned），不物理删除。
 * 注意：本测试需要真实数据库，./mvnw test 运行时必须提供 SPRING_DATASOURCE_PASSWORD。
 */
@SpringBootTest(properties = {
        "JWT_SECRET=test-jwt-secret-0123456789abcdef0123456789abcdef",
        "ADMIN_INIT_PASSWORD=test-init-pw"
})
class RelatedTest {

    private static final int BOOK_A = 1;
    private static final int BOOK_B = 2;
    /** 从未借出过的书（未分类，样本数据 id4-12 均无借阅记录），用于冷启动退化断言 */
    private static final int BOOK_COLD = 4;
    private static final List<Integer> FIXTURE_USERS = List.of(99996, 99997);

    @Autowired
    private BookService bookService;
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @BeforeEach
    @AfterEach
    void cleanup() {
        // 夹具借阅记录改为 returned、书恢复在馆（markReturned/setBorrowed 幂等，不物理删除；
        // 不走 service.returnBook——无在借记录时会抛 40901）
        for (int u : FIXTURE_USERS) {
            for (int b : List.of(BOOK_A, BOOK_B)) {
                borrowRecordMapper.markReturned(b, u);
            }
        }
        bookMapper.setBorrowed(false, BOOK_A);
        bookMapper.setBorrowed(false, BOOK_B);
    }

    @Test
    void coBorrowedBookAppearsInRelated() {
        // 两人各借还 A、B：构造共借（A 的借阅人群中 ≥2 人也借过 B）
        for (int u : FIXTURE_USERS) {
            borrowService.borrow(BOOK_A, u);
            borrowService.returnBook(BOOK_A, u);
            borrowService.borrow(BOOK_B, u);
            borrowService.returnBook(BOOK_B, u);
        }

        List<Integer> relatedOfA = bookService.getRelatedBookIds(BOOK_A);
        assertTrue(relatedOfA.contains(BOOK_B),
                "共借构造后 A 的 related 必须含 B，实际=" + relatedOfA);
        assertFalse(relatedOfA.contains(BOOK_A), "related 不得包含自身");
    }

    @Test
    void noCoBorrowFallsBackToSameCategoryRecent() {
        // BOOK_COLD 无任何共借数据 → 退化为同分类最近入库（LIMIT 6），不允许空列表
        List<Integer> related = bookService.getRelatedBookIds(BOOK_COLD);

        assertFalse(related.isEmpty(), "共借无数据必须退化同分类，不允许返回空列表");
        assertTrue(related.size() <= 6, "退化结果上限 6");
        assertFalse(related.contains(BOOK_COLD), "退化结果不得包含自身");

        String coldCategory = bookMapper.findById(BOOK_COLD).getCategory();
        for (int id : related) {
            Book b = bookMapper.findById(id);
            assertEquals(coldCategory, b.getCategory(),
                    "退化结果必须与原书同分类，id=" + id + " 实际=" + b.getCategory());
        }
    }
}
