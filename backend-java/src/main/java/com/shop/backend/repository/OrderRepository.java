package com.shop.backend.repository;

import com.shop.backend.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserLoginOrderByCreatedAtDesc(String userLogin);
}
