package com.book.dolphin.product.domain.entity;


import com.book.dolphin.product.domain.exception.ProductErrorCode;
import com.book.dolphin.product.domain.exception.ProductException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventories",
        indexes = {
                @Index(name = "idx_inventory_sku", columnList = "sku_code")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_variant", columnNames = "variant_id")
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    /**
     * 재고는 변형(Variant) 단위
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    /**
     * 운영/조회 편의를 위한 디노말라이즈된 SKU 사본(선택) - 유지하려면 생성 시 variant.getSkuCode()로 세팅 - 굳이 보관하지 않을 거면 이 필드
     * 제거해도 됨
     */
    @Column(name = "sku_code", length = 64, nullable = false)
    private String skuCode;

    @Column(name = "on_hand", nullable = false)
    private long onHand;

    @Column(name = "allocated", nullable = false)
    private long allocated;

    @Column(name = "safety_stock", nullable = false)
    private long safetyStock;

    @Column(name = "backorderable", nullable = false)
    private boolean backorderable;

    @Version
    private long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 팩토리
     */
    public static Inventory of(ProductVariant variant,
            long onHand,
            long safetyStock,
            boolean backorderable) {
        Inventory inv = new Inventory();
        inv.variant = Objects.requireNonNull(variant);
        inv.skuCode = variant.getSkuCode();       // 캐싱
        inv.onHand = onHand;
        inv.allocated = 0L;
        inv.safetyStock = safetyStock;
        inv.backorderable = backorderable;
        return inv;
    }

    // ===== 도메인 로직 =====
    public long available() {
        long avail = onHand - allocated - safetyStock;
        return Math.max(avail, 0L);
    }

    public boolean canAllocate(long qty) {
        if (qty <= 0) {
            return false;
        }
        if (backorderable) {
            return true;
        }
        return available() >= qty;
    }

    public void allocate(long qty) {
        if (!canAllocate(qty)) {
            throw new ProductException(ProductErrorCode.INVENTORY_OUT_OF_STOCK, qty);
        }
        this.allocated += qty;
    }

    public void deallocate(long qty) {
        if (qty <= 0 || qty > allocated) {
            throw new ProductException(ProductErrorCode.INCORRECT_DEALLOCATION_QUANTITY, qty);
        }
        this.allocated -= qty;
    }

    public void increaseOnHand(long qty) {
        if (qty <= 0) {
            throw new ProductException(ProductErrorCode.QUANTITY_IN_STOCK_MINIMUM_ONE, qty);
        }
        this.onHand += qty;
    }

    public void decreaseOnHand(long qty) {
        if (qty <= 0 || qty > onHand) {
            throw new ProductException(ProductErrorCode.DEDUCT_MORE_THAN_AMOUNT, qty);
        }
        this.onHand -= qty;
    }
}

