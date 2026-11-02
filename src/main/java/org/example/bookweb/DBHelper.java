package org.example.bookweb;
import java.sql.Connection;
import java.sql.DriverManager;


public class DBHelper  {
    private static final String URL = "jdbc:mysql://localhost:3306/jdbc_demo?characterEncoding=utf-8";
    private static final String USER = "root" ;
    private static final String PASSWORD = "123456" ;
    public static Connection getConnection() throws Exception{
        return DriverManager.getConnection(URL,USER,PASSWORD);
    }
}
