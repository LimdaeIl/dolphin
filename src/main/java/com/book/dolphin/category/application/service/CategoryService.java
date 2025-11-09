package com.book.dolphin.category.application.service;

import static com.book.dolphin.category.domain.entity.Category.createChild;
import static com.book.dolphin.category.domain.entity.Category.createRoot;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.ALREADY_PATH;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.DUPLICATE_SLUG_BY_PARENT;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.DUPLICATE_SLUG_BY_ROOT;
import static com.book.dolphin.category.domain.exception.CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND;

import com.book.dolphin.category.application.dto.request.CreateCategoryRequest;
import com.book.dolphin.category.application.dto.request.UpdateCategoryRequest;
import com.book.dolphin.category.application.dto.response.BreadcrumbNode;
import com.book.dolphin.category.application.dto.response.CategoryDetailResponse;
import com.book.dolphin.category.application.dto.response.CreateCategoryResponse;
import com.book.dolphin.category.application.dto.response.MegaMenuResponse;
import com.book.dolphin.category.application.dto.response.MoveCategoryResponse;
import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryClosure;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.exception.CategoryErrorCode;
import com.book.dolphin.category.domain.exception.CategoryException;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        List<Long> parentIds = new ArrayList<>(roots.size());
        for (Category r : roots) {
            parentIds.add(r.getId());
        }

        List<Object[]> countsRaw = categoryRepository.countChildrenByParents(parentIds, activeOnly);
        Map<Long, Integer> countMap = new HashMap<>(
                Math.max(16, roots.size() * 2)
        );
        for (Object[] row : countsRaw) {
            Long parentId = (Long) row[0];
            Number cnt = (Number) row[1];
            countMap.put(parentId, cnt == null ? 0 : cnt.intValue());
        }

        // 5) DTO 매핑 (루트들, 선택 루트, 선택 루트의 직계)
        List<MegaMenuResponse.Node> rootsDto = new ArrayList<>(roots.size());
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

        List<MegaMenuResponse.Node> childrenDto = new ArrayList<>(
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
        List<BreadcrumbNode> breadcrumb =
                new ArrayList<>(ancestorEntities.size());
        for (Category a : ancestorEntities) {
            breadcrumb.add(new BreadcrumbNode(
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

    @Transactional
    public void updateBasic(Long id, UpdateCategoryRequest request) {
        // 0) 대상 로드
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryException(
                        CategoryErrorCode.CATEGORY_NOT_FOUND, id));

        // 1) 부분 업데이트 (null인 항목은 건드리지 않음)
        category.changeName(request.name());
        category.changeImageUrl(request.imageUrl());
        category.changeSortOrder(request.sortOrder());
        category.changeStatus(request.status());
    }


    @Transactional
    public MoveCategoryResponse move(Long id, Long newParentId) {

        // 0) 대상/새 부모 로딩
        Category node = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND, id));

        Category newParent = null;
        if (newParentId != null) {
            newParent = categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new CategoryException(
                            CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND, newParentId));
        }

        // 1) 사이클 방지: newParent가 node 자신 또는 node의 자손이면 금지
        //    (자기 자손 밑으로 이동하면 트리가 순환됨)
        if (newParent != null) {
            // node의 서브트리(자기 자신 포함) 로딩
            List<Category> subtreeForCycle = categoryRepository.findSubtreeDescendants(id);
            for (Category d : subtreeForCycle) {
                if (d.getId().equals(newParent.getId())) {
                    throw new CategoryException(
                            CategoryErrorCode.INVALID_REPARENT_TARGET, id, newParentId);
                }
            }
        }

        // 2) 유니크 제약 사전검사
        //    - (부모, slug) 유니크: 새 부모 아래 동일 slug 형제 존재 금지(자기 자신 제외)
        //    - path(전역 유니크): newPrefix가 서브트리 외부에서 이미 존재하면 금지
        String slug = node.getSlug();

        if (newParent == null) {
            // 루트 셋에서 slug 충돌? (자기 자신이 원래 루트였던 경우는 예외 상황 고려)
            boolean slugTakenAtRoot = categoryRepository.existsByParentIsNullAndSlug(slug);
            boolean movingToRootFromNonRoot = (node.getParent() != null);
            if (slugTakenAtRoot && movingToRootFromNonRoot) {
                // 의도를 명확하게: 루트 묶음에 동일 slug 존재 -> 금지
                throw new CategoryException(CategoryErrorCode.DUPLICATE_SLUG_BY_ROOT, slug);
            }
        } else {
            // 새 부모 밑에서 동일 slug 존재? (단, 원래 부모 == 새 부모라면 자기 자신일 수 있으므로 제외 로직 필요)
            // 여기선 보수적으로: 원래 부모와 다르고, 동일 slug가 존재하면 금지
            if (!newParent.equals(node.getParent())
                    && categoryRepository.existsByParentIdAndSlug(newParent.getId(), slug)) {
                throw new CategoryException(CategoryErrorCode.DUPLICATE_SLUG_BY_PARENT,
                        newParent.getId(), slug);
            }
        }

        // 3) 최대 깊이 검증: 새 위치에서 서브트리 최심 깊이가 MAX_DEPTH를 넘는지 검사
        //    - maxOffset: 서브트리에서 node보다 가장 깊은 노드까지의 거리
        //    - deepestNewDepth = (newParentDepth + 1 + maxOffset)
        Integer maxOffset = categoryRepository.findMaxDepthOffsetInSubtree(id);
        int subtreeMaxOffset = (maxOffset == null) ? 0 : maxOffset; // leaf면 0
        int newParentDepth =
                (newParent == null) ? -1 : newParent.getDepth(); // 루트는 0이어야 하므로 base를 -1로
        int deepestNewDepth = newParentDepth + 1 + subtreeMaxOffset;
        if (deepestNewDepth > MAX_DEPTH) {
            throw new CategoryException(CategoryErrorCode.MAX_DEPTH_EXCEEDED, MAX_DEPTH);
        }

        // 4) 새 prefix 계산 (oldPrefix -> newPrefix)
        String oldPrefix = node.getPath(); // ex. "/men/top"
        String newPrefix = (newParent == null) ? ("/" + slug) : (newParent.getPath() + "/" + slug);

        // path 전역 유니크 사전검증:
        // 자기 자신(oldPrefix)와 동일하면 OK. 다르면 "서브트리 외부"와 충돌하는지 추가 확인.
        if (!oldPrefix.equals(newPrefix)
                && categoryRepository.existsByPathOtherThan(newPrefix, oldPrefix)) {
            throw new CategoryException(CategoryErrorCode.ALREADY_PATH, newPrefix);
        }

        // 5) 서브트리 로딩 (node 포함, 깊이 오름차순) - 클로저 테이블 기반
        List<Category> subtree = categoryRepository.findSubtreeDescendants(id);

        // 6) 부모/깊이/path 재계산
        //    - node.setParentUnsafe(newParent)
        //    - newDepth = baseDepth + (oldDepth - nodeOldDepth)
        //    - newPath  = newPrefix + suffix (suffix = oldPath.substring(oldPrefix.length()))
        node.setParentUnsafe(newParent); // 부모 변경 (검증은 이미 위에서 완료됨)

        int baseDepth = (newParent == null) ? 0 : newParent.getDepth() + 1;
        int nodeOldDepth = node.getDepth();

        for (Category d : subtree) {
            // depth 재계산
            int offset = d.getDepth() - nodeOldDepth;
            int newDepth = baseDepth + offset;
            d.setDepthUnsafe(newDepth);

            // path 재계산 (프리픽스 치환)
            String dp = d.getPath();
            if (!dp.startsWith(oldPrefix)) {
                // 데이터 무결성 방어: 서브트리여야 하는데 prefix가 안 맞으면 문제
                throw new CategoryException(CategoryErrorCode.SUB_TREE_INCONSISTENCY, dp);
            }
            String suffix = dp.substring(oldPrefix.length()); // "" 또는 "/shirts" 등
            String newPath = newPrefix + suffix;

            // 루프 진행 중에도 path 충돌 방어 (전역 유니크 제약으로도 잡히지만, 선제 확인)
            if (!dp.equals(newPath) && categoryRepository.existsByPath(newPath)) {
                throw new CategoryException(CategoryErrorCode.ALREADY_PATH, newPath);
            }
            d.setPathUnsafe(newPath);
        }

        // 7) Closure 재구성 (부분 갱신)
        //    전략:
        //    7-1) 서브트리 외부 조상 링크만 삭제 (내부 self/내부조상 링크는 유지 -> 내부 거리 불변)
        //    7-2) newParent의 조상 체인(ancestor, depthToNewParent)을 한 번에 조회
        //    7-3) (a) newParent -> 모든 자손, (b) ancestor -> 모든 자손 링크를 공식으로 일괄 삽입
        //        depth(newParent -> d) = 1 + offset
        //        depth(ancestor -> d)  = depth(ancestor-> newParent) + 1 + offset
        //        where offset = d.depth - (newParent.depth + 1)
        // 7-1. 외부 조상 링크 삭제
        List<Long> subtreeIds = subtree.stream().map(Category::getId).toList();
        categoryClosureRepository.deleteLinksOutsideSubtree(subtreeIds);

        // 7-2. (ancestor, depthToNewParent) 목록
        List<Object[]> ancWithDepth = (newParent == null)
                ? List.of() // 루트로 이동이면 상위 조상 없음
                : categoryClosureRepository.findAncestorsWithDepth(newParent.getId());

        // 7-3) 일괄 삽입
        List<CategoryClosure> insertLinks = new ArrayList<>();
        int newParentBaseDepth = (newParent == null) ? -1 : newParent.getDepth();

        // (a) newParent self(0) -> 모든 자손
        if (newParent != null) {
            for (Category d : subtree) {
                int offset = d.getDepth() - (newParentBaseDepth + 1); // newParent→descendant
                insertLinks.add(CategoryClosure.create(newParent, d, 1 + offset));
            }
        }

        // (b) ancestor -> 모든 자손 (ancestor와 newParent 사이 깊이를 이용)
        for (Object[] row : ancWithDepth) {
            Category ancestor = (Category) row[0];
            int depthAncToNewParent = ((Number) row[1]).intValue(); // ancestor→newParent
            for (Category d : subtree) {
                int offset = d.getDepth() - (newParentBaseDepth + 1);
                insertLinks.add(
                        CategoryClosure.create(ancestor, d, depthAncToNewParent + 1 + offset));
            }
        }

        categoryClosureRepository.saveAll(insertLinks);

        // 8) 응답 조립
        Long newParentIdOrNull = (newParent == null) ? null : newParent.getId();

        // 이동 직후의 브레드크럼 재조회 (관리자 화면이면 activeOnly = false 권장)
        List<Category> ancestorsForBreadcrumb =
                categoryRepository.findBreadcrumbAncestors(node.getId(), /*activeOnly=*/false);

        List<BreadcrumbNode> breadcrumb = ancestorsForBreadcrumb.stream()
                .map(a -> new BreadcrumbNode(a.getId(), a.getName(), a.getSlug()))
                .toList();

        return new MoveCategoryResponse(
                node.getId(),
                newParentIdOrNull,
                node.getPath(),   // 이동 후 최신 path
                node.getDepth(),  // 이동 후 최신 depth
                breadcrumb        // 루트 -> 현재까지
        );
    }

    /**
     * 서브트리 하드 삭제(자기 자신 포함).
     *
     * <p>삭제 단계</p>
     * <ol>
     *   <li><b>대상 존재 확인</b>: id가 유효한지 확인합니다. 없으면 CategoryException을 던집니다.</li>
     *   <li><b>서브트리 ID 수집</b>: 클로저 테이블을 통해 (조상=id)인 모든 descendant id를 가져옵니다.</li>
     *   <li><b>연관 데이터 검증/정리(선택)</b>:
     *       상품-카테고리 매핑 등 참조가 존재하면 정책에 따라 차단(BLOCK)하거나,
     *       대체 카테고리로 이관(REASSIGN)/매핑 제거(DETACH)를 수행합니다.
     *       해당 로직은 validateOrDetachAssociations(...)에서 구현합니다.</li>
     *   <li><b>클로저 링크 일괄 삭제</b>:
     *       서브트리 내부 링크와 외부-내부/내부-외부 링크를 모두 제거합니다.</li>
     *   <li><b>카테고리 일괄 삭제</b>:
     *       수집한 id 목록을 배치로 나눠 IN 삭제합니다(대형 서브트리 대응).</li>
     * </ol>
     *
     * <p>트랜잭션:</p>
     * 본 메서드는 트랜잭션 안에서 실행되어야 하며, 실패 시 전체 롤백됩니다.
     *
     * @param id 서브트리 루트가 될 카테고리 ID
     * @return 삭제된 카테고리 행 수(클로저 링크 삭제 수는 포함하지 않음)
     * @throws CategoryException CATEGORY_NOT_FOUND 등 정책 위반 시
     */
    @Transactional
    public int hardDeleteSubtree(Long id) {
        // 0) 대상 확인
        categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND, id));

        // 1) 서브트리 (id, depth) 내림차순 조회  ex) [(shirts,3), (top,2), (men,1)]
        List<Object[]> rows = categoryClosureRepository.findSubtreeIdWithDepthDesc(id);
        if (rows.isEmpty()) {
            return 0;
        }

        // ids만 모아 클로저 링크부터 제거 (안전)
        List<Long> allIds = rows.stream().map(r -> (Long) r[0]).toList();
        categoryClosureRepository.deleteAllTouchingIds(allIds);

        // 2) depth별로 묶어서 "자식 depth → 부모 depth" 순으로 각 depth를 개별 DELETE 실행
        Map<Integer, List<Long>> byDepthDesc = new LinkedHashMap<>();
        for (Object[] r : rows) {
            Long cId = (Long) r[0];
            Integer depth = ((Number) r[1]).intValue();
            byDepthDesc.computeIfAbsent(depth, k -> new ArrayList<>()).add(cId);
        }

        int totalDeleted = 0;
        for (Map.Entry<Integer, List<Long>> e : byDepthDesc.entrySet()) {
            List<Long> idsAtDepth = e.getValue();
            // 필요 시 대형 트리 대비 배치 분할
            for (List<Long> batch : batches(idsAtDepth, 800)) {
                totalDeleted += categoryRepository.deleteAllByIdsIn(batch);
            }
        }

        return totalDeleted;
    }

    /**
     * 대량 IN 삭제 시 DB 제한과 성능을 고려해 리스트를 적절한 크기로 나눕니다.
     *
     * @param src  원본 리스트
     * @param size 배치 크기(예: 500~1000)
     * @return 분할된 서브리스트 목록 (뷰이므로 호출자가 수정하지 않도록 주의)
     */
    private static <T> List<List<T>> batches(List<T> src, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < src.size(); i += size) {
            out.add(src.subList(i, Math.min(i + size, src.size())));
        }
        return out;
    }

    /**
     * 연관(상품·배너·프로모션 등) 존재 시의 처리 정책을 캡슐화합니다.
     *
     * <p>예시 정책:</p>
     * <ul>
     *   <li>BLOCK: 참조 개수가 0이 아니면 예외(CategoryErrorCode.CATEGORY_IN_USE)를 던져 삭제 차단</li>
     *   <li>REASSIGN: 대체 카테고리로 일괄 이관(파라미터 필요)</li>
     *   <li>DETACH: 매핑만 제거(권장하지 않음)</li>
     * </ul>
     *
     * <p>운영 환경에 맞는 구현으로 교체하세요.</p>
     */
    private void validateOrDetachAssociations(List<Long> categoryIds) {
        // long cnt = productCategoryRepository.countByCategoryIdIn(categoryIds);
        // if (cnt > 0) throw new CategoryException(CategoryErrorCode.CATEGORY_IN_USE);
        // 또는 productCategoryRepository.bulkReassign(categoryIds, newCategoryId);
    }
}

