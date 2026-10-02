package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class AdminFoodController {

    private final JdbcTemplate jdbcTemplate;

    public AdminFoodController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/admin/food")
    public String foodMenu(
            HttpSession session,
            Model model) {

        Object roleObject = session.getAttribute("userRole");

        if (roleObject == null ||
                !"ADMIN".equalsIgnoreCase(roleObject.toString())) {

            return "redirect:/login";
        }

        List<Map<String, Object>> foodItems =
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
                        ORDER BY id ASC
                        """
                );

        model.addAttribute("foodItems", foodItems);

        return "admin/food";
    }
}
