package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class AdminFoodEditController {

    private final JdbcTemplate jdbcTemplate;

    public AdminFoodEditController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/food/edit/{foodId}")
    public String showEditFood(
            @PathVariable Long foodId,
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Map<String, Object>> foods =
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
                        WHERE id = ?
                        """,
                        foodId
                );

        if (foods.isEmpty()) {
            return "redirect:/admin/food";
        }

        model.addAttribute("food", foods.get(0));

        return "admin/edit-food";
    }

    @PostMapping("/admin/food/edit")
    public String updateFood(
            @RequestParam Long foodId,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam double price,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String available,
            HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String cleanName = name.trim();
        String cleanDescription = description.trim();
        String cleanCategory = category.trim();
        String cleanImageUrl =
                imageUrl == null ? "" : imageUrl.trim();

        if (cleanName.isEmpty() ||
                cleanDescription.isEmpty() ||
                cleanCategory.isEmpty() ||
                price <= 0) {

            return "redirect:/admin/food/edit/" + foodId;
        }

        int isAvailable =
                available != null ? 1 : 0;

        jdbcTemplate.update(
                """
                UPDATE food_items
                SET name = ?,
                    description = ?,
                    category = ?,
                    price = ?,
                    image_url = ?,
                    available = ?
                WHERE id = ?
                """,
                cleanName,
                cleanDescription,
                cleanCategory,
                price,
                cleanImageUrl,
                isAvailable,
                foodId
        );

        return "redirect:/admin/food";
    }

    private boolean isAdmin(HttpSession session) {

        Object roleObject =
                session.getAttribute("userRole");

        return roleObject != null &&
                "ADMIN".equalsIgnoreCase(
                        roleObject.toString()
                );
    }
}
