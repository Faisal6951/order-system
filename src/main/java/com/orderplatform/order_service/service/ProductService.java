package com.orderplatform.order_service.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.orderplatform.order_service.dto.BulkFailure;
import com.orderplatform.order_service.dto.BulkProductResponse;
import com.orderplatform.order_service.dto.ProductRequest;
import com.orderplatform.order_service.dto.ProductUpdateRequest;
import com.orderplatform.order_service.exception.ProductNotFoundException;
import com.orderplatform.order_service.model.Inventory;
import com.orderplatform.order_service.model.Product;
import com.orderplatform.order_service.repository.InventoryRepository;
import com.orderplatform.order_service.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProduct(savedProduct);
        inventory.setStockAvaiable(10);
        inventoryRepository.save(inventory);
        return savedProduct;
    }

    @Cacheable(key = "#id", value = "products")
    public Product getProductById(Long id) {

        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setProductTitle(request.getProductTitle());
        product.setPrice(request.getPrice());
        product.setProductDescription(request.getProductDescription());
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        productRepository.deleteById(id);
    }

    @Transactional
    public BulkProductResponse bulkCreateProducts(List<ProductRequest> requests,
            List<BulkFailure> parseFailures) {
        List<BulkFailure> failures = new ArrayList<>(parseFailures); // seed with parse failures
        List<ProductRequest> candidates = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();

        // Pass 1: cheap in-memory validation
        if (requests != null) {
            for (ProductRequest request : requests) {
                if (request == null) {
                    failures.add(new BulkFailure("unknown", "Empty entry"));
                    continue;
                }
                String title = request.getProductTitle();
                if (title == null || title.isBlank()) {
                    failures.add(new BulkFailure("unknown", "Title cannot be blank"));
                    continue;
                }
                String normalized = title.trim();
                if (!seenTitles.add(normalized)) {
                    failures.add(new BulkFailure(normalized, "Duplicate title in file"));
                    continue;
                }
                if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    failures.add(new BulkFailure(normalized, "Invalid price"));
                    continue;
                }
                request.setProductTitle(normalized);
                candidates.add(request);
            }
        }

        // Pass 2: one batched DB lookup instead of one query per row
        Set<String> existing = candidates.isEmpty() ? Set.of()
                : productRepository.findExistingTitles(
                        candidates.stream().map(ProductRequest::getProductTitle).toList());

        List<Product> validProducts = new ArrayList<>();
        for (ProductRequest request : candidates) {
            if (existing.contains(request.getProductTitle())) {
                failures.add(new BulkFailure(request.getProductTitle(), "Product already exists in database"));
                continue;
            }
            Product product = new Product();
            product.setProductTitle(request.getProductTitle());
            product.setProductDescription(request.getProductDescription());
            product.setPrice(request.getPrice());
            validProducts.add(product);
        }

        List<Product> saved = validProducts.isEmpty() ? List.of() : productRepository.saveAll(validProducts);

        if (!saved.isEmpty()) {
            List<Inventory> inventories = new ArrayList<>();
            for (Product product : saved) {
                Inventory inventory = new Inventory();
                inventory.setProduct(product);
                inventory.setStockAvaiable(10);
                inventories.add(inventory);
            }
            inventoryRepository.saveAll(inventories);
        }

        return new BulkProductResponse(saved.size(), failures.size(), saved, failures);
    }
}
