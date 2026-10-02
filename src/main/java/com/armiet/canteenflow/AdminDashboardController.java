package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        Object roleObject = session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(roleObject.toString())) {

            return "redirect:/login";
        }

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
                        "SELECT COUNT(*) FROM food_items WHERE available = 1",
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
                        WHERE status IN ('PLACED', 'PREPARING', 'READY')
                        """,
                        Integer.class
                );

        // ==============================
        // TOTAL SALES
        // ==============================
        Double totalSales =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(SUM(total_amount), 0)
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
                        SELECT COALESCE(SUM(total_amount), 0)
                        FROM orders
                        WHERE status != 'CANCELLED'
                        AND DATE(order_time) = DATE('now')
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
        model.addAttribute("totalFood", totalFood);
        model.addAttribute("availableFood", availableFood);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("todaySales", todaySales);
        model.addAttribute("recentOrders", recentOrders);

        return "admin/dashboard";
    }
}
