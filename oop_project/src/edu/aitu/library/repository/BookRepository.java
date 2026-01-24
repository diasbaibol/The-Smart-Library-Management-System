package edu.aitu.library.repository;

import edu.aitu.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookRepository {
    private final Connection conn;

    public BookRepository(Connection conn) {
        this.conn = conn;
    }

    public int addBook(String title, String author) throws SQLException {
        String sql = "INSERT INTO books(title, author, available) VALUES(?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to add book.");
    }

    public Book findById(int id) throws SQLException {
        String sql = "SELECT id, title, author, available FROM books WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available") == 1
                );
            }
        }
    }

    public List<Book> findAll() throws SQLException {
        String sql = "SELECT id, title, author, available FROM books ORDER BY id";
        List<Book> books = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available") == 1
                ));
            }
        }
        return books;
    }

    public void setAvailability(int bookId, boolean available) throws SQLException {
        String sql = "UPDATE books SET available = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        }
    }
}
