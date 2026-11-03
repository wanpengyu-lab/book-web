package org.example.bookweb;


import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookMapper {
    @Select("SELECT id,title,author,book.is_borrowed AS borrowed FROM book ")
    List<Book>findAll();

    @Select("SELECT id,title,author,book.is_borrowed AS borrowed FROM book WHERE id = #{id}")
    Book findById(@Param("id") int id);

    @Insert("INSERT INTO book(id,title,author,is_borrowed) VALUES (#{id},#{title},#{author},0)")
    boolean add(@Param("id") int id,@Param("title") String title,@Param("author") String author);

    @Delete("DELETE FROM book WHERE id = #{id}")
    boolean delete(@Param("id") int id);

    @Update("UPDATE book SET title = #{title},author = #{author} WHERE id = #{id}")
    boolean update(@Param("id") int id,@Param("title") String title,@Param("author") String author);

    @Update("UPDATE book SET is_borrowed =#{is_borrowed} WHERE id = #{id}")
    boolean setBorrowed(@Param("is_borrowed") boolean is_borrowed,@Param("id") int id);

    @Select("SELECT COUNT(*) FROM book")
    int countAll();

    @Select("SELECT id,title,author,is_borrowed AS borrowed FROM book LIMIT #{size} OFFSET #{offset}")
    List<Book> findPage(@Param("size") int size,@Param("offset") int offset);

}
