package org.example.bookweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.bookweb.entity.User;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password_hash AS passwordHash, role, created_at AS createdAt
            FROM app_user WHERE username = #{username}
            """)
    User findByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM app_user WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    @Insert("INSERT INTO app_user(username, password_hash, role) VALUES(#{username}, #{passwordHash}, #{role})")
    int insert(@Param("username") String username,
               @Param("passwordHash") String passwordHash,
               @Param("role") String role);

    /** AdminInitializer 依据：库中是否存在 admin 角色账号 */
    @Select("SELECT COUNT(*) FROM app_user WHERE role = 'admin'")
    int countAdmin();
}
