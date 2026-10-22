package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class StudentDashboardController {

    private final JdbcTemplate jdbcTemplate;

    public StudentDashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(
            HttpSession session,
            Model model) {

        // ==========================================
        // CHECK STUDENT LOGIN
        // ==========================================

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        int userId =
                ((Number) session.getAttribute("userId"))
                        .intValue();

        // ==========================================
        // STUDENT INFORMATION
        // ==========================================

        String userName =
                String.valueOf(session.getAttribute("userName"));

        String userEmail =
                String.valueOf(session.getAttribute("userEmail"));

        // ==========================================
        // ENSURE CANTEEN SETTINGS TABLE EXISTS
        // ==========================================

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS canteen_settings (
                id INTEGER PRIMARY KEY,
                is_live INTEGER NOT NULL DEFAULT 1
            )
        """);

        // ==========================================
        // CREATE DEFAULT SETTINGS IF REQUIRED
        // ==========================================

        Integer settingsCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM canteen_settings",
                        Integer.class
                );

        if (settingsCount == null || settingsCount == 0) {

            jdbcTemplate.update("""
                INSERT INTO canteen_settings
                (id, is_live)
                VALUES (1, 1)
            """);
        }

        // ==========================================
        // GET CANTEEN LIVE / CLOSED STATUS
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
        // STUDENT'S ACTIVE ORDERS
        // ==========================================

        Integer myActiveOrders =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM orders
                        WHERE user_id = ?
                        AND status IN
                        ('PLACED', 'PREPARING', 'READY')
                        """,
                        Integer.class,
                        userId
                );

        if (myActiveOrders == null) {
            myActiveOrders = 0;
        }

        // ==========================================
        // RUSH PERCENTAGE AND STATUS
        // ==========================================

        int rushPercentage;
        String rushStatus;

        if (!canteenLive) {

            // CLOSED: Always show zero rush.

            rushPercentage = 0;
            rushStatus = "CLOSED";

        } else {

            // LIVE: Calculate using actual active orders.

            rushPercentage =
                    (int) Math.round(
                            (activeOrders / 20.0) * 100
                    );

            if (rushPercentage > 100) {
                rushPercentage = 100;
            }

            if (activeOrders < 5) {
                rushStatus = "LOW";
            } else if (activeOrders < 10) {
                rushStatus = "MODERATE";
            } else if (activeOrders < 15) {
                rushStatus = "CROWDED";
            } else {
                rushStatus = "VERY CROWDED";
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
        // GET AVAILABLE FOOD MENU
        // ==========================================

        List<Map<String, Object>> foodItems =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            id,
                            name,
                            description,
                            category,
                            price,
                            image_url,
                            available
                        FROM food_items
                        WHERE available = 1
                        ORDER BY id ASC
                        """
                );

        // ==========================================
        // SEND VALUES TO STUDENT DASHBOARD
        // ==========================================

        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);

        model.addAttribute("canteenLive", canteenLive);

        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("myActiveOrders", myActiveOrders);

        model.addAttribute("rushPercentage", rushPercentage);
        model.addAttribute("rushStatus", rushStatus);

        model.addAttribute("currentToken", currentToken);
        model.addAttribute("estimatedWait", estimatedWait);

        model.addAttribute("foodItems", foodItems);

        return "student/dashboard";
    }
}