package com.book.dolphin.product.domain.repository;

import com.book.dolphin.product.domain.entity.Inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByVariantId(Long variantId);

    Optional<Inventory> findByVariantId(Long variantId);

    Optional<Inventory> findFirstBySkuCodeOrderByIdAsc(String skuCode);
}
