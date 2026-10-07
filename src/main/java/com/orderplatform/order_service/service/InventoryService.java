package com.orderplatform.order_service.service;

import org.springframework.stereotype.Service;

import com.orderplatform.order_service.dto.InventoryRequest;
import com.orderplatform.order_service.exception.ProductNotFoundException;
import com.orderplatform.order_service.model.Inventory;
import com.orderplatform.order_service.model.Product;
import com.orderplatform.order_service.repository.InventoryRepository;
import com.orderplatform.order_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public Inventory createInventory(InventoryRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(request.getProductId()));

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setStockAvaiable(request.getStockAvailable());

        return inventoryRepository.save(inventory);

    }

    public Inventory getInventoryByProductId(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return inventoryRepository.findByProduct(product)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + id));
    }

    public Inventory updateInventory(Long id, InventoryRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        Inventory inventory = inventoryRepository.findByProduct(product)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + request.getProductId()));

        inventory.setStockAvaiable(request.getStockAvailable());

        return inventoryRepository.save(inventory);
    }
}
