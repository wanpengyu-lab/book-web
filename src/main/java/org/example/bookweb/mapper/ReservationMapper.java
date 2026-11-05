package org.example.bookweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bookweb.entity.Reservation;

import java.util.List;

@Mapper
public interface ReservationMapper {

    /** 预约插入：status 走表默认 'waiting'，created_at 走默认 CURRENT_TIMESTAMP（队首按其升序） */
    @Insert("INSERT INTO reservation(book_id, user_id) VALUES(#{bookId}, #{userId})")
    boolean insert(@Param("bookId") int bookId, @Param("userId") int userId);

    /** 重复预约判定：同人同书存在 waiting/notified 记录即重复（PRD v0.6 → 40901） */
    @Select("""
            SELECT COUNT(*) FROM reservation
            WHERE book_id = #{bookId} AND user_id = #{userId} AND status IN ('waiting', 'notified')
            """)
    int countActive(@Param("bookId") int bookId, @Param("userId") int userId);

    /** 队首：该书最早创建的 waiting 预约（created_at 升序，同秒以 id 兜底保证稳定） */
    @Select("""
            SELECT id, book_id AS bookId, user_id AS userId, status, created_at AS createdAt
            FROM reservation
            WHERE book_id = #{bookId} AND status = 'waiting'
            ORDER BY created_at ASC, id ASC
            LIMIT 1
            """)
    Reservation findFirstWaiting(@Param("bookId") int bookId);

    /** 状态机流转（还书事务内 waiting→notified；取消为 cancelled） */
    @Update("UPDATE reservation SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") int id, @Param("status") String status);

    /** 借出人在 borrow 事务内：其本人预约（waiting/notified）置 fulfilled（PRD v0.6 状态机闭环） */
    @Update("""
            UPDATE reservation SET status = 'fulfilled'
            WHERE book_id = #{bookId} AND user_id = #{userId} AND status IN ('waiting', 'notified')
            """)
    int fulfillForBorrower(@Param("bookId") int bookId, @Param("userId") int userId);

    /** 查询某人某书的预约（测试与排障用） */
    @Select("""
            SELECT id, book_id AS bookId, user_id AS userId, status, created_at AS createdAt
            FROM reservation
            WHERE book_id = #{bookId} AND user_id = #{userId}
            ORDER BY id
            """)
    List<Reservation> findByBookAndUser(@Param("bookId") int bookId, @Param("userId") int userId);
}
