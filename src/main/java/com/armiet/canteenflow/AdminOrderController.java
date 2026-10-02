package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class AdminOrderController {

    private final JdbcTemplate jdbcTemplate;

    public AdminOrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================================================
    // ADMIN ORDERS PAGE
    // ==================================================

    @GetMapping("/admin/orders")
    public String orders(
            HttpSession session,
            Model model) {

        // ==============================================
        // CHECK ADMIN LOGIN
        // ==============================================

        Object roleObject =
                session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(
                        roleObject.toString())) {

            return "redirect:/login";
        }


        // ==============================================
        // GET ALL ORDERS
        // ==============================================

        List<Map<String, Object>> orders =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            o.id,
                            o.token_number,
                            o.total_amount,
                            o.status,
                            o.payment_method,
                            o.payment_status,
                            o.payment_id,
                            o.order_time,

                            u.name AS student_name,
                            u.email AS student_email

                        FROM orders o

                        JOIN users u
                        ON o.user_id = u.id

                        ORDER BY o.id DESC
                        """
                );


        // ==============================================
        // GET FOOD ITEMS FOR EACH ORDER
        // ==============================================

        for (Map<String, Object> order : orders) {

            long orderId =
                    ((Number) order.get("id"))
                            .longValue();


            List<Map<String, Object>> items =
                    jdbcTemplate.queryForList(
                            """
                            SELECT
                                oi.quantity,
                                oi.price,
                                f.name,
                                f.category

                            FROM order_items oi

                            JOIN food_items f
                            ON oi.food_item_id = f.id

                            WHERE oi.order_id = ?

                            ORDER BY oi.id ASC
                            """,
                            orderId
                    );


            // Add items to the order map
            order.put(
                    "items",
                    items
            );
        }


        // ==============================================
        // SEND ORDERS TO THYMELEAF
        // ==============================================

        model.addAttribute(
                "orders",
                orders
        );


        return "admin/orders";
    }
}