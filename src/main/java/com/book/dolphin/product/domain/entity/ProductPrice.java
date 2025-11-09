package com.book.dolphin.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "product_prices",
        indexes = {
                @Index(name = "idx_price_product_type_active", columnList = "product_id, type, active"),
                @Index(name = "idx_price_valid_from", columnList = "valid_from"),
                @Index(name = "idx_price_valid_until", columnList = "valid_until")
        }
)
@Entity
public class ProductPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_price_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PriceType type; // LIST(정상가), SALE(할인가)

    @Embedded
    private Money amount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    private ProductPrice(Product product, PriceType type, Money amount,
            LocalDateTime validFrom, LocalDateTime validUntil) {
        this.product = product;
        this.type = type;
        this.amount = amount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public static ProductPrice listPrice(Product p, Money amount) {
        return new ProductPrice(p, PriceType.LIST, amount, null, null);
    }

    public static ProductPrice salePrice(Product p, Money amount, DateRange period) {
        return new ProductPrice(p, PriceType.SALE, amount, period.from(), period.until());
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isEffectiveNow() {
        if (!active) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = (validFrom == null) || !now.isBefore(validFrom);
        boolean beforeEnd = (validUntil == null) || !now.isAfter(validUntil);
        if (type == PriceType.LIST) {
            return active; // 상시가
        }
        return afterStart && beforeEnd;
    }

    public boolean overlaps(DateRange r) {
        if (r == null) {
            return false;
        }
        LocalDateTime s = validFrom, e = validUntil;
        // 열린구간 포함 간단 판정
        if (s == null || r.until() == null) {
            return true;
        }
        if (e == null || r.from() == null) {
            return true;
        }
        return !e.isBefore(r.from()) && !s.isAfter(r.until());
    }
}