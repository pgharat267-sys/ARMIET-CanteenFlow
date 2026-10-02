package com.armiet.canteenflow;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {

        // ==============================
        // USERS TABLE
        // ==============================
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'STUDENT',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);


        // ==============================
        // FOOD ITEMS TABLE
        // ==============================
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS food_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                category TEXT,
                price REAL NOT NULL,
                image_url TEXT,
                available INTEGER NOT NULL DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);


        // ==============================
        // ORDERS TABLE
        // ==============================
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                token_number INTEGER,
                total_amount REAL NOT NULL,
                status TEXT NOT NULL DEFAULT 'PLACED',
                payment_method TEXT NOT NULL DEFAULT 'CASH_AT_COUNTER',
                payment_status TEXT NOT NULL DEFAULT 'PENDING',
                payment_id TEXT,
                order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """);


        // ==================================================
        // SAFE DATABASE UPGRADE
        // Add payment columns if they do not exist
        // ==================================================

        addColumnIfMissing(
                "orders",
                "payment_method",
                "TEXT NOT NULL DEFAULT 'CASH_AT_COUNTER'"
        );

        addColumnIfMissing(
                "orders",
                "payment_status",
                "TEXT NOT NULL DEFAULT 'PENDING'"
        );

        addColumnIfMissing(
                "orders",
                "payment_id",
                "TEXT"
        );


        // ==================================================
        // NORMALIZE EXISTING ORDERS
        // Old demo orders may have UPI as payment method.
        // Convert them to the new cash-at-counter system.
        // ==================================================

        jdbcTemplate.update("""
            UPDATE orders
            SET payment_method = 'CASH_AT_COUNTER'
            WHERE payment_method IS NULL
               OR payment_method = 'UPI'
        """);

        jdbcTemplate.update("""
            UPDATE orders
            SET payment_status = 'PENDING'
            WHERE payment_status IS NULL
        """);


        // ==============================
        // ORDER ITEMS TABLE
        // ==============================
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS order_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL,
                food_item_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                price REAL NOT NULL,
                FOREIGN KEY (order_id) REFERENCES orders(id),
                FOREIGN KEY (food_item_id) REFERENCES food_items(id)
            )
        """);


        // ==============================
        // DEFAULT ADMIN ACCOUNT
        // ==============================
        Integer adminCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?",
            Integer.class,
            "admin@armiet.com"
        );

        if (adminCount != null && adminCount == 0) {

            jdbcTemplate.update("""
                INSERT INTO users
                (name, email, password, role)
                VALUES (?, ?, ?, ?)
            """,
            "ARMIET Admin",
            "admin@armiet.com",
            "admin123",
            "ADMIN");

            System.out.println("======================================");
            System.out.println("Default admin account created");
            System.out.println("Email    : admin@armiet.com");
            System.out.println("Password : admin123");
            System.out.println("======================================");
        }


        // ==============================
        // DEFAULT FOOD MENU
        // ==============================
        Integer foodCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_items",
            Integer.class
        );

        if (foodCount != null && foodCount == 0) {

            jdbcTemplate.update("""
                INSERT INTO food_items
                (name, description, category, price, image_url)
                VALUES (?, ?, ?, ?, ?)
            """,
            "Vada Pav",
            "Mumbai's favourite spicy snack",
            "Snacks",
            20.0,
            "🥪");

            jdbcTemplate.update("""
                INSERT INTO food_items
                (name, description, category, price, image_url)
                VALUES (?, ?, ?, ?, ?)
            """,
            "Samosa",
            "Crispy samosa with delicious filling",
            "Snacks",
            15.0,
            "🥟");

            jdbcTemplate.update("""
                INSERT INTO food_items
                (name, description, category, price, image_url)
                VALUES (?, ?, ?, ?, ?)
            """,
            "Sandwich",
            "Fresh vegetable sandwich",
            "Fast Food",
            40.0,
            "🥪");

            jdbcTemplate.update("""
                INSERT INTO food_items
                (name, description, category, price, image_url)
                VALUES (?, ?, ?, ?, ?)
            """,
            "Pizza",
            "Cheesy college special pizza",
            "Fast Food",
            80.0,
            "🍕");

            jdbcTemplate.update("""
                INSERT INTO food_items
                (name, description, category, price, image_url)
                VALUES (?, ?, ?, ?, ?)
            """,
            "Burger",
            "Fresh and tasty veg burger",
            "Fast Food",
            60.0,
            "🍔");

            jdbcTemplate.update("""
                INSERT INTO food_items
                (name, description, category, price, image_url)
                VALUES (?, ?, ?, ?, ?)
            """,
            "Cold Coffee",
            "Chilled creamy cold coffee",
            "Beverages",
            50.0,
            "☕");

            System.out.println("======================================");
            System.out.println("Default food menu inserted");
            System.out.println("======================================");
        }


        // ==============================
        // DATABASE READY MESSAGE
        // ==============================
        System.out.println("======================================");
        System.out.println("ARMIET CanteenFlow Database Ready!");
        System.out.println("Tables created:");
        System.out.println("1. users");
        System.out.println("2. food_items");
        System.out.println("3. orders");
        System.out.println("4. order_items");
        System.out.println("--------------------------------------");
        System.out.println("Payment System:");
        System.out.println("Method : CASH AT COUNTER");
        System.out.println("Status : PENDING");
        System.out.println("======================================");
    }


    // ==================================================
    // ADD COLUMN ONLY IF IT DOES NOT ALREADY EXIST
    // ==================================================

    private void addColumnIfMissing(
            String tableName,
            String columnName,
            String columnDefinition) {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM pragma_table_info(?)
                        WHERE name = ?
                        """,
                        Integer.class,
                        tableName,
                        columnName
                );

        if (count != null && count == 0) {

            jdbcTemplate.execute(
                    "ALTER TABLE "
                    + tableName
                    + " ADD COLUMN "
                    + columnName
                    + " "
                    + columnDefinition
            );

            System.out.println(
                    "Added database column: "
                    + tableName
                    + "."
                    + columnName
            );
        }
    }
}