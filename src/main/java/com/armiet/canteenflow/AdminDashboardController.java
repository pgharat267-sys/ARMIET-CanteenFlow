package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class AdminDashboardController {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(
            HttpSession session,
            Model model) {

        // ==============================
        // ADMIN SECURITY CHECK
        // ==============================

        Object roleObject =
                session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(
                        roleObject.toString())) {

            return "redirect:/login";
        }

        // ==============================
        // CREATE CANTEEN SETTINGS TABLE
        // ==============================

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS canteen_settings (
                id INTEGER PRIMARY KEY,
                is_live INTEGER NOT NULL DEFAULT 1
            )
        """);

        // ==============================
        // CREATE DEFAULT LIVE SETTING
        // ==============================

        Integer settingsCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM canteen_settings",
                        Integer.class
                );

        if (settingsCount == null ||
                settingsCount == 0) {

            jdbcTemplate.update(
                    """
                    INSERT INTO canteen_settings
                    (id, is_live)
                    VALUES (1, 1)
                    """
            );
        }

        // ==============================
        // GET CANTEEN STATUS
        // ==============================

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
                liveValue != null &&
                liveValue == 1;

        // ==============================
        // ADMIN INFORMATION
        // ==============================

        model.addAttribute(
                "adminName",
                session.getAttribute("userName")
        );

        // ==============================
        // TOTAL FOOD ITEMS
        // ==============================

        Integer totalFood =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM food_items",
                        Integer.class
                );

        // ==============================
        // AVAILABLE FOOD
        // ==============================

        Integer availableFood =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM food_items
                        WHERE available = 1
                        """,
                        Integer.class
                );

        // ==============================
        // TOTAL ORDERS
        // ==============================

        Integer totalOrders =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM orders",
                        Integer.class
                );

        // ==============================
        // ACTIVE ORDERS
        // ==============================

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

        // ==============================
        // TOTAL SALES
        // ==============================

        Double totalSales =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(
                            SUM(total_amount), 0
                        )
                        FROM orders
                        WHERE status != 'CANCELLED'
                        """,
                        Double.class
                );

        // ==============================
        // TODAY'S SALES
        // ==============================

        Double todaySales =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(
                            SUM(total_amount), 0
                        )
                        FROM orders
                        WHERE status != 'CANCELLED'
                        AND DATE(order_time) =
                            DATE('now')
                        """,
                        Double.class
                );

        // ==============================
        // RECENT ORDERS
        // ==============================

        List<Map<String, Object>> recentOrders =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            o.id,
                            o.token_number,
                            o.total_amount,
                            o.status,
                            o.order_time,
                            u.name AS customer_name,
                            u.email AS customer_email
                        FROM orders o
                        JOIN users u
                        ON o.user_id = u.id
                        ORDER BY o.id DESC
                        LIMIT 10
                        """
                );

        // ==============================
        // SEND DATA TO PAGE
        // ==============================

        model.addAttribute(
                "totalFood",
                totalFood
        );

        model.addAttribute(
                "availableFood",
                availableFood
        );

        model.addAttribute(
                "totalOrders",
                totalOrders
        );

        model.addAttribute(
                "activeOrders",
                activeOrders
        );

        model.addAttribute(
                "totalSales",
                totalSales
        );

        model.addAttribute(
                "todaySales",
                todaySales
        );

        model.addAttribute(
                "recentOrders",
                recentOrders
        );

        // ==============================
        // CANTEEN LIVE / CLOSED STATUS
        // ==============================

        model.addAttribute(
                "canteenLive",
                canteenLive
        );

        return "admin/dashboard";
    }


    // ==========================================
    // CHANGE CANTEEN LIVE / CLOSED STATUS
    // ==========================================

    @PostMapping("/admin/canteen/status")
    public String changeCanteenStatus(
            @RequestParam boolean live,
            HttpSession session) {

        // ==============================
        // ADMIN SECURITY CHECK
        // ==============================

        Object roleObject =
                session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(
                        roleObject.toString())) {

            return "redirect:/login";
        }

        // ==============================
        // UPDATE CANTEEN STATUS
        // ==============================

        jdbcTemplate.update(
                """
                UPDATE canteen_settings
                SET is_live = ?
                WHERE id = 1
                """,
                live ? 1 : 0
        );

        // ==============================
        // RETURN TO DASHBOARD
        // ==============================

        return "redirect:/admin/dashboard";
    }
}