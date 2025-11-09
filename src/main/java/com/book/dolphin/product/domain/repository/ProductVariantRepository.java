package com.book.dolphin.product.domain.repository;

import com.book.dolphin.product.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsByProductIdAndSkuCode(Long aLong, String s);
}
