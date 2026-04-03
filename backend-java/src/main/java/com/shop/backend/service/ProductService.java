package com.shop.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.backend.model.Product;
import com.shop.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String cacheKey;
    private final long ttlSeconds;

    public ProductService(ProductRepository productRepository,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper,
                          @Value("${shop.cache.products-key:cache:products}") String cacheKey,
                          @Value("${shop.cache.products-ttl-seconds:60}") long ttlSeconds) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheKey = cacheKey;
        this.ttlSeconds = ttlSeconds;
    }

    public List<Product> getProducts() {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, new TypeReference<>() {});
                } catch (JsonProcessingException ignored) {
                }
            }
        } catch (RuntimeException ignored) {
            // If Redis is unavailable, continue with DB fallback.
        }

        List<Product> products = productRepository.findAll();
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(products), ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException | RuntimeException ignored) {
        }
        return products;
    }

    public Product createProduct(String name, Double price, String imageUrl) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("price must be greater than 0");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl is required");
        }

        Product product = new Product();
        product.setName(name.trim());
        product.setPrice(price);
        product.setImageUrl(imageUrl);

        Product saved = productRepository.save(product);
        invalidateProductsCache();
        return saved;
    }

    private void invalidateProductsCache() {
        try {
            redisTemplate.delete(cacheKey);
        } catch (RuntimeException ignored) {
        }
    }
}
