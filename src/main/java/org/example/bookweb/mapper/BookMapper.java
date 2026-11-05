package org.example.bookweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bookweb.entity.Book;

import java.util.List;

@Mapper
public interface BookMapper {
    @Select("SELECT id,title,author,book.is_borrowed AS borrowed FROM book ")
    List<Book> findAll();

    @Select("SELECT id,title,author,book.is_borrowed AS borrowed FROM book WHERE id = #{id}")
    Book findById(@Param("id") int id);

    @Insert("INSERT INTO book(title,author,is_borrowed) VALUES (#{title},#{author},0)")
    boolean add(@Param("title") String title, @Param("author") String author);

    @Delete("DELETE FROM book WHERE id = #{id}")
    boolean delete(@Param("id") int id);

    @Update("UPDATE book SET title = #{title},author = #{author} WHERE id = #{id}")
    boolean update(@Param("id") int id, @Param("title") String title, @Param("author") String author);

    @Update("UPDATE book SET is_borrowed = #{is_borrowed} WHERE id = #{id}")
    boolean setBorrowed(@Param("is_borrowed") boolean is_borrowed, @Param("id") int id);

    @Select("SELECT COUNT(*) FROM book")
    int countAll();

    @Select("SELECT id,title,author,is_borrowed AS borrowed FROM book LIMIT #{size} OFFSET #{offset}")
    List<Book> findPage(@Param("size") int size, @Param("offset") int offset);

    /**
     * v0.2 服务端搜索（修 G3）：keyword 同时匹配 title/author（LIKE 两端 %）；
     * 空/空白 keyword 不拼过滤条件（全库分页）。ORDER BY id 保证翻页顺序稳定。
     */
    @Select("""
            <script>
            SELECT id,title,author,is_borrowed AS borrowed FROM book
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

    /** v0.2 统计聚合（修 G2）：在馆/在借数量，全库口径。分类统计 SQL（GROUP BY category）待 v0.5 字段落地后补充 */
    @Select("SELECT COUNT(*) FROM book WHERE is_borrowed = #{borrowed}")
    int countByBorrowed(@Param("borrowed") boolean borrowed);
}
