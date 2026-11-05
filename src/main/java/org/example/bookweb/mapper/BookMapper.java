package org.example.bookweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bookweb.dto.DashboardResponse;
import org.example.bookweb.dto.StatsResponse;
import org.example.bookweb.entity.Book;
import org.example.bookweb.entity.BorrowRecord;

import java.util.List;

@Mapper
public interface BookMapper {
    @Select("""
            SELECT id,title,author,is_borrowed AS borrowed,category,publisher,isbn,
                   cover_url AS coverUrl,description,total_copies AS totalCopies
            FROM book
            """)
    List<Book> findAll();

    @Select("""
            SELECT id,title,author,is_borrowed AS borrowed,category,publisher,isbn,
                   cover_url AS coverUrl,description,total_copies AS totalCopies
            FROM book WHERE id = #{id}
            """)
    Book findById(@Param("id") int id);

    /** v0.5：新增/更新携带扩展字段；category/total_copies 的默认值由服务层归一化后传入 */
    @Insert("""
            INSERT INTO book(title,author,is_borrowed,category,publisher,isbn,cover_url,description,total_copies)
            VALUES (#{title},#{author},0,#{category},#{publisher},#{isbn},#{coverUrl},#{description},#{totalCopies})
            """)
    boolean add(@Param("title") String title, @Param("author") String author,
                @Param("category") String category, @Param("publisher") String publisher,
                @Param("isbn") String isbn, @Param("coverUrl") String coverUrl,
                @Param("description") String description, @Param("totalCopies") int totalCopies);

    @Delete("DELETE FROM book WHERE id = #{id}")
    boolean delete(@Param("id") int id);

    @Update("""
            UPDATE book SET title = #{title},author = #{author},category = #{category},
                   publisher = #{publisher},isbn = #{isbn},cover_url = #{coverUrl},
                   description = #{description},total_copies = #{totalCopies}
            WHERE id = #{id}
            """)
    boolean update(@Param("id") int id, @Param("title") String title, @Param("author") String author,
                   @Param("category") String category, @Param("publisher") String publisher,
                   @Param("isbn") String isbn, @Param("coverUrl") String coverUrl,
                   @Param("description") String description, @Param("totalCopies") int totalCopies);

    @Update("UPDATE book SET is_borrowed = #{is_borrowed} WHERE id = #{id}")
    boolean setBorrowed(@Param("is_borrowed") boolean is_borrowed, @Param("id") int id);

    /**
     * v0.4 借阅并发互斥（PRD 2.1 锁定方案）：条件更新，影响行数=0 表示书已被借走
     * （并发抢锁失败或已在借出状态）→ 由 BorrowService 抛 40901「该书刚被借走」。
     */
    @Update("UPDATE book SET is_borrowed = 1 WHERE id = #{id} AND is_borrowed = 0")
    int borrowIfAvailable(@Param("id") int id);

    @Select("SELECT COUNT(*) FROM book")
    int countAll();

    @Select("""
            SELECT id,title,author,is_borrowed AS borrowed,category,publisher,isbn,
                   cover_url AS coverUrl,description,total_copies AS totalCopies
            FROM book LIMIT #{size} OFFSET #{offset}
            """)
    List<Book> findPage(@Param("size") int size, @Param("offset") int offset);

    /**
     * v0.2 服务端搜索（修 G3）：keyword 同时匹配 title/author（LIKE 两端 %）；
     * 空/空白 keyword 不拼过滤条件（全库分页）。ORDER BY id 保证翻页顺序稳定。
     */
    @Select("""
            <script>
            SELECT id,title,author,is_borrowed AS borrowed,category,publisher,isbn,
                   cover_url AS coverUrl,description,total_copies AS totalCopies
            FROM book
            <where>
                <if test="keyword != null and keyword != ''">
                    (title LIKE CONCAT('%',#{keyword},'%') OR author LIKE CONCAT('%',#{keyword},'%'))
                </if>
            </where>
            ORDER BY id
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Book> search(@Param("keyword") String keyword, @Param("size") int size, @Param("offset") int offset);

    /** v0.2 搜索计数：与 search 同一过滤口径，total 为命中的全量条数 */
    @Select("""
            <script>
            SELECT COUNT(*) FROM book
            <where>
                <if test="keyword != null and keyword != ''">
                    (title LIKE CONCAT('%',#{keyword},'%') OR author LIKE CONCAT('%',#{keyword},'%'))
                </if>
            </where>
            </script>
            """)
    int countByKeyword(@Param("keyword") String keyword);

    /**
     * v0.5 分类精确过滤（PRD 交付物 findByCategory）：category 精确匹配，
     * keyword 可选（同时携带时组合过滤，B 阶段列表页会同传两者）。ORDER BY id 保证翻页顺序稳定。
     */
    @Select("""
            <script>
            SELECT id,title,author,is_borrowed AS borrowed,category,publisher,isbn,
                   cover_url AS coverUrl,description,total_copies AS totalCopies
            FROM book
            <where>
                category = #{category}
                <if test="keyword != null and keyword != ''">
                    AND (title LIKE CONCAT('%',#{keyword},'%') OR author LIKE CONCAT('%',#{keyword},'%'))
                </if>
            </where>
            ORDER BY id
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Book> findByCategory(@Param("category") String category, @Param("keyword") String keyword,
                              @Param("size") int size, @Param("offset") int offset);

    /** 与 findByCategory 同一过滤口径的计数（total 为筛选后全量） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM book
            <where>
                category = #{category}
                <if test="keyword != null and keyword != ''">
                    AND (title LIKE CONCAT('%',#{keyword},'%') OR author LIKE CONCAT('%',#{keyword},'%'))
                </if>
            </where>
            </script>
            """)
    int countByCategory(@Param("category") String category, @Param("keyword") String keyword);

    /** v0.5 统计聚合：在馆/在借数量，全库口径 */
    @Select("SELECT COUNT(*) FROM book WHERE is_borrowed = #{borrowed}")
    int countByBorrowed(@Param("borrowed") boolean borrowed);

    /** v0.5 真实分类统计：GROUP BY category 按 count 降序（替换 v0.2「全部」占位） */
    @Select("""
            SELECT category AS name, COUNT(*) AS `count`
            FROM book
            GROUP BY category
            ORDER BY `count` DESC, name
            """)
    List<StatsResponse.CategoryCount> categoryCounts();

    /** v0.5 详情聚合：某书最近 limit 条借阅记录（新→旧），一次请求随详情取齐 */
    @Select("""
            SELECT id, book_id AS bookId, user_id AS userId, borrowed_at AS borrowedAt,
                   due_at AS dueAt, returned_at AS returnedAt, status
            FROM borrow_record
            WHERE book_id = #{bookId}
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<BorrowRecord> recentRecords(@Param("bookId") int bookId, @Param("limit") int limit);

    /** v0.5 看板·趋势：近 30 天（含今天）每日借出量，只返回有借出的日期 */
    @Select("""
            SELECT DATE_FORMAT(borrowed_at, '%Y-%m-%d') AS date, COUNT(*) AS `count`
            FROM borrow_record
            WHERE borrowed_at >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)
            GROUP BY DATE_FORMAT(borrowed_at, '%Y-%m-%d')
            ORDER BY date
            """)
    List<DashboardResponse.TrendPoint> trend();

    /** v0.5 看板·分类占比：各分类藏书量，value 降序 */
    @Select("""
            SELECT category AS name, COUNT(*) AS value
            FROM book
            GROUP BY category
            ORDER BY value DESC, name
            """)
    List<DashboardResponse.CategoryRatio> categoryRatio();

    /** v0.5 看板·借阅排行：按借阅次数前 10（含已归还，借出即一次借阅行为） */
    @Select("""
            SELECT b.id, b.title, b.author, COUNT(*) AS `count`
            FROM borrow_record r
            JOIN book b ON b.id = r.book_id
            GROUP BY b.id, b.title, b.author
            ORDER BY `count` DESC, b.id
            LIMIT 10
            """)
    List<DashboardResponse.TopBorrowed> topBorrowed();

    /**
     * v0.6 共借推荐（PRD v0.6 内嵌 SQL 口径）：借过 #{id} 的人还借过哪些书，按共借人数降序前 6。
     * 注：PRD 原文 GROUP BY b2.book_id 的 b2 为笔误（语句中无 b2 别名），按语义修正为 r2.book_id。
     */
    @Select("""
            SELECT r2.book_id AS bookId, COUNT(*) AS cnt
            FROM borrow_record r1
            JOIN borrow_record r2 ON r1.user_id = r2.user_id AND r1.book_id != r2.book_id
            WHERE r1.book_id = #{id}
            GROUP BY r2.book_id
            ORDER BY cnt DESC, r2.book_id
            LIMIT 6
            """)
    List<Integer> relatedByCoBorrow(@Param("id") int id);

    /** v0.6 冷启动退化：同分类最近入库 LIMIT 6（排除自身）；共借无数据时必须走此路径，不允许返回空列表 */
    @Select("""
            SELECT id FROM book
            WHERE category = #{category} AND id != #{id}
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<Integer> recentIdsByCategory(@Param("category") String category,
                                      @Param("id") int id,
                                      @Param("limit") int limit);
}
