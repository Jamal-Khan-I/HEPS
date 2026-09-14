package com.hospital.priority.dao.impl;

import com.hospital.priority.dao.UserDao;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.Role;
import com.hospital.priority.model.User;
import com.hospital.priority.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of UserDao.
 */
public class UserDaoImpl implements UserDao {

    @Override
    public int insert(User user) throws DatabaseOperationException {
        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getRole().name());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseOperationException("User creation failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    user.setUserId(id);
                    return id;
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(int userId) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by username: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM users ORDER BY full_name ASC";
        List<User> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error listing users: " + e.getMessage(), e);
        }
    }

    private User mapResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String username = rs.getString("username");
        String passHash = rs.getString("password_hash");
        String name = rs.getString("full_name");
        Role role = Role.fromString(rs.getString("role"));

        Timestamp crTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = crTs != null ? crTs.toLocalDateTime() : null;

        return new User(id, username, passHash, name, role, createdAt);
    }
}
