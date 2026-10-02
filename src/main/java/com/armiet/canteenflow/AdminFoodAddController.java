package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminFoodAddController {

    private final JdbcTemplate jdbcTemplate;

    public AdminFoodAddController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==========================================
    // SHOW ADD FOOD PAGE
    // ==========================================

    @GetMapping("/admin/food/add")
    public String showAddFoodPage(HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        return "admin/add-food";
    }


    // ==========================================
    // SAVE NEW FOOD
    // ==========================================

    @PostMapping("/admin/food/add")
    public String addFood(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam double price,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String available,
            HttpSession session) {

        // Check that only admin can add food
        if (!isAdmin(session)) {
            return "redirect:/login";
        }


        // Remove unnecessary spaces
        String cleanName = name.trim();

        String cleanDescription =
                description.trim();

        String cleanCategory =
                category.trim();

        String cleanImageUrl =
                imageUrl == null
                        ? ""
                        : imageUrl.trim();


        // Basic validation
        if (cleanName.isEmpty() ||
                cleanDescription.isEmpty() ||
                cleanCategory.isEmpty() ||
                price <= 0) {

            return "redirect:/admin/food/add";
        }


        // Checkbox handling
        //
        // Checkbox checked:
        // available = "on"
        //
        // Checkbox unchecked:
        // available = null
        //
        // Database uses:
        // 1 = available
        // 0 = unavailable

        int isAvailable =
                available != null ? 1 : 0;


        // ==========================================
        // INSERT FOOD INTO DATABASE
        // ==========================================

        jdbcTemplate.update(
                """
                INSERT INTO food_items
                (name, description, category, price, image_url, available)
                VALUES (?, ?, ?, ?, ?, ?)
                """,

                cleanName,
                cleanDescription,
                cleanCategory,
                price,
                cleanImageUrl,
                isAvailable
        );


        // After successful insertion,
        // go back to Food Menu.

        return "redirect:/admin/food";
    }


    // ==========================================
    // ADMIN SECURITY CHECK
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
