package com.book.dolphin.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product_variants",
        indexes = {
                @Index(name = "idx_variant_sku", columnList = "sku_code"),
                @Index(name = "idx_variant_product", columnList = "product_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_variant_product_sku",
                columnNames = {"product_id", "sku_code"}
        )
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 상품 내 유니크 SKU
     */
    @Column(name = "sku_code", length = 64, nullable = false)
    private String skuCode;

    /**
     * 대표 옵션들 (필요 시 Enum 또는 코드테이블/JSON으로 확장)
     */
    @Column(name = "option_size", length = 20)
    private String size;

    @Column(name = "option_color", length = 30)
    private String color;

    /**
     * 바코드/GTIN/MPN 등 외부 식별자
     */
    @Column(name = "barcode", length = 64)
    private String barcode;

    /**
     * 무게/부피 등 배송 스펙
     */
    @Column(name = "weight_g")
    private Long weightG;

    @Column(name = "length_mm")
    private Long lengthMm;

    @Column(name = "width_mm")
    private Long widthMm;

    @Column(name = "height_mm")
    private Long heightMm;

    /**
     * 자유 확장용 JSON(예: {"fabric":"cotton","fit":"slim"})
     */
    @Lob
    @Column(name = "attributes_json", columnDefinition = "text")
    private String attributesJson;

    @Builder
    public ProductVariant(
            Product product,
            String skuCode,
            String size,
            String color,
            String barcode,
            Long weightG,
            Long lengthMm,
            Long widthMm,
            Long heightMm,
            String attributesJson) {
        this.product = product;
        this.skuCode = skuCode;
        this.size = size;
        this.color = color;
        this.barcode = barcode;
        this.weightG = weightG;
        this.lengthMm = lengthMm;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.attributesJson = attributesJson;
    }

    public static ProductVariant of(Product product, String skuCode) {
        return ProductVariant.builder()
                .product(product)
                .skuCode(skuCode)
                .build();
    }
}