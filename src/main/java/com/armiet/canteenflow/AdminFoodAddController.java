package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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
            @RequestParam(required = false) MultipartFile foodImage,
            @RequestParam(required = false) String available,
            HttpSession session) {

        // ==========================================
        // ADMIN SECURITY CHECK
        // ==========================================

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // ==========================================
        // CLEAN INPUT
        // ==========================================

        String cleanName =
                name == null
                        ? ""
                        : name.trim();

        String cleanDescription =
                description == null
                        ? ""
                        : description.trim();

        String cleanCategory =
                category == null
                        ? ""
                        : category.trim();

        // ==========================================
        // BASIC VALIDATION
        // ==========================================

        if (cleanName.isEmpty() ||
                cleanDescription.isEmpty() ||
                cleanCategory.isEmpty() ||
                price <= 0) {

            return "redirect:/admin/food/add";
        }

        // ==========================================
        // AVAILABILITY
        // ==========================================

        int isAvailable =
                available != null ? 1 : 0;

        // ==========================================
        // FOOD IMAGE
        // ==========================================

        String imageUrl = "";

        if (foodImage != null &&
                !foodImage.isEmpty()) {

            // Make sure uploaded file is an image
            String contentType =
                    foodImage.getContentType();

            if (contentType == null ||
                    !contentType.startsWith("image/")) {

                return "redirect:/admin/food/add";
            }

            try {

                // ==========================================
                // CREATE UPLOAD DIRECTORY
                // ==========================================

                Path uploadDirectory =
                        Paths.get(
                                "src",
                                "main",
                                "resources",
                                "static",
                                "uploads",
                                "food"
                        );

                Files.createDirectories(
                        uploadDirectory
                );

                // ==========================================
                // GET ORIGINAL FILE EXTENSION
                // ==========================================

                String originalFileName =
                        foodImage.getOriginalFilename();

                String extension = "";

                if (originalFileName != null &&
                        originalFileName.contains(".")) {

                    extension =
                            originalFileName.substring(
                                    originalFileName
                                            .lastIndexOf(".")
                            ).toLowerCase();
                }

                // ==========================================
                // CREATE UNIQUE FILE NAME
                // ==========================================

                String newFileName =
                        UUID.randomUUID()
                                .toString()
                                + extension;

                // ==========================================
                // FINAL FILE PATH
                // ==========================================

                Path filePath =
                        uploadDirectory.resolve(
                                newFileName
                        );

                // ==========================================
                // SAVE IMAGE
                // ==========================================

                try (InputStream inputStream =
                             foodImage.getInputStream()) {

                    Files.copy(
                            inputStream,
                            filePath,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                // ==========================================
                // IMAGE URL FOR DATABASE
                // ==========================================

                imageUrl =
                        "/uploads/food/"
                                + newFileName;

            } catch (IOException e) {

                e.printStackTrace();

                return "redirect:/admin/food/add";
            }
        }

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
                imageUrl,
                isAvailable
        );

        // ==========================================
        // RETURN TO FOOD MENU
        // ==========================================

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