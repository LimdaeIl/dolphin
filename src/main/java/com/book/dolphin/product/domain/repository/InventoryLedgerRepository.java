package com.book.dolphin.product.domain.repository;

import com.book.dolphin.product.domain.entity.InventoryLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, Long> {

}
