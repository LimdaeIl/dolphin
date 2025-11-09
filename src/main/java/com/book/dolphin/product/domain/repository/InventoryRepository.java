package com.book.dolphin.product.domain.repository;

import com.book.dolphin.product.domain.entity.Inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductIdAndSkuCode(Long productId, String skuCode);

    Optional<Inventory> findByProductIdAndSkuCode(Long productId, String skuCode);

    // 점검 편의용(단건 샘플) — 운영에선 페이징 목록 API 권장
    Optional<Inventory> findFirstBySkuCodeOrderByIdAsc(String skuCode);
    Optional<Inventory> findFirstByProductIdOrderByIdAsc(Long productId);
}
