package com.shop.backend.controller;

import com.shop.backend.model.Product;
import com.shop.backend.service.MediaService;
import com.shop.backend.service.ProductService;
import com.shop.backend.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final SessionService sessionService;
    private final ProductService productService;
    private final MediaService mediaService;

    public AdminController(SessionService sessionService, ProductService productService, MediaService mediaService) {
        this.sessionService = sessionService;
        this.productService = productService;
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/products", consumes = "multipart/form-data")
    public ResponseEntity<?> createProduct(@RequestHeader(name = "X-Session-Token", required = false) String token,
                                           @RequestParam String name,
                                           @RequestParam Double price,
                                           @RequestParam("image") MultipartFile image) {
        String login = sessionService.getLoginByToken(token);
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid session");
        }
        if (!"admin".equals(login)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("admin access required");
        }

        try {
            String imageUrl = mediaService.uploadProductImage(image);
            Product saved = productService.createProduct(name, price, imageUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
