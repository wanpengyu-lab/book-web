package org.example.bookweb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class BookDao {
    private static Map<String, Object> rowToMap(ResultSet rs) throws Exception {
        Map<String, Object> book = new HashMap<>();
        book.put("id", rs.getInt("id"));
        book.put("title", rs.getString("title"));
        book.put("author", rs.getString("author"));
        book.put("borrowed", rs.getInt("is_borrowed") == 1);
        return book;
    }

    public static List<Map<String, Object>> findAll() throws Exception {
        String sql = "SELECT id ,title ,author,is_borrowed FROM book ";
        Connection conn = DBHelper.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Map<String, Object>> books = new ArrayList<>();
        while (rs.next()) {
            books.add(rowToMap(rs));
        }
        rs.close();
        stmt.close();
        conn.close();
        return books;
    }

    public static List<Map<String, Object>> findById(int id) throws Exception {
        List<Map<String, Object>> books = new ArrayList<>();
        Connection conn = DBHelper.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT id,title,author,is_borrowed FROM book WHERE id = ?"
        );
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            books.add(rowToMap(rs));
        }
        rs.close();
        ps.close();
        conn.close();
        return books;
    }

    public static boolean add(int id, String title, String author) throws Exception {
        String sql = "INSERT INTO book (id,title,author,is_borrowed) VALUES (?,?,?,0)";
        Connection conn = DBHelper.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.setString(2, title);
        ps.setString(3, author);
        int rows = ps.executeUpdate();
        ps.close();
        conn.close();
        return rows > 0;
    }

    public static boolean update(int id, String title, String author) throws Exception {
        Connection conn = DBHelper.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE book SET title = ?,author = ? WHERE id = ?"
        );
        ps.setString(1, title);
        ps.setString(2, author);
        ps.setInt(3, id);
        int rows = ps.executeUpdate();
        ps.close();
        conn.close();
        return rows > 0;
    }

    public static boolean delete(int id) throws Exception {
        Connection conn = DBHelper.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM book WHERE id = ?"
        );
        ps.setInt(1, id);
        int rows = ps.executeUpdate();
        ps.close();
        conn.close();
        return rows > 0;
    }
    public static boolean setBorrowed(int id,boolean borrowed) throws Exception {
        Connection conn = DBHelper.getConnection();
        PreparedStatement p1 = conn.prepareStatement(
                "UPDATE book SET is_borrowed = ? WHERE id = ?");
        p1.setInt(1,borrowed ? 1 : 0);
        p1.setInt(2, id);
        int rows = p1.executeUpdate();
        p1.close();
        conn.close();
        return rows > 0;
    }
    public static int countAll() throws Exception {
        Connection conn = DBHelper.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM book");
        rs.next();
        int total = rs.getInt(1);
        rs.close();stmt.close();conn.close();
        return total;
    }
    public static List<Map<String,Object>> findPage (int offset,int size) throws Exception {
        List<Map<String, Object>> books = new LinkedList<>();
        Connection conn = DBHelper.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT id,title,author,is_borrowed FROM book LIMIT ? OFFSET ?");
        ps.setInt(1,size);
        ps.setInt(2,offset);
        ResultSet rs = ps.executeQuery();
        while (rs.next()){
            books.add(rowToMap(rs));
        }
        rs.close();ps.close();conn.close();
        return books;
    }
}