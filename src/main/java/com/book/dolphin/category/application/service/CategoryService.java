package com.book.dolphin.category.application.service;

import static com.book.dolphin.category.domain.entity.Category.createChild;
import static com.book.dolphin.category.domain.entity.Category.createRoot;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.ALREADY_PATH;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.DUPLICATE_SLUG_BY_PARENT;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.DUPLICATE_SLUG_BY_ROOT;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND;

import com.book.dolphin.category.application.dto.request.CreateCategoryRequest;
import com.book.dolphin.category.application.dto.response.CategoryDetailResponse;
import com.book.dolphin.category.application.dto.response.CreateCategoryResponse;
import com.book.dolphin.category.application.dto.response.MegaMenuResponse;
import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryClosure;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.exception.CategoryErrorCode;
import com.book.dolphin.category.domain.exception.CategoryException;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CategoryService")
public class CategoryService {

    private static final int MAX_DEPTH = 6;

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
        final String imageUrl = request.imageUrl();

        // 0) 부모 로딩
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new CategoryException(PARENT_CATEGORY_NOT_FOUND, parentId));
        }

        // 0-1) 깊이 검증 (루트=0, 부모가 있으면 부모+1)
        final int newDepth = (parent == null) ? 0 : parent.getDepth() + 1;
        if (newDepth > MAX_DEPTH) {
            throw new CategoryException(
                    CategoryErrorCode.MAX_DEPTH_EXCEEDED, MAX_DEPTH, parentId
            );
        }

        // 1) 선제 중복 검사 (UX용)
        final String path = (parent == null) ? ("/" + slug) : (parent.getPath() + "/" + slug);
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

        // 2) 저장
        final Category saved = (parent == null)
                ? categoryRepository.save(createRoot(name, slug, sortOrder, status, imageUrl))
                : categoryRepository.save(
                        createChild(name, slug, parent, sortOrder, status, imageUrl));

        // 3) 클로저 생성
        final List<CategoryClosure> closures = new ArrayList<>();
        closures.add(CategoryClosure.create(saved, saved, 0)); // self
        if (parent != null) {
            var parentAncestors = categoryClosureRepository.findAllAncestorsOf(parent.getId());
            for (var pa : parentAncestors) {
                // pa.depth 는 ancestor→parent 까지의 거리, 신규 노드는 +1
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

    @Transactional(readOnly = true)
    public MegaMenuResponse getMegaMenu(Long selectedRootId, boolean activeOnly) {
        // 1) 루트 목록 조회
        List<Category> roots = categoryRepository.findRoots(activeOnly);
        if (roots.isEmpty()) {
            return new MegaMenuResponse(List.of(), null, List.of());
        }

        // 2) 선택 루트 결정 (요청한 rootId가 없으면 첫 번째 루트로 대체)
        Category selected = null;
        if (selectedRootId == null) {
            selected = roots.get(0);
        } else {
            for (Category r : roots) {
                if (r.getId().equals(selectedRootId)) {
                    selected = r;
                    break;
                }
            }
            if (selected == null) {
                selected = roots.get(0);
            }
        }

        // 3) 선택 루트의 직계 자식 조회
        List<Category> children = categoryRepository.findDirectChildren(selected.getId(),
                activeOnly);

        // 4) 루트들의 childCount 뱃지 계산 (쿼리 1번)
        //    countChildrenByParents 결과 형식: Object[] { parentId(Long), childCount(Number) }
        List<Long> parentIds = new ArrayList<Long>(roots.size());
        for (Category r : roots) {
            parentIds.add(r.getId());
        }

        List<Object[]> countsRaw = categoryRepository.countChildrenByParents(parentIds, activeOnly);
        Map<Long, Integer> countMap = new HashMap<Long, Integer>(
                Math.max(16, roots.size() * 2)
        );
        for (Object[] row : countsRaw) {
            Long parentId = (Long) row[0];
            Number cnt = (Number) row[1];
            countMap.put(parentId, cnt == null ? 0 : cnt.intValue());
        }

        // 5) DTO 매핑 (루트들, 선택 루트, 선택 루트의 직계)
        List<MegaMenuResponse.Node> rootsDto = new ArrayList<MegaMenuResponse.Node>(roots.size());
        for (Category r : roots) {
            Integer childCount = countMap.get(r.getId());
            rootsDto.add(new MegaMenuResponse.Node(
                    r.getId(),
                    r.getName(),
                    r.getSlug(),
                    r.getImageUrl(),
                    childCount == null ? 0 : childCount
            ));
        }

        MegaMenuResponse.SelectedRoot selectedDto = new MegaMenuResponse.SelectedRoot(
                selected.getId(),
                selected.getName(),
                selected.getSlug(),
                selected.getImageUrl()
        );

        List<MegaMenuResponse.Node> childrenDto = new ArrayList<MegaMenuResponse.Node>(
                children.size());
        for (Category c : children) {
            childrenDto.add(new MegaMenuResponse.Node(
                    c.getId(),
                    c.getName(),
                    c.getSlug(),
                    c.getImageUrl(),
                    null // 자식 뱃지는 메가 메뉴 2뎁스에서는 보통 표시 안 함
            ));
        }

        return new MegaMenuResponse(rootsDto, selectedDto, childrenDto);
    }

    @Transactional(readOnly = true)
    public CategoryDetailResponse getDetail(Long id, boolean activeOnly, Set<String> include) {
        // 0) 대상 카테고리 로딩
        Category category = categoryRepository.findOneForDetail(id, activeOnly)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND, id));

        // 1) 코어 노드
        CategoryDetailResponse.Node categoryNode = new CategoryDetailResponse.Node(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getPath(),
                category.getDepth(),
                category.getImageUrl()
        );

        // 2) 브레드크럼 (항상 제공 권장)
        List<Category> ancestorEntities = categoryRepository.findBreadcrumbAncestors(id,
                activeOnly);
        List<CategoryDetailResponse.BreadcrumbNode> breadcrumb =
                new ArrayList<>(ancestorEntities.size());
        for (Category a : ancestorEntities) {
            breadcrumb.add(new CategoryDetailResponse.BreadcrumbNode(
                    a.getId(), a.getName(), a.getSlug()
            ));
        }

        // 3) children (옵션)
        List<CategoryDetailResponse.Node> children = new ArrayList<>();
        if (include.contains("children")) {
            List<Category> childEntities =
                    categoryRepository.findDirectChildren(category.getId(), activeOnly);
            children = new ArrayList<>(childEntities.size());
            for (Category c : childEntities) {
                children.add(new CategoryDetailResponse.Node(
                        c.getId(), c.getName(), c.getSlug(), c.getPath(), c.getDepth(),
                        c.getImageUrl()
                ));
            }
        }

        // 4) siblings (옵션)
        List<CategoryDetailResponse.Node> siblings = new ArrayList<>();
        if (include.contains("siblings")) {
            Long parentId = (category.getParent() == null) ? null : category.getParent().getId();
            List<Category> siblingEntities =
                    categoryRepository.findSiblings(parentId, category.getId(), activeOnly);
            siblings = new ArrayList<>(siblingEntities.size());
            for (Category s : siblingEntities) {
                siblings.add(new CategoryDetailResponse.Node(
                        s.getId(), s.getName(), s.getSlug(), s.getPath(), s.getDepth(),
                        s.getImageUrl()
                ));
            }
        }

        // 5) roots (옵션)
        List<CategoryDetailResponse.Node> roots = new ArrayList<>();
        if (include.contains("roots")) {
            List<Category> rootEntities = categoryRepository.findRoots(activeOnly);
            roots = new ArrayList<>(rootEntities.size());
            for (Category r : rootEntities) {
                roots.add(new CategoryDetailResponse.Node(
                        r.getId(), r.getName(), r.getSlug(), r.getPath(), r.getDepth(),
                        r.getImageUrl()
                ));
            }
        }

        return new CategoryDetailResponse(categoryNode, breadcrumb, children, siblings, roots);
    }


    private static String normalizeSlug(String raw) {
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

