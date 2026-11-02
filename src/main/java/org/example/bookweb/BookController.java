package org.example.bookweb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping("/books")
    public List<Book> listBooks(){
        return bookService.getAllBooks();}
    @GetMapping("/books/{id}")
    public Book getBook(@PathVariable int id){
        return bookService.getBookById(id);}
    @GetMapping("/books/page")
    public Map<String, Object> pageBooks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) throws Exception {
        return bookService.getBooksPage(page, size);
    }
    @PostMapping("/books")
    public Map<String, Object> addBook(@RequestBody Map<String,Object> body){
        int id = Integer.parseInt(String.valueOf(body.get("id")));
        String title = (String) body.get("title");
        String author = (String) body.get("author");
        Map<String,Object> result = new HashMap<>();
        result.put("success", bookService.addBook(id,title,author));
        return result;
    }
    @PutMapping("/books/{id}")
    public Map<String,Object> updateBook(@PathVariable int id,@RequestBody Map<String,Object> body){
        String title = (String) body.get("title");
        String author = (String) body.get("author");
        Map<String,Object> result = new HashMap<>();
        result.put("success",bookService.updateBook(id,title,author));
        return result;
    }

    @DeleteMapping ("/books/{id}")
    public Map<String,Object> deleteBook(@PathVariable int id){
        Map<String, Object> result = new HashMap<>();
        result.put("success", bookService.deleteBook(id));
        return result;
    }
    @PutMapping("/books/{id}/borrowed")
    public Map<String,Object> borrowedBook(@PathVariable int id){
        Map<String, Object> result = new HashMap<>();
        result.put("success", bookService.borrowBook(id));
        return result;
    }
    @PutMapping("/books/{id}/return")
    public Map<String,Object> returnBook(@PathVariable int id) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("success", bookService.returnBook(id));
        return result;
    }

}


