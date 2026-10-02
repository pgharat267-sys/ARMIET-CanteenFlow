package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class AdminSalesController {

    private final JdbcTemplate jdbcTemplate;

    public AdminSalesController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/sales")
    public String sales(
            HttpSession session,
            Model model) {

        // Admin security check
        Object roleObject = session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(roleObject.toString())) {

            return "redirect:/login";
        }

        // Total sales
        Double totalSales =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(SUM(total_amount), 0)
                        FROM orders
                        WHERE status != 'CANCELLED'
                        """,
                        Double.class
                );

        // Today's sales
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

        // Total orders
        Integer totalOrders =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM orders
                        WHERE status != 'CANCELLED'
                        """,
                        Integer.class
                );

        // Completed orders
        Integer completedOrders =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM orders
                        WHERE status = 'COMPLETED'
                        """,
                        Integer.class
                );

        // Active orders
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

        // Popular food
        List<Map<String, Object>> popularFood =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            f.name,
                            f.category,
                            SUM(oi.quantity) AS quantity_sold,
                            SUM(oi.quantity * oi.price) AS revenue
                        FROM order_items oi
                        JOIN food_items f
                        ON oi.food_item_id = f.id
                        JOIN orders o
                        ON oi.order_id = o.id
                        WHERE o.status != 'CANCELLED'
                        GROUP BY f.id, f.name, f.category
                        ORDER BY quantity_sold DESC
                        LIMIT 10
                        """
                );

        // Recent sales
        List<Map<String, Object>> recentSales =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            o.id,
                            o.token_number,
                            o.total_amount,
                            o.status,
                            o.order_time,
                            u.name AS customer_name
                        FROM orders o
                        JOIN users u
                        ON o.user_id = u.id
                        WHERE o.status != 'CANCELLED'
                        ORDER BY o.id DESC
                        LIMIT 15
                        """
                );

        model.addAttribute("totalSales", totalSales);
        model.addAttribute("todaySales", todaySales);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("popularFood", popularFood);
        model.addAttribute("recentSales", recentSales);

        return "admin/sales";
    }
}