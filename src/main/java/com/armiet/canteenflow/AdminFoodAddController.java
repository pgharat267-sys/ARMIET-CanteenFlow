package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
public class AdminFoodAddController {

    private final JdbcTemplate jdbcTemplate;
    private final CloudinaryService cloudinaryService;

    public AdminFoodAddController(
            JdbcTemplate jdbcTemplate,
            CloudinaryService cloudinaryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.cloudinaryService = cloudinaryService;
    }

    // Show Add Food page
    @GetMapping("/admin/food/add")
    public String showAddFoodPage(HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        return "admin/add-food";
    }

    // Save new food item
    @PostMapping("/admin/food/add")
    public String addFood(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam double price,
            @RequestParam(required = false) MultipartFile foodImage,
            @RequestParam(required = false) String available,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String cleanName = name == null ? "" : name.trim();
        String cleanDescription =
                description == null ? "" : description.trim();
        String cleanCategory =
                category == null ? "" : category.trim();

        if (cleanName.isEmpty()
                || cleanDescription.isEmpty()
                || cleanCategory.isEmpty()
                || !Double.isFinite(price)
                || price <= 0) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter valid food details and price."
            );

            return "redirect:/admin/food/add";
        }

        int isAvailable = available != null ? 1 : 0;
        String imageUrl = "";

        // Upload the selected image to Cloudinary
        if (foodImage != null && !foodImage.isEmpty()) {
            String contentType = foodImage.getContentType();

            if (contentType == null
                    || !contentType.toLowerCase()
                            .startsWith("image/")) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please select a valid image file."
                );

                return "redirect:/admin/food/add";
            }

            try {
                imageUrl = cloudinaryService.uploadImage(foodImage);
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Image upload failed. Please try again."
                );

                return "redirect:/admin/food/add";
            }
        }

        try {
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
                    imageUrl,
                    isAvailable
            );
        } catch (RuntimeException e) {
            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Could not save the food item. Please try again."
            );

            return "redirect:/admin/food/add";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Food item added successfully."
        );

        return "redirect:/admin/food";
    }

    // Admin security check
    private boolean isAdmin(HttpSession session) {
        Object roleObject = session.getAttribute("userRole");

        return roleObject != null
                && "ADMIN".equalsIgnoreCase(roleObject.toString());
    }
}
