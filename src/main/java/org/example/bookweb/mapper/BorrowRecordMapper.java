package org.example.bookweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bookweb.entity.BorrowRecord;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BorrowRecordMapper {

    /** 借出时插入：borrowed_at/status 走表默认值（CURRENT_TIMESTAMP / 'active'）；due_at 由服务端计算后传入 */
    @Insert("INSERT INTO borrow_record(book_id, user_id, due_at) VALUES(#{bookId}, #{userId}, #{dueAt})")
    int insert(@Param("bookId") int bookId,
               @Param("userId") int userId,
               @Param("dueAt") LocalDateTime dueAt);

    /** 归还：回写 returned_at 并置 returned；只对借出人在借中的记录生效（PRD v0.4 核心实现） */
    @Update("""
            UPDATE borrow_record SET returned_at = NOW(), status = 'returned'
            WHERE book_id = #{bookId} AND user_id = #{userId} AND status = 'active'
            """)
    int markReturned(@Param("bookId") int bookId, @Param("userId") int userId);

    /**
     * 记录筛选查询：userId 为空查全部（admin），status 三态——
     * active → status='active'；returned → status='returned'；overdue → 派生 status='active' AND due_at &lt; NOW()。
     */
    @Select("""
            <script>
            SELECT id, book_id AS bookId, user_id AS userId, borrowed_at AS borrowedAt,
                   due_at AS dueAt, returned_at AS returnedAt, status
            FROM borrow_record
            <where>
                <if test="userId != null">user_id = #{userId}</if>
                <if test="status != null and status != ''">
                    <choose>
                        <when test="status == 'overdue'">AND status = 'active' AND due_at &lt; NOW()</when>
                        <otherwise>AND status = #{status}</otherwise>
                    </choose>
                </if>
            </where>
            ORDER BY id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<BorrowRecord> findByFilter(@Param("userId") Integer userId,
                                    @Param("status") String status,
                                    @Param("size") int size,
                                    @Param("offset") int offset);

    /** 与 findByFilter 同口径的计数（total 为筛选后全量） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM borrow_record
            <where>
                <if test="userId != null">user_id = #{userId}</if>
                <if test="status != null and status != ''">
                    <choose>
                        <when test="status == 'overdue'">AND status = 'active' AND due_at &lt; NOW()</when>
                        <otherwise>AND status = #{status}</otherwise>
                    </choose>
                </if>
            </where>
            </script>
            """)
    int countByFilter(@Param("userId") Integer userId,
                      @Param("status") String status);

    /** v0.6 OverdueScanJob 扫描：active 且 due_at 距今不足 3 天（未逾期、即将到期）的记录 */
    @Select("""
            SELECT id, book_id AS bookId, user_id AS userId, borrowed_at AS borrowedAt,
                   due_at AS dueAt, returned_at AS returnedAt, status
            FROM borrow_record
            WHERE status = 'active'
              AND due_at > NOW()
              AND due_at < DATE_ADD(NOW(), INTERVAL 3 DAY)
            ORDER BY id
            """)
    List<BorrowRecord> findDueSoon();
}
