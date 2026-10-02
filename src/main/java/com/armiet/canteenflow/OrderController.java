package com.armiet.canteenflow;

import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private final JdbcTemplate jdbcTemplate;

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================================================
    // PLACE ORDER
    // ==================================================

    @PostMapping("/student/order/place")
    public String placeOrder(HttpSession session) {

        // Check login
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        int userId =
                ((Number) session.getAttribute("userId")).intValue();

        // Get cart
        Map<Integer, Integer> cart =
                getCartMap(session);

        // Cart empty
        if (cart.isEmpty()) {
            return "redirect:/student/cart";
        }

        List<Map<String, Object>> cartItems =
                new ArrayList<>();

        double totalAmount = 0;

        // ==================================================
        // READ FOOD ITEMS FROM CART
        // ==================================================

        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {

            int foodId = entry.getKey();
            int quantity = entry.getValue();

            List<Map<String, Object>> foods =
                    jdbcTemplate.queryForList(
                            """
                            SELECT
                                id,
                                name,
                                price
                            FROM food_items
                            WHERE id = ?
                            AND available = 1
                            """,
                            foodId
                    );

            if (!foods.isEmpty()) {

                Map<String, Object> food =
                        new HashMap<>(foods.get(0));

                double price =
                        ((Number) food.get("price"))
                                .doubleValue();

                double itemTotal =
                        price * quantity;

                food.put("quantity", quantity);
                food.put("itemTotal", itemTotal);

                cartItems.add(food);

                totalAmount += itemTotal;
            }
        }

        // No valid food items
        if (cartItems.isEmpty()) {
            return "redirect:/student/cart";
        }

        // ==================================================
        // GENERATE DIGITAL TOKEN
        // ==================================================

        Integer lastToken =
                jdbcTemplate.queryForObject(
                        """
                        SELECT MAX(token_number)
                        FROM orders
                        """,
                        Integer.class
                );

        int tokenNumber;

        if (lastToken == null) {
            tokenNumber = 1;
        } else {
            tokenNumber = lastToken + 1;
        }

        // ==================================================
        // CREATE ORDER
        // ==================================================

        jdbcTemplate.update(
                """
                INSERT INTO orders
                (
                    user_id,
                    token_number,
                    total_amount,
                    status,
                    payment_method,
                    payment_status,
                    payment_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                tokenNumber,
                totalAmount,
                "PLACED",
                "CASH_AT_COUNTER",
                "PENDING",
                null
        );

        // ==================================================
        // GET NEW ORDER ID
        // ==================================================

        Long orderId =
                jdbcTemplate.queryForObject(
                        "SELECT last_insert_rowid()",
                        Long.class
                );

        if (orderId == null) {
            return "redirect:/student/cart";
        }

        // ==================================================
        // FIND FOOD COLUMN
        // ==================================================

        String foodColumn =
                findFoodColumn();

        // ==================================================
        // SAVE ORDER ITEMS
        // ==================================================

        for (Map<String, Object> item : cartItems) {

            int foodId =
                    ((Number) item.get("id"))
                            .intValue();

            int quantity =
                    ((Number) item.get("quantity"))
                            .intValue();

            double price =
                    ((Number) item.get("price"))
                            .doubleValue();

            String sql =
                    """
                    INSERT INTO order_items
                    (order_id, %s, quantity, price)
                    VALUES (?, ?, ?, ?)
                    """.formatted(foodColumn);

            jdbcTemplate.update(
                    sql,
                    orderId,
                    foodId,
                    quantity,
                    price
            );
        }

        // ==================================================
        // CLEAR CART
        // ==================================================

        session.removeAttribute("cart");

        // ==================================================
        // SAVE LAST ORDER INFORMATION
        // ==================================================

        session.setAttribute(
                "lastOrderId",
                orderId
        );

        session.setAttribute(
                "lastTokenNumber",
                tokenNumber
        );

        // ==================================================
        // SHOW ORDER CONFIRMATION
        // ==================================================

        return "redirect:/student/order/" + orderId;
    }


    // ==================================================
    // ORDER CONFIRMATION
    // ==================================================

    @GetMapping("/student/order/{orderId}")
    public String orderConfirmation(
            @PathVariable Long orderId,
            HttpSession session,
            Model model) {

        // Check login
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        int userId =
                ((Number) session.getAttribute("userId"))
                        .intValue();

        // ==================================================
        // GET ORDER
        // ==================================================

        List<Map<String, Object>> orders =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            id,
                            user_id,
                            token_number,
                            total_amount,
                            status,
                            payment_method,
                            payment_status,
                            payment_id,
                            order_time
                        FROM orders
                        WHERE id = ?
                        AND user_id = ?
                        """,
                        orderId,
                        userId
                );

        if (orders.isEmpty()) {
            return "redirect:/student/dashboard";
        }

        Map<String, Object> order =
                orders.get(0);

        // ==================================================
        // GET ORDER ITEMS
        // ==================================================

        String foodColumn =
                findFoodColumn();

        String itemQuery =
                """
                SELECT
                    oi.id,
                    oi.%s AS food_id,
                    oi.quantity,
                    oi.price,
                    f.name,
                    f.category
                FROM order_items oi
                JOIN food_items f
                ON oi.%s = f.id
                WHERE oi.order_id = ?
                """.formatted(
                        foodColumn,
                        foodColumn
                );

        List<Map<String, Object>> orderItems =
                jdbcTemplate.queryForList(
                        itemQuery,
                        orderId
                );

        // ==================================================
        // CALCULATE TOTAL ITEMS
        // ==================================================

        int totalItems = 0;

        for (Map<String, Object> item :
                orderItems) {

            totalItems +=
                    ((Number) item.get("quantity"))
                            .intValue();
        }

        // ==================================================
        // ESTIMATED WAITING TIME
        // ==================================================

        int estimatedWait =
                5 + (totalItems * 3);

        // ==================================================
        // SEND DATA TO PAGE
        // ==================================================

        model.addAttribute(
                "order",
                order
        );

        model.addAttribute(
                "orderItems",
                orderItems
        );

        model.addAttribute(
                "estimatedWait",
                estimatedWait
        );

        return "student/order-confirmation";
    }


    // ==================================================
    // FIND FOOD COLUMN
    // ==================================================

    private String findFoodColumn() {

        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList(
                        "PRAGMA table_info(order_items)"
                );

        String[] possibleColumns = {
                "food_id",
                "food_item_id",
                "menu_item_id",
                "item_id",
                "fooditem_id"
        };

        for (String possible : possibleColumns) {

            for (Map<String, Object> column :
                    columns) {

                Object nameObject =
                        column.get("name");

                if (nameObject != null) {

                    String actualName =
                            nameObject.toString();

                    if (actualName.equalsIgnoreCase(
                            possible)) {

                        return actualName;
                    }
                }
            }
        }

        throw new IllegalStateException(
                "Could not find the food item column in order_items table. "
                + "Available columns: "
                + columns
        );
    }


    // ==================================================
    // GET CART
    // ==================================================

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getCartMap(
            HttpSession session) {

        Object cartObject =
                session.getAttribute("cart");

        if (cartObject == null) {

            Map<Integer, Integer> newCart =
                    new HashMap<>();

            session.setAttribute(
                    "cart",
                    newCart
            );

            return newCart;
        }

        return (Map<Integer, Integer>) cartObject;
    }
}