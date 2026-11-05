package org.example.bookweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bookweb.entity.Notification;

import java.util.List;

@Mapper
public interface NotificationMapper {

    /** 站内通知插入（还书事务内预约提醒 / OverdueScanJob 到期提醒共用） */
    @Insert("INSERT INTO notification(user_id, content) VALUES(#{userId}, #{content})")
    int insert(@Param("userId") int userId, @Param("content") String content);

    /** 通知列表：未读优先（is_read 升序），同状态按最新在前 */
    @Select("""
            SELECT id, user_id AS userId, content, is_read AS `read`, created_at AS createdAt
            FROM notification
            WHERE user_id = #{userId}
            ORDER BY is_read ASC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<Notification> findByUser(@Param("userId") int userId,
                                  @Param("size") int size,
                                  @Param("offset") int offset);

    /** 与 findByUser 同口径计数 */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId}")
    int countByUser(@Param("userId") int userId);

    /** OverdueScanJob 同书同人当日去重：内容含书名且措辞确定，同人同书同日内容一致 */
    @Select("""
            SELECT COUNT(*) FROM notification
            WHERE user_id = #{userId} AND content = #{content} AND created_at >= CURDATE()
            """)
    int countTodayByUserAndContent(@Param("userId") int userId, @Param("content") String content);
}
