package com.orderplatform.order_service.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.order_service.dto.BulkFailure;
import com.orderplatform.order_service.dto.BulkProductResponse;
import com.orderplatform.order_service.dto.ProductRequest;
import com.orderplatform.order_service.dto.ProductUpdateRequest;
import com.orderplatform.order_service.model.Product;
import com.orderplatform.order_service.service.ProductService;

import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody @Valid Product product) {

        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getAllProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {

        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request) {

        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper(); // or inject Spring's ObjectMapper bean

    @PostMapping(value = "/bulk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkProductResponse> bulkUpload(@RequestParam("file") MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        List<ProductRequest> requests;
        List<BulkFailure> parseFailures = new ArrayList<>();

        switch (extension) {
            case "json" -> {
                try {
                    requests = MAPPER.readValue(file.getInputStream(),
                            new TypeReference<List<ProductRequest>>() {
                            });
                } catch (JsonProcessingException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Malformed JSON: " + e.getOriginalMessage());
                }
            }
            case "csv" -> requests = parseCsv(file, parseFailures);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only CSV and JSON files are accepted");
        }

        return ResponseEntity.ok(productService.bulkCreateProducts(requests, parseFailures));
    }

    private List<ProductRequest> parseCsv(MultipartFile file, List<BulkFailure> failures) throws IOException {
        List<ProductRequest> requests = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        // try-with-resources closes the parser and reader; BOM stripped for Excel-saved
        // files
        try (Reader reader = new InputStreamReader(
                BOMInputStream.builder().setInputStream(file.getInputStream()).get(),
                StandardCharsets.UTF_8);
                CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {
                String title = record.isMapped("productTitle") ? record.get("productTitle") : null;
                String label = (title == null || title.isBlank()) ? "row " + record.getRecordNumber() : title;
                try {
                    ProductRequest req = new ProductRequest();
                    req.setProductTitle(title);
                    req.setProductDescription(
                            record.isMapped("productDescription") ? record.get("productDescription") : null);
                    req.setPrice(new BigDecimal(record.get("price"))); // throws on missing column or bad number
                    requests.add(req);
                } catch (IllegalArgumentException e) { // NumberFormatException extends this
                    failures.add(new BulkFailure(label, "Invalid price format"));
                }
            }
        }
        return requests;
    }
}
