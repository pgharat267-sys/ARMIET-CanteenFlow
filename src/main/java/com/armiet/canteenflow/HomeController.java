package com.armiet.canteenflow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String home(Model model) {

        // ==========================================
        // ENSURE CANTEEN SETTINGS EXIST
        // ==========================================

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS canteen_settings (
                id INTEGER PRIMARY KEY,
                is_live INTEGER NOT NULL DEFAULT 1
            )
        """);

        Integer settingsCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM canteen_settings",
                        Integer.class
                );

        if (settingsCount == null || settingsCount == 0) {
            jdbcTemplate.update("""
                INSERT INTO canteen_settings (id, is_live)
                VALUES (1, 1)
            """);
        }

        // ==========================================
        // GET CANTEEN STATUS
        // ==========================================

        Integer liveValue =
                jdbcTemplate.queryForObject(
                        """
                        SELECT is_live
                        FROM canteen_settings
                        WHERE id = 1
                        """,
                        Integer.class
                );

        boolean canteenLive =
                liveValue != null && liveValue == 1;

        // ==========================================
        // COUNT ACTUAL ACTIVE ORDERS
        // ==========================================

        Integer activeOrders =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM orders
                        WHERE status IN
                        ('PLACED', 'PREPARING', 'READY')
                        """,
                        Integer.class
                );

        if (activeOrders == null) {
            activeOrders = 0;
        }

        // ==========================================
        // RUSH PERCENTAGE AND STATUS
        // ==========================================

        int rushPercentage;
        String rushStatus;

        if (!canteenLive) {

            // Closed canteen always displays zero rush.
            rushPercentage = 0;
            rushStatus = "CLOSED";

        } else {

            rushPercentage =
                    (int) Math.round(
                            (activeOrders / 20.0) * 100
                    );

            rushPercentage =
                    Math.min(rushPercentage, 100);

            if (activeOrders < 5) {
                rushStatus = "Low";
            } else if (activeOrders < 10) {
                rushStatus = "Moderate";
            } else if (activeOrders < 15) {
                rushStatus = "Crowded";
            } else {
                rushStatus = "Very Crowded";
            }
        }

        // ==========================================
        // CURRENT ACTIVE TOKEN
        // ==========================================

        Integer currentToken =
                jdbcTemplate.queryForObject(
                        """
                        SELECT MAX(token_number)
                        FROM orders
                        WHERE status IN
                        ('PLACED', 'PREPARING', 'READY')
                        """,
                        Integer.class
                );

        if (currentToken == null) {
            currentToken = 0;
        }

        // ==========================================
        // ESTIMATED WAITING TIME
        // ==========================================

        int estimatedWait;

        if (!canteenLive || activeOrders == 0) {

            estimatedWait = 0;

        } else if (activeOrders < 5) {

            estimatedWait = 5;

        } else if (activeOrders < 10) {

            estimatedWait = 10;

        } else if (activeOrders < 15) {

            estimatedWait = 15;

        } else {

            estimatedWait = 20;
        }

        // ==========================================
        // SEND VALUES TO HOME PAGE
        // ==========================================

        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("rushStatus", rushStatus);
        model.addAttribute("rushPercentage", rushPercentage);
        model.addAttribute("currentToken", currentToken);
        model.addAttribute("estimatedWait", estimatedWait);
        model.addAttribute("canteenLive", canteenLive);

        return "index";
    }
}