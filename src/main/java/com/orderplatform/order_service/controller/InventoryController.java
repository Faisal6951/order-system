package com.orderplatform.order_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderplatform.order_service.dto.InventoryRequest;
import com.orderplatform.order_service.model.Inventory;
import com.orderplatform.order_service.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<Inventory> createInventory(@RequestBody @Valid InventoryRequest request) {

        Inventory created = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventory> getInventoryByProductId(@PathVariable Long id) {

        return ResponseEntity.ok(inventoryService.getInventoryByProductId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Inventory> updateInventoryByProductId(
            @PathVariable Long id,
            @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventory(id, request));
    }
}
