package com.book.dolphin.product.domain.entity;


import com.book.dolphin.product.domain.exception.ProductErrorCode;
import com.book.dolphin.product.domain.exception.ProductException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product_sku", columnNames = {
                "product_id", "sku_code"}))
@Entity
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, length = 64)
    private String skuCode;

    @Column(name = "on_hand", nullable = false)
    private long onHand;       // 입고/조정으로 증가·감소

    @Column(name = "allocated", nullable = false)
    private long allocated;    // 주문확정 전 임시 할당 수량

    @Column(name = "safety_stock", nullable = false)
    private long safetyStock;  // 최소 유지 수량(가용 계산에 반영)

    @Column(name = "backorderable", nullable = false)
    private boolean backorderable; // 품절 시 주문 허용 여부

    @Version
    private long version;      // 낙관적 락

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Inventory of(Product product, String skuCode,
            long onHand, long safetyStock, boolean backorderable) {
        Inventory inv = new Inventory();
        inv.product = product;
        inv.skuCode = skuCode;
        inv.onHand = onHand;
        inv.allocated = 0L;
        inv.safetyStock = safetyStock;
        inv.backorderable = backorderable;
        return inv;
    }

    // ---- 도메인 로직 ----
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
            throw new ProductException(ProductErrorCode.DEDUCT_MORE_THEN_AMOUNT, qty);
        }
        this.onHand -= qty;
    }
}
