package com.shop.backend.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseBootstrap {
    public DatabaseBootstrap(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT");
        jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ");
        jdbcTemplate.execute("UPDATE products SET image_url = '/media/shop-images/products/laptop.svg' WHERE name = 'Laptop' AND (image_url IS NULL OR image_url = '')");
        jdbcTemplate.execute("UPDATE products SET image_url = '/media/shop-images/products/headphones.svg' WHERE name = 'Headphones' AND (image_url IS NULL OR image_url = '')");
        jdbcTemplate.execute("UPDATE products SET image_url = '/media/shop-images/products/mouse.svg' WHERE name = 'Mouse' AND (image_url IS NULL OR image_url = '')");
    }
}
