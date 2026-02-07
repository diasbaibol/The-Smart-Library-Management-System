package edu.aitu.library.repository;

import edu.aitu.library.model.Reservation;
import edu.aitu.library.model.ReservationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    private final Connection conn;

    public ReservationRepository(Connection conn) {
        this.conn = conn;
    }

    public boolean hasActiveReservation(int userId, int bookId) throws SQLException {
        String sql = """
            SELECT 1 FROM reservations
            WHERE user_id=? AND book_id=? AND status='ACTIVE'
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation findOldestActiveReservationForBook(int bookId) throws SQLException {
        String sql = """
            SELECT id,user_id,book_id,status,created_at,expires_at,fulfilled_loan_id
            FROM reservations
            WHERE book_id=? AND status='ACTIVE'
            ORDER BY id ASC
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

    public int createReservation(int userId, int bookId, LocalDate expiresAt) throws SQLException {
        String sql = """
            INSERT INTO reservations(user_id, book_id, status, created_at, expires_at, fulfilled_loan_id)
            VALUES(?, ?, 'ACTIVE', ?, ?, NULL)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.setString(3, LocalDate.now().toString());
            ps.setString(4, expiresAt.toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to create reservation.");
    }

    public void cancelReservation(int reservationId) throws SQLException {
        String sql = "UPDATE reservations SET status='CANCELLED' WHERE id=? AND status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    public void fulfillReservation(int reservationId, int loanId) throws SQLException {
        String sql = """
            UPDATE reservations
            SET status='FULFILLED', fulfilled_loan_id=?
            WHERE id=? AND status='ACTIVE'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }
    }

    public int expireOldReservations(LocalDate today) throws SQLException {
        String sql = """
            UPDATE reservations
            SET status='EXPIRED'
            WHERE status='ACTIVE' AND expires_at < ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today.toString());
            return ps.executeUpdate();
        }
    }

    public List<Reservation> listAll() throws SQLException {
        String sql = """
            SELECT id,user_id,book_id,status,created_at,expires_at,fulfilled_loan_id
            FROM reservations ORDER BY id DESC
        """;
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Reservation map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");
        ReservationStatus st = ReservationStatus.fromString(rs.getString("status"));
        LocalDate createdAt = LocalDate.parse(rs.getString("created_at"));
        LocalDate expiresAt = LocalDate.parse(rs.getString("expires_at"));
        int fulfilled = rs.getInt("fulfilled_loan_id");
        Integer fulfilledLoanId = rs.wasNull() ? null : fulfilled;
        return new Reservation(id, userId, bookId, st, createdAt, expiresAt, fulfilledLoanId);
    }
}
