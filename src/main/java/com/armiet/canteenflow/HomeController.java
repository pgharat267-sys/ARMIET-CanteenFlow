package com.armiet.canteenflow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String home(Model model) {

        boolean canteenLive = true;

        try {
            Integer status = jdbcTemplate.queryForObject(
                "SELECT is_live FROM canteen_settings WHERE id = 1",
                Integer.class
            );

            canteenLive = status != null && status == 1;

        } catch (Exception e) {
            // Keep the public homepage available if the status
            // cannot be retrieved from the database.
            canteenLive = true;
        }

        model.addAttribute("canteenLive", canteenLive);

        return "home";
    }
}