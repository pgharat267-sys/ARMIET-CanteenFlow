package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminOrderStatusController {

    private final JdbcTemplate jdbcTemplate;

    public AdminOrderStatusController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================================================
    // UPDATE ORDER STATUS
    // ==================================================

    @PostMapping("/admin/orders/status")
    public String updateStatus(
            @RequestParam Long orderId,
            @RequestParam String status,
            HttpSession session) {

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
        // CLEAN STATUS
        // ==============================================

        String cleanStatus =
                status.trim().toUpperCase();


        // ==============================================
        // VALIDATE STATUS
        // ==============================================

        if (!cleanStatus.equals("PLACED") &&
                !cleanStatus.equals("PREPARING") &&
                !cleanStatus.equals("READY") &&
                !cleanStatus.equals("COMPLETED") &&
                !cleanStatus.equals("CANCELLED")) {

            return "redirect:/admin/orders";
        }


        // ==============================================
        // CHECK PAYMENT STATUS
        // ==============================================

        String paymentStatus =
                jdbcTemplate.queryForObject(
                        """
                        SELECT payment_status
                        FROM orders
                        WHERE id = ?
                        """,
                        String.class,
                        orderId
                );


        // ==============================================
        // PREVENT COMPLETING UNPAID ORDER
        // ==============================================

        if ("COMPLETED".equals(cleanStatus) &&
                !"PAID".equalsIgnoreCase(paymentStatus)) {

            return "redirect:/admin/orders?paymentRequired=true";
        }


        // ==============================================
        // UPDATE ORDER STATUS
        // ==============================================

        jdbcTemplate.update(
                """
                UPDATE orders
                SET status = ?
                WHERE id = ?
                """,
                cleanStatus,
                orderId
        );


        // ==============================================
        // RETURN TO ORDERS
        // ==============================================

        return "redirect:/admin/orders";
    }
}