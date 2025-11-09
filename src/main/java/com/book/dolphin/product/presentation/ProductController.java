package com.book.dolphin.product.presentation;

import com.book.dolphin.common.response.ApiResponse;
import com.book.dolphin.product.application.dto.request.ProductCreateRequest;
import com.book.dolphin.product.application.dto.response.ProductResponse;
import com.book.dolphin.product.application.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@RestController
public class ProductController {

    private final ProductService productService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
