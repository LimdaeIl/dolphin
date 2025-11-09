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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inventory_ledgers", indexes = {
        @Index(name="idx_ledger_inventory", columnList = "inventory_id, occurred_at")
})
@Entity
public class InventoryLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_ledger_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="inventory_id", nullable=false)
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(name="event_type", nullable=false, length=30)
    private LedgerEventType eventType; // INBOUND, ADJUST, ALLOCATE, DEALLOCATE, SHIP, CANCEL, RETURN

    @Column(name="quantity", nullable=false)
    private long quantity; // +/-

    @Column(name="reason", length=255)
    private String reason;

    @Column(name="occurred_at", nullable=false)
    private LocalDateTime occurredAt;

    @Builder
    public InventoryLedger(Inventory inventory, LedgerEventType eventType, long quantity, String reason) {
        this.inventory = inventory;
        this.eventType = eventType;
        this.quantity = quantity;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    public enum LedgerEventType { INBOUND, ADJUST, ALLOCATE, DEALLOCATE, SHIP, CANCEL, RETURN }
}
