package edu.aitu.library.repository;

import edu.aitu.library.model.Loan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoanRepository {
    private final Connection conn;

    public LoanRepository(Connection conn) {
        this.conn = conn;
    }

    public int countOpenLoansByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM loans WHERE user_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        }
    }

    public boolean hasOpenLoanForBook(int bookId) throws SQLException {
        String sql = "SELECT 1 FROM loans WHERE book_id = ? AND return_date IS NULL LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int createLoan(int userId, int bookId) throws SQLException {
        String sql = "INSERT INTO loans(user_id, book_id, loan_date, return_date) VALUES(?, ?, ?, NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.setString(3, LocalDate.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to create loan.");
    }

    public void closeLoan(int loanId) throws SQLException {
        String sql = "UPDATE loans SET return_date = ? WHERE id = ? AND return_date IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, loanId);
            ps.executeUpdate();
        }
    }

    public Loan findOpenLoanByBook(int bookId) throws SQLException {
        String sql = """
            SELECT id, user_id, book_id, loan_date, return_date
            FROM loans
            WHERE book_id = ? AND return_date IS NULL
            ORDER BY id DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public List<Loan> listAllLoans() throws SQLException {
        String sql = "SELECT id, user_id, book_id, loan_date, return_date FROM loans ORDER BY id DESC";
        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) loans.add(map(rs));
        }
        return loans;
    }

    private Loan map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");
        LocalDate loanDate = LocalDate.parse(rs.getString("loan_date"));
        String returnDateStr = rs.getString("return_date");
        LocalDate returnDate = (returnDateStr == null) ? null : LocalDate.parse(returnDateStr);
        return new Loan(id, userId, bookId, loanDate, returnDate);
    }
}

