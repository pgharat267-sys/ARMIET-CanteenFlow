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
                id BIGSERIAL PRIMARY KEY,
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
                id BIGSERIAL PRIMARY KEY,
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
                id BIGSERIAL PRIMARY KEY,
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


        // ==============================
        // SAFE PAYMENT COLUMN UPGRADES
        // ==============================

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


        // ==============================
        // NORMALIZE PAYMENT DATA
        // ==============================

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
                id BIGSERIAL PRIMARY KEY,
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

        Integer adminCount =
                jdbcTemplate.queryForObject(
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
                    "ADMIN"
            );

            System.out.println("======================================");
            System.out.println("Default admin account created");
            System.out.println("Email    : admin@armiet.com");
            System.out.println("Password : admin123");
            System.out.println("======================================");
        }


        // ==============================
        // DEFAULT FOOD MENU
        // ==============================

        Integer foodCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM food_items",
                        Integer.class
                );

        if (foodCount != null && foodCount == 0) {

            insertFood(
                    "Vada Pav",
                    "Mumbai's favourite spicy snack",
                    "Snacks",
                    20.0,
                    "🥪"
            );

            insertFood(
                    "Samosa",
                    "Crispy samosa with delicious filling",
                    "Snacks",
                    15.0,
                    "🥟"
            );

            insertFood(
                    "Sandwich",
                    "Fresh vegetable sandwich",
                    "Fast Food",
                    40.0,
                    "🥪"
            );

            insertFood(
                    "Pizza",
                    "Cheesy college special pizza",
                    "Fast Food",
                    80.0,
                    "🍕"
            );

            insertFood(
                    "Burger",
                    "Fresh and tasty veg burger",
                    "Fast Food",
                    60.0,
                    "🍔"
            );

            insertFood(
                    "Cold Coffee",
                    "Chilled creamy cold coffee",
                    "Beverages",
                    50.0,
                    "☕"
            );

            System.out.println("======================================");
            System.out.println("Default food menu inserted");
            System.out.println("======================================");
        }


        // ==============================
        // DATABASE READY
        // ==============================

        System.out.println("======================================");
        System.out.println("ARMIET CanteenFlow Database Ready!");
        System.out.println("Database : PostgreSQL");
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
    // INSERT FOOD
    // ==================================================

    private void insertFood(
            String name,
            String description,
            String category,
            double price,
            String imageUrl) {

        jdbcTemplate.update("""
            INSERT INTO food_items
            (name, description, category, price, image_url)
            VALUES (?, ?, ?, ?, ?)
        """,
                name,
                description,
                category,
                price,
                imageUrl
        );
    }


    // ==================================================
    // ADD COLUMN IF MISSING
    // POSTGRESQL-COMPATIBLE VERSION
    // ==================================================

    private void addColumnIfMissing(
            String tableName,
            String columnName,
            String columnDefinition) {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = ?
                          AND column_name = ?
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