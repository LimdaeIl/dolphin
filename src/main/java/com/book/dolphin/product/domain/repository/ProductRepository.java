package com.book.dolphin.product.domain.repository;

import com.book.dolphin.product.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
