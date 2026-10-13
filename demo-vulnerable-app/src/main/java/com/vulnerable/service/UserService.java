package com.vulnerable.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;

    // VULNERABLE: Hardcoded database credentials
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    private static final String API_KEY = System.getenv().getOrDefault("API_KEY", "");
    private static final String AWS_SECRET = System.getenv().getOrDefault("AWS_SECRET", "");

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // VULNERABLE: SQL Injection — string concatenation in SQL query
    public List<Map<String, Object>> searchUsers(String searchQuery) {
        String sql = "SELECT * FROM users WHERE name LIKE ? OR email LIKE ?";
        return jdbcTemplate.queryForList(sql, "%" + searchQuery + "%", "%" + searchQuery + "%");
    }

    // VULNERABLE: SQL Injection via string concatenation
    public Map<String, Object> getUserById(String userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
        if (results.isEmpty()) {
            return Map.of("error", "User not found");
        }
        return results.get(0);
    }

    // VULNERABLE: SQL Injection
    public void deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, userId);
    }

    // VULNERABLE: Logging sensitive data
    public void processPayment(String creditCard, double amount) {
        System.out.println("Processing payment for card: " + creditCard + " amount: " + amount);
        String sql = "INSERT INTO payments (card_number, amount) VALUES (?, ?)";
        jdbcTemplate.update(sql, creditCard, amount);
    }
}
