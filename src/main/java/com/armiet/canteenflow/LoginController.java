package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class LoginController {

    private final JdbcTemplate jdbcTemplate;

    public LoginController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==============================
    // SHOW LOGIN PAGE
    // ==============================
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String registered,
            Model model) {

        if ("true".equals(registered)) {
            model.addAttribute(
                    "success",
                    "Registration successful! Please login."
            );
        }

        return "login";
    }

    // ==============================
    // PROCESS LOGIN
    // ==============================
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        email = email.trim().toLowerCase();

        if (email.isEmpty() || password.isEmpty()) {
            model.addAttribute(
                    "error",
                    "Please enter email and password."
            );
            return "login";
        }

        // Find user by email
        var users = jdbcTemplate.queryForList(
                "SELECT id, name, email, password, role FROM users WHERE email = ?",
                email
        );

        // User not found
        if (users.isEmpty()) {
            model.addAttribute(
                    "error",
                    "Invalid email or password."
            );
            return "login";
        }

        Map<String, Object> user = users.get(0);

        String storedPassword = (String) user.get("password");
        String role = (String) user.get("role");

        // Check password
        if (!password.equals(storedPassword)) {
            model.addAttribute(
                    "error",
                    "Invalid email or password."
            );
            return "login";
        }

        // ==============================
        // CREATE LOGIN SESSION
        // ==============================

        session.setAttribute("userId", user.get("id"));
        session.setAttribute("userName", user.get("name"));
        session.setAttribute("userEmail", user.get("email"));
        session.setAttribute("userRole", role);

        // ==============================
        // REDIRECT BASED ON ROLE
        // ==============================

        if ("ADMIN".equalsIgnoreCase(role)) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/student/dashboard";
    }

    // ==============================
    // LOGOUT
    // ==============================
    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/?logout=true";
    }
}