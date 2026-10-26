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

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS canteen_settings (
                id INTEGER PRIMARY KEY,
                is_live INTEGER NOT NULL DEFAULT 1
            )
        """);

        Integer settingsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM canteen_settings",
                Integer.class
        );

        if (settingsCount == null || settingsCount == 0) {
            jdbcTemplate.update("""
                INSERT INTO canteen_settings (id, is_live)
                VALUES (1, 1)
            """);
        }

        Integer liveValue = jdbcTemplate.queryForObject(
                "SELECT is_live FROM canteen_settings WHERE id = 1",
                Integer.class
        );

        boolean canteenLive = liveValue != null && liveValue == 1;

        model.addAttribute("canteenLive", canteenLive);

        return "home";
    }
}