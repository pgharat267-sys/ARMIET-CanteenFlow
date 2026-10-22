package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TokenResetController {

    private final JdbcTemplate jdbcTemplate;

    public TokenResetController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================================================
    // RESET TOKEN NUMBER
    // ==================================================

    @PostMapping("/admin/token/reset")
    public String resetToken(HttpSession session) {

        // ==========================================
        // ADMIN SECURITY
        // ==========================================

        Object roleObject =
                session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(
                        roleObject.toString())) {

            return "redirect:/login";
        }

        // ==========================================
        // CREATE TOKEN SETTINGS TABLE IF NEEDED
        // ==========================================

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS token_settings (
                id INTEGER PRIMARY KEY,
                next_token INTEGER NOT NULL DEFAULT 1
            )
        """);

        // ==========================================
        // CREATE DEFAULT RECORD IF NEEDED
        // ==========================================

        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM token_settings",
                        Integer.class
                );

        if (count == null || count == 0) {

            jdbcTemplate.update(
                    """
                    INSERT INTO token_settings
                    (id, next_token)
                    VALUES (1, 1)
                    """
            );

        } else {

            // ==========================================
            // RESET TOKEN COUNTER
            // ==========================================

            jdbcTemplate.update(
                    """
                    UPDATE token_settings
                    SET next_token = 1
                    WHERE id = 1
                    """
            );
        }

        // ==========================================
        // RETURN TO ADMIN DASHBOARD
        // ==========================================

        return "redirect:/admin/dashboard";
    }
}