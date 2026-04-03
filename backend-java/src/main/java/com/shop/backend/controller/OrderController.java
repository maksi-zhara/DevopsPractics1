package com.shop.backend.controller;

import com.shop.backend.model.OrderEntity;
import com.shop.backend.model.Product;
import com.shop.backend.repository.ProductRepository;
import com.shop.backend.service.OrderService;
import com.shop.backend.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final String DELIVERED_MESSAGE = "Заказ доставлен в магазин по адресу Университетская площадь, д.1";

    private final SessionService sessionService;
    private final OrderService orderService;
    private final ProductRepository productRepository;

    public OrderController(SessionService sessionService,
                           OrderService orderService,
                           ProductRepository productRepository) {
        this.sessionService = sessionService;
        this.orderService = orderService;
        this.productRepository = productRepository;
    }

    public record CartItemRequest(Long productId, Integer quantity) {}

    public record CheckoutRequest(List<CartItemRequest> items) {}

    public record OrderView(Long id,
                            Long productId,
                            String productName,
                            Integer quantity,
                            String status,
                            Instant createdAt) {}

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestHeader(name = "X-Session-Token", required = false) String token,
                                      @RequestBody CheckoutRequest request) {
        String login = sessionService.getLoginByToken(token);
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid session");
        }

        try {
            List<OrderService.CreateOrderItem> items = (request == null || request.items() == null)
                    ? List.of()
                    : request.items().stream()
                    .map(i -> new OrderService.CreateOrderItem(i.productId(), i.quantity()))
                    .toList();
            List<OrderEntity> saved = orderService.createOrders(login, items);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> myOrders(@RequestHeader(name = "X-Session-Token", required = false) String token) {
        String login = sessionService.getLoginByToken(token);
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid session");
        }

        List<OrderEntity> orders = orderService.ordersForUser(login);
        Map<Long, Product> productMap = productRepository.findAllById(
                        orders.stream().map(OrderEntity::getProductId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<OrderView> result = orders.stream().map(o -> {
            Product p = productMap.get(o.getProductId());
            return new OrderView(
                    o.getId(),
                    o.getProductId(),
                    p == null ? "Unknown product" : p.getName(),
                    o.getQuantity(),
                    mapStatus(o.getStatus()),
                    o.getCreatedAt()
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

    private String mapStatus(String status) {
        if ("ORDERED".equalsIgnoreCase(status) || "заказано".equalsIgnoreCase(status)) {
            return "заказано";
        }
        if ("Обработано".equalsIgnoreCase(status) || "PROCESSED".equalsIgnoreCase(status)) {
            return DELIVERED_MESSAGE;
        }
        return status;
    }
}
