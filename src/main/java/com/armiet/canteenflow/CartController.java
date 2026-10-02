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
public class CartController {

    private final JdbcTemplate jdbcTemplate;

    public CartController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==============================
    // SHOW CART
    // ==============================
    @GetMapping("/student/cart")
    public String showCart(HttpSession session, Model model) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> cart =
                getCart(session);

        double total = calculateTotal(cart);

        model.addAttribute("cartItems", cart);
        model.addAttribute("cartTotal", total);

        return "student/cart";
    }


    // ==============================
    // ADD FOOD TO CART
    // ==============================
    @PostMapping("/student/cart/add")
    public String addToCart(
            @RequestParam int foodId,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        Map<Integer, Integer> cart = getCartMap(session);

        cart.put(
                foodId,
                cart.getOrDefault(foodId, 0) + 1
        );

        session.setAttribute("cart", cart);

        return "redirect:/student/cart";
    }


    // ==============================
    // INCREASE QUANTITY
    // ==============================
    @PostMapping("/student/cart/increase")
    public String increaseQuantity(
            @RequestParam int foodId,
            HttpSession session) {

        Map<Integer, Integer> cart = getCartMap(session);

        cart.put(
                foodId,
                cart.getOrDefault(foodId, 0) + 1
        );

        session.setAttribute("cart", cart);

        return "redirect:/student/cart";
    }


    // ==============================
    // DECREASE QUANTITY
    // ==============================
    @PostMapping("/student/cart/decrease")
    public String decreaseQuantity(
            @RequestParam int foodId,
            HttpSession session) {

        Map<Integer, Integer> cart = getCartMap(session);

        if (cart.containsKey(foodId)) {

            int quantity = cart.get(foodId) - 1;

            if (quantity <= 0) {
                cart.remove(foodId);
            } else {
                cart.put(foodId, quantity);
            }
        }

        session.setAttribute("cart", cart);

        return "redirect:/student/cart";
    }


    // ==============================
    // REMOVE ITEM
    // ==============================
    @PostMapping("/student/cart/remove")
    public String removeItem(
            @RequestParam int foodId,
            HttpSession session) {

        Map<Integer, Integer> cart = getCartMap(session);

        cart.remove(foodId);

        session.setAttribute("cart", cart);

        return "redirect:/student/cart";
    }


    // ==============================
    // CLEAR CART
    // ==============================
    @PostMapping("/student/cart/clear")
    public String clearCart(HttpSession session) {

        session.removeAttribute("cart");

        return "redirect:/student/cart";
    }


    // ==============================
    // GET CART MAP
    // ==============================
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


    // ==============================
    // GET CART FOOD DETAILS
    // ==============================
    private List<Map<String, Object>> getCart(
            HttpSession session) {

        Map<Integer, Integer> cart =
                getCartMap(session);

        List<Map<String, Object>> cartItems =
                new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry :
                cart.entrySet()) {

            int foodId = entry.getKey();
            int quantity = entry.getValue();

            List<Map<String, Object>> foods =
                    jdbcTemplate.queryForList(
                            """
                            SELECT id, name, description,
                                   category, price, image_url
                            FROM food_items
                            WHERE id = ? AND available = 1
                            """,
                            foodId
                    );

            if (!foods.isEmpty()) {

                Map<String, Object> food =
                        new HashMap<>(foods.get(0));

                double price =
                        ((Number) food.get("price"))
                                .doubleValue();

                food.put(
                        "quantity",
                        quantity
                );

                food.put(
                        "itemTotal",
                        price * quantity
                );

                cartItems.add(food);
            }
        }

        return cartItems;
    }


    // ==============================
    // CALCULATE TOTAL
    // ==============================
    private double calculateTotal(
            List<Map<String, Object>> cartItems) {

        double total = 0;

        for (Map<String, Object> item :
                cartItems) {

            total +=
                    ((Number) item.get("itemTotal"))
                            .doubleValue();
        }

        return total;
    }
}