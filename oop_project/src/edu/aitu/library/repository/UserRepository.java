package edu.aitu.library.repository;

import edu.aitu.library.factory.UserFactory;
import edu.aitu.library.model.Role;
import edu.aitu.library.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final Connection conn;

    public UserRepository(Connection conn) {
        this.conn = conn;
    }

    public int createUser(String name, Role role) throws SQLException {
        String sql = "INSERT INTO users(name, role) VALUES(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, role.name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to create user.");
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, name, role FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Role role = Role.fromString(rs.getString("role"));
                return UserFactory.create(rs.getInt("id"), rs.getString("name"), role);
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, name, role FROM users ORDER BY id";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Role role = Role.fromString(rs.getString("role"));
                users.add(UserFactory.create(rs.getInt("id"), rs.getString("name"), role));
            }
        }
        return users;
    }
}

