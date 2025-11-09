package com.book.dolphin.product.domain.entity;

import com.book.dolphin.category.domain.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "product_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_category", columnNames = {"product_id",
                        "category_id"})
        },
        indexes = {
                @Index(name = "idx_pc_category_sort", columnList = "category_id, sort_key, product_id"),
                @Index(name = "idx_pc_product", columnList = "product_id")
        }
)
@Entity
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_category_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "sort_key", nullable = false)
    private int sortKey;

    public ProductCategory(Product product, Category category, boolean primary, int sortKey) {
        this.product = product;
        this.category = category;
        this.primary = primary;
        this.sortKey = sortKey;
    }

}