package com.erplab.inventory.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

	Optional<StockItem> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

}
