package com.book.dolphin.category.presentation;

import com.book.dolphin.category.application.dto.request.CreateCategoryRequest;
import com.book.dolphin.category.application.dto.request.MoveCategoryRequest;
import com.book.dolphin.category.application.dto.request.UpdateCategoryRequest;
import com.book.dolphin.category.application.dto.response.CategoryDetailResponse;
import com.book.dolphin.category.application.dto.response.CreateCategoryResponse;
import com.book.dolphin.category.application.dto.response.MegaMenuResponse;
import com.book.dolphin.category.application.dto.response.MoveCategoryResponse;
import com.book.dolphin.category.application.service.CategoryService;
import com.book.dolphin.common.response.ApiResponse;
import com.book.dolphin.common.response.ResultCode;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreateCategoryResponse>> create(
            @RequestBody @Valid CreateCategoryRequest request
    ) {
        CreateCategoryResponse response = categoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        ResultCode.SUCCESS,
                        null,
                        response)
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MegaMenuResponse>> getMegaMenu(
            @RequestParam(required = false) String view,
            @RequestParam(required = false) Long selectedRootId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        if (!"mega".equalsIgnoreCase(view)) {
            // 다음 단계에서 상세 GET 등 다른 view로 분기 예정
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(ResultCode.ERROR, "지원하지 않는 view 입니다.", null));
        }
        MegaMenuResponse res = categoryService.getMegaMenu(selectedRootId, activeOnly);
        return ResponseEntity.ok(new ApiResponse<>(ResultCode.SUCCESS, null, res));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> getDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(required = false, defaultValue = "children,breadcrumb") String include
    ) {
        Set<String> includeSet = Arrays.stream(include.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        CategoryDetailResponse response = categoryService.getDetail(id, activeOnly, includeSet);
        return ResponseEntity.ok(new ApiResponse<>(ResultCode.SUCCESS, null, response));
    }

    // 경로 기반 조회 (옵션)(TODO)
    // 카테고리별 상품 페이징(확장)(TODO)

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateBasic(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCategoryRequest request
    ) {
        categoryService.updateBasic(id, request);
        return ResponseEntity.ok(new ApiResponse<>(ResultCode.SUCCESS, null, null));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<ApiResponse<MoveCategoryResponse>> move(
            @PathVariable Long id,
            @RequestBody @Valid MoveCategoryRequest request
    ) {
        MoveCategoryResponse move = categoryService.move(id, request.newParentId());
        return ResponseEntity.ok(new ApiResponse<>(ResultCode.SUCCESS, null, move));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteCategory(
            @PathVariable Long id
    ) {
        int i = categoryService.hardDeleteSubtree(id);

        return ResponseEntity.ok(new ApiResponse<>(ResultCode.SUCCESS, null, i));
    }

    // 슬러그 수정(TODO)
    // 재정렬(TODO)
    // 상태전환(TODO)
    // 소프트 삭제(TODO)
}
