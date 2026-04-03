package com.shop.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.backend.model.OrderEntity;
import com.shop.backend.model.Product;
import com.shop.backend.repository.OrderRepository;
import com.shop.backend.repository.ProductRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    public record CreateOrderItem(Long productId, Integer quantity) {}

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        RabbitTemplate rabbitTemplate,
                        ObjectMapper objectMapper,
                        @Value("${shop.queue.orders}") String queueName) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    public List<OrderEntity> createOrders(String login, List<CreateOrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("cart is empty");
        }

        List<Long> productIds = items.stream().map(CreateOrderItem::productId).toList();
        Set<Long> existingProductIds = productRepository.findAllById(productIds)
                .stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        List<OrderEntity> toSave = new ArrayList<>();
        for (CreateOrderItem item : items) {
            if (item.productId() == null || !existingProductIds.contains(item.productId())) {
                throw new IllegalArgumentException("product not found: " + item.productId());
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new IllegalArgumentException("invalid quantity for product: " + item.productId());
            }

            OrderEntity order = new OrderEntity();
            order.setUserLogin(login);
            order.setProductId(item.productId());
            order.setQuantity(item.quantity());
            order.setStatus("ORDERED");
            toSave.add(order);
        }

        List<OrderEntity> saved = orderRepository.saveAll(toSave);
        saved.forEach(this::publishOrderEvent);
        return saved;
    }

    public List<OrderEntity> ordersForUser(String login) {
        return orderRepository.findByUserLoginOrderByCreatedAtDesc(login);
    }

    private void publishOrderEvent(OrderEntity order) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "orderId", order.getId(),
                    "userLogin", order.getUserLogin(),
                    "productId", order.getProductId(),
                    "quantity", order.getQuantity(),
                    "status", order.getStatus()
            ));
            rabbitTemplate.convertAndSend(queueName, payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize order event", e);
        }
    }
}
