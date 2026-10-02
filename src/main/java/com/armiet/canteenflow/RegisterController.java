package com.armiet.canteenflow;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {

    private final JdbcTemplate jdbcTemplate;

    public RegisterController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Show registration page
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    // Process registration
    @PostMapping("/register")
    public String registerUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        // Remove unnecessary spaces
        name = name.trim();
        email = email.trim().toLowerCase();

        // Basic validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            model.addAttribute("error", "Please fill in all required fields.");
            return "register";
        }

        if (password.length() < 6) {
            model.addAttribute("error", "Password must contain at least 6 characters.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        // Check whether email already exists
        Integer existingUser = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );

        if (existingUser != null && existingUser > 0) {
            model.addAttribute("error", "An account with this email already exists.");
            return "register";
        }

        try {

            // Save student in database
            jdbcTemplate.update("""
                INSERT INTO users (name, email, password, role)
                VALUES (?, ?, ?, ?)
            """,
                    name,
                    email,
                    password,
                    "STUDENT"
            );

        } catch (DuplicateKeyException e) {

            model.addAttribute("error", "This email is already registered.");
            return "register";
        }

        // Registration successful
        return "redirect:/login?registered=true";
    }
}