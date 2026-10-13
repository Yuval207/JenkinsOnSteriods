package com.vulnerable.controller;

import com.vulnerable.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * VULNERABLE: This controller has multiple security issues for testing.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // VULNERABLE: SQL Injection — user input passed directly to query
    @GetMapping("/search")
    public List<Map<String, Object>> searchUsers(@RequestParam String query) {
        return userService.searchUsers(query);
    }

    // VULNERABLE: No authentication, no authorization, no input validation
    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        return userService.getUserById(id);
    }

    // VULNERABLE: No CSRF protection, no auth
    @DeleteMapping("/{id}")
    public Map<String, String> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return Map.of("status", "deleted", "id", id);
    }
}
