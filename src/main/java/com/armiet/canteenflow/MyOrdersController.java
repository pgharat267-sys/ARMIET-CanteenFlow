package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class MyOrdersController {

    private final JdbcTemplate jdbcTemplate;

    public MyOrdersController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/student/orders")
    public String myOrders(
            HttpSession session,
            Model model) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        int userId =
                ((Number) session.getAttribute("userId"))
                        .intValue();

        List<Map<String, Object>> orders =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            id,
                            token_number,
                            total_amount,
                            status,
                            payment_method,
                            payment_status,
                            payment_id,
                            order_time
                        FROM orders
                        WHERE user_id = ?
                        ORDER BY id DESC
                        """,
                        userId
                );

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
                            """,
                            orderId
                    );

            order.put("items", items);
        }

        model.addAttribute(
                "orders",
                orders
        );

        return "student/orders";
    }
}