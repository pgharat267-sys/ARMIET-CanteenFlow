package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminFoodActionController {

    private final JdbcTemplate jdbcTemplate;

    public AdminFoodActionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==========================================
    // TOGGLE FOOD AVAILABILITY
    // ==========================================

    @PostMapping("/admin/food/toggle")
    public String toggleAvailability(
            @RequestParam Long foodId,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        jdbcTemplate.update(
                """
                UPDATE food_items
                SET available =
                    CASE
                        WHEN available = 1 THEN 0
                        ELSE 1
                    END
                WHERE id = ?
                """,
                foodId
        );

        return "redirect:/admin/food";
    }


    // ==========================================
    // DELETE FOOD
    // ==========================================

    @PostMapping("/admin/food/delete")
    public String deleteFood(
            @RequestParam Long foodId,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        jdbcTemplate.update(
                """
                DELETE FROM food_items
                WHERE id = ?
                """,
                foodId
        );

        return "redirect:/admin/food";
    }


    // ==========================================
    // ADMIN SECURITY
    // ==========================================

    private boolean isAdmin(HttpSession session) {

        Object roleObject =
                session.getAttribute("userRole");

        return roleObject != null &&
                "ADMIN".equalsIgnoreCase(
                        roleObject.toString()
                );
    }
}
