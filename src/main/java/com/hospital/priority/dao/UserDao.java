package com.hospital.priority.dao;

import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Staff User operations.
 */
public interface UserDao {

    int insert(User user) throws DatabaseOperationException;

    Optional<User> findById(int userId) throws DatabaseOperationException;

    Optional<User> findByUsername(String username) throws DatabaseOperationException;

    List<User> findAll() throws DatabaseOperationException;
}
