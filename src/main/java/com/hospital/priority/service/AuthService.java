package com.hospital.priority.service;

import com.hospital.priority.dao.UserDao;
import com.hospital.priority.dao.impl.UserDaoImpl;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.Role;
import com.hospital.priority.model.User;
import com.hospital.priority.util.PasswordHasher;

import java.util.Optional;

/**
 * Service managing user authentication and active staff session.
 */
public class AuthService {

    private static AuthService instance;
    private final UserDao userDao;
    private User currentUser;

    private AuthService() {
        this.userDao = new UserDaoImpl();
        // Default to Administrator session
        this.currentUser = new User(1, "admin", "", "Administrator", Role.ADMIN, null);
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public boolean login(String username, String password) throws DatabaseOperationException {
        if (username == null || password == null) {
            return false;
        }

        Optional<User> userOpt = userDao.findByUsername(username.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hashedInput = PasswordHasher.hash(password.trim());
            if (hashedInput.equalsIgnoreCase(user.getPasswordHash()) || password.equals(user.getPasswordHash())) {
                this.currentUser = user;
                return true;
            }
        }
        return false;
    }

    public void logout() {
        // Reset to guest receptionist default
        this.currentUser = new User(0, "guest", "", "Guest Staff", Role.RECEPTIONIST, null);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }
}
