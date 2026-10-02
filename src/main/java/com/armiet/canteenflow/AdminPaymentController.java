package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminPaymentController {

    private final JdbcTemplate jdbcTemplate;

    public AdminPaymentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/admin/orders/payment")
    public String updatePayment(
            @RequestParam Long orderId,
            @RequestParam String paymentStatus,
            HttpSession session) {

        Object roleObject =
                session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(
                        roleObject.toString())) {

            return "redirect:/login";
        }

        String cleanStatus =
                paymentStatus
                        .trim()
                        .toUpperCase();

        if (!cleanStatus.equals("PAID") &&
                !cleanStatus.equals("PENDING")) {

            return "redirect:/admin/orders";
        }

        jdbcTemplate.update(
                """
                UPDATE orders
                SET payment_status = ?
                WHERE id = ?
                """,
                cleanStatus,
                orderId
        );

        return "redirect:/admin/orders";
    }
}