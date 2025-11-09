package com.book.dolphin.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "product_media",
        indexes = {
                @Index(name = "idx_media_product_type_sort", columnList = "product_id, type, sort_key")
        }
)
@Entity
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_media_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MediaType type; // REPRESENTATIVE or CONTENT

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_key", nullable = false)
    private int sortKey;

    private ProductMedia(Product product, MediaType type, String url, String altText, int sortKey) {
        this.product = product;
        this.type = type;
        this.url = url;
        this.altText = altText;
        this.sortKey = sortKey;
    }

    public static ProductMedia representative(Product p, String url, String altText, int sortKey) {
        return new ProductMedia(p, MediaType.REPRESENTATIVE, url, altText, sortKey);
    }
    public static ProductMedia content(Product p, String url, String altText, int sortKey) {
        return new ProductMedia(p, MediaType.CONTENT, url, altText, sortKey);
    }

    public void changeSortKey(int k) { this.sortKey = k; }
}