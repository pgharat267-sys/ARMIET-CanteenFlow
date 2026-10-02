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

        // Check student login
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        int userId =
                ((Number) session.getAttribute("userId"))
                        .intValue();

        // Get logged-in student's name
        String userName =
                String.valueOf(
                        session.getAttribute("userName")
                );

        String userEmail =
                String.valueOf(
                        session.getAttribute("userEmail")
                );

        /*
         * ==========================================
         * ACTIVE ORDERS / RUSH METER
         * ==========================================
         */

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

        /*
         * Rush levels:
         *
         * 0-4   = LOW
         * 5-9   = MODERATE
         * 10-14 = CROWDED
         * 15+   = VERY CROWDED
         */

        String rushStatus;

        if (activeOrders < 5) {
            rushStatus = "LOW";
        }
        else if (activeOrders < 10) {
            rushStatus = "MODERATE";
        }
        else if (activeOrders < 15) {
            rushStatus = "CROWDED";
        }
        else {
            rushStatus = "VERY CROWDED";
        }

        /*
         * Maximum rush capacity = 20 orders.
         * Anything above 20 stays at 100%.
         */

        int rushPercentage =
                (int) Math.round(
                        (activeOrders / 20.0) * 100
                );

        if (rushPercentage > 100) {
            rushPercentage = 100;
        }

        /*
         * ==========================================
         * CURRENT TOKEN
         * ==========================================
         */

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

        /*
         * ==========================================
         * STUDENT'S ACTIVE ORDERS
         * ==========================================
         */

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

        /*
         * ==========================================
         * FOOD MENU
         * ==========================================
         */

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

        /*
         * ==========================================
         * ESTIMATED WAITING TIME
         * ==========================================
         *
         * Rough estimate based on active orders.
         */

        int estimatedWait;

        if (activeOrders == 0) {
            estimatedWait = 0;
        }
        else if (activeOrders < 5) {
            estimatedWait = 5;
        }
        else if (activeOrders < 10) {
            estimatedWait = 10;
        }
        else if (activeOrders < 15) {
            estimatedWait = 15;
        }
        else {
            estimatedWait = 20;
        }

        /*
         * ==========================================
         * SEND DATA TO THYMELEAF
         * ==========================================
         */

        model.addAttribute(
                "userName",
                userName
        );

        model.addAttribute(
                "userEmail",
                userEmail
        );

        model.addAttribute(
                "activeOrders",
                activeOrders
        );

        model.addAttribute(
                "myActiveOrders",
                myActiveOrders
        );

        model.addAttribute(
                "rushPercentage",
                rushPercentage
        );

        model.addAttribute(
                "rushStatus",
                rushStatus
        );

        model.addAttribute(
                "currentToken",
                currentToken
        );

        model.addAttribute(
                "estimatedWait",
                estimatedWait
        );

        model.addAttribute(
                "foodItems",
                foodItems
        );

        return "student/dashboard";
    }
}