package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@Controller
public class TrackingController {

    private final JdbcTemplate jdbcTemplate;

    public TrackingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    // ==================================================
    // TRACK ORDER
    // ==================================================

    @GetMapping("/student/order/{orderId}/track")
    public String trackOrder(
            @PathVariable Long orderId,
            HttpSession session,
            Model model) {

        // ==============================================
        // CHECK STUDENT LOGIN
        // ==============================================

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }


        int userId =
                ((Number) session.getAttribute("userId"))
                        .intValue();


        // ==============================================
        // GET ORDER
        // ==============================================

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
                        WHERE id = ?
                        AND user_id = ?
                        """,
                        orderId,
                        userId
                );


        // ==============================================
        // ORDER NOT FOUND
        // ==============================================

        if (orders.isEmpty()) {
            return "redirect:/student/orders";
        }


        Map<String, Object> order =
                orders.get(0);


        // ==============================================
        // GET ORDER ITEMS
        // ==============================================

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


        // ==============================================
        // ORDER STATUS
        // ==============================================

        String status =
                String.valueOf(order.get("status"))
                        .toUpperCase();


        int statusStep;


        switch (status) {

            case "PREPARING":

                statusStep = 2;

                break;


            case "READY":

                statusStep = 3;

                break;


            case "COMPLETED":

                statusStep = 4;

                break;


            case "PLACED":

            default:

                statusStep = 1;

                break;
        }


        // ==============================================
        // TOTAL ITEMS
        // ==============================================

        int totalItems = 0;


        for (Map<String, Object> item : items) {

            totalItems +=
                    ((Number) item.get("quantity"))
                            .intValue();
        }


        // ==============================================
        // ESTIMATED WAITING TIME
        // ==============================================

        int estimatedWait;


        if (statusStep >= 3) {

            estimatedWait = 0;

        } else {

            estimatedWait =
                    5 + (totalItems * 3);
        }


        // ==============================================
        // SEND DATA TO THYMELEAF
        // ==============================================

        model.addAttribute(
                "order",
                order
        );


        model.addAttribute(
                "orderItems",
                items
        );


        model.addAttribute(
                "status",
                status
        );


        model.addAttribute(
                "statusStep",
                statusStep
        );


        model.addAttribute(
                "estimatedWait",
                estimatedWait
        );


        return "student/order-tracking";
    }
}