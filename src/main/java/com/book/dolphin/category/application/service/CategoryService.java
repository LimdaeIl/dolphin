package com.book.dolphin.category.application.service;

import static com.book.dolphin.category.domain.entity.Category.createChild;
import static com.book.dolphin.category.domain.entity.Category.createRoot;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.ALREADY_PATH;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.DUPLICATE_SLUG_BY_PARENT;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.DUPLICATE_SLUG_BY_ROOT;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND;

import com.book.dolphin.category.application.dto.request.CreateCategoryRequest;
import com.book.dolphin.category.application.dto.response.CreateCategoryResponse;
import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryClosure;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.exception.CategoryErrorCode;
import com.book.dolphin.category.domain.exception.CategoryException;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "CategoryService")
@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryClosureRepository categoryClosureRepository;

    @Transactional
    public CreateCategoryResponse create(CreateCategoryRequest request) {

        final Long parentId = request.parentId();
        final String name = request.name().trim();
        final String slug = normalizeSlug(request.slug());
        final int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        final CategoryStatus status =
                request.status() == null ? CategoryStatus.READY : request.status();

        // 0) 부모 로딩 (null이면 루트)
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new CategoryException(PARENT_CATEGORY_NOT_FOUND, parentId));
        }

        // 1) 선제 중복 검사 (UX용) — 레이스는 고려하지 않음
        final String path = (parent == null) ? ("/" + slug) : (parent.getPath() + "/" + slug);
        log.info("path : {}", path);

        if (parent == null) {
            if (categoryRepository.existsByParentIsNullAndSlug(slug)) {
                throw new CategoryException(DUPLICATE_SLUG_BY_ROOT, slug);
            }
        } else {
            if (categoryRepository.existsByParentIdAndSlug(parent.getId(), slug)) {
                throw new CategoryException(DUPLICATE_SLUG_BY_PARENT, parent.getId(), slug);
            }
        }
        if (categoryRepository.existsByPath(path)) {
            throw new CategoryException(ALREADY_PATH, path);
        }

        // 2) 엔티티 생성 & 저장 (예외는 그대로 바깥으로 던짐)
        final Category saved = (parent == null)
                ? categoryRepository.save(createRoot(name, slug, sortOrder, status))
                : categoryRepository.save(createChild(name, slug, parent, path, sortOrder, status));

        // 3) 클로저 생성: self(0) + (부모의 모든 조상 → 신규, depth+1)
        final List<CategoryClosure> closures = new ArrayList<>();
        closures.add(CategoryClosure.create(saved, saved, 0)); // self

        if (parent != null) {
            var parentAncestors = categoryClosureRepository.findAllAncestorsOf(parent.getId());
            for (var pa : parentAncestors) {
                closures.add(CategoryClosure.create(pa.getAncestor(), saved, pa.getDepth() + 1));
            }
        }
        categoryClosureRepository.saveAll(closures);

        // 4) 응답
        return CreateCategoryResponse.of(
                saved.getId(),
                saved.getParent() == null ? null : saved.getParent().getId(),
                saved.getName(),
                saved.getSlug(),
                saved.getPath(),
                saved.getDepth(),
                saved.getSortOrder(),
                saved.getStatus()
        );
    }

    private static String normalizeSlug(String raw) {
        // 슬러그 정규화(기본): trim → 소문자 → 공백→- → 연속 하이픈 1개로 → 허용문자만
        String s = raw == null ? "" : raw.trim().toLowerCase();
        s = s.replaceAll("\\s+", "-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("[^a-z0-9-]", "");
        if (s.isBlank()) {
            throw new CategoryException(CategoryErrorCode.EMPTY_SLUG);
        }
        return s;
    }
}
