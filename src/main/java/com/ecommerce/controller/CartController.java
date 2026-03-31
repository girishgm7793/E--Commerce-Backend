package com.ecommerce.controller;

import com.ecommerce.dto.CartRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCart(auth.getName()));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addItem(Authentication auth, @RequestBody CartRequest request) {
        return ResponseEntity.ok(cartService.addItem(auth.getName(), request));
    }

    @PutMapping("/update/{productId}")
    public ResponseEntity<Cart> updateItem(Authentication auth,
                                           @PathVariable Long productId,
                                           @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateItem(auth.getName(), productId, quantity));
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Cart> removeItem(Authentication auth, @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(auth.getName(), productId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(Authentication auth) {
        cartService.clearCart(auth.getName());
        return ResponseEntity.ok("Cart cleared");
    }
}
