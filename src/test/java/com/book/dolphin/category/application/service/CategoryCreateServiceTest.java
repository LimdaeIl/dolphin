package com.book.dolphin.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.dolphin.category.application.dto.request.CreateCategoryRequest;
import com.book.dolphin.category.application.dto.response.CreateCategoryResponse;
import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryClosure;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.exception.CategoryErrorCode;
import com.book.dolphin.category.domain.exception.CategoryException;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("카테고리 서비스 - 생성(create)")
@ExtendWith(MockitoExtension.class)
class CategoryCreateServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryClosureRepository categoryClosureRepository;

    // 공통
    private Long id;
    private String name;
    private String slug;
    private Category root;
    private CategoryClosure closure;

    @BeforeEach
    void beforeEach() {
        id = 1L;
        name = "남성";
        slug = "men";
        root = Category.createRoot(name, slug, null, null, null);
        // PK 강제 세팅
        ReflectionTestUtils.setField(root, "id", id);

        closure = CategoryClosure.create(root, root, 0);
    }

    @DisplayName("성공: 루트 생성 -> 남성")
    @Test
    void create_root_category_success() {
        // given
        CreateCategoryRequest request = new CreateCategoryRequest(
                null, name, slug, null, null, null
        );

        when(categoryRepository.existsByParentIsNullAndSlug(slug)).thenReturn(false);
        when(categoryRepository.existsByPath("/" + slug)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(root);
        when(categoryClosureRepository.saveAll(anyList())).thenReturn(List.of(closure));

        // when
        CreateCategoryResponse response = categoryService.create(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.parentId()).isNull();
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.slug()).isEqualTo("men");
        assertThat(response.path()).isEqualTo("/men");
        assertThat(response.depth()).isEqualTo(0);
        assertThat(response.sortOrder()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(root.getStatus());

        verify(categoryRepository).existsByParentIsNullAndSlug(slug);
        verify(categoryRepository).existsByPath("/" + slug);
        verify(categoryRepository).save(any(Category.class));

        verify(categoryClosureRepository).saveAll(anyList());
        verify(categoryClosureRepository, never()).save(any(CategoryClosure.class));
        verify(categoryRepository, never()).findById(any());
    }

    @DisplayName("성공: 직계 생성 -> 남성/상의")
    @Test
    void create_direct_category_success() {
        // given
        long parentId = 1L;
        Category root = Category.createRoot("남성", "men", 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(root, "id", parentId); // /men

        String childName = "상의";
        String childSlug = "top";
        Category child = Category.createChild(childName, childSlug, root, 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(child, "id", 2L); // /men/top

        // 요청 DTO (path 없음!)
        CreateCategoryRequest request = new CreateCategoryRequest(
                parentId,          // parentId
                childName,         // name
                childSlug,         // slug
                0,                 // sortOrder
                CategoryStatus.ACTIVE, // optional
                null               // imageUrl
        );

        // 선제 검증 스텁
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(root));
        when(categoryRepository.existsByParentIdAndSlug(parentId, childSlug)).thenReturn(false);
        when(categoryRepository.existsByPath("/men/top")).thenReturn(false);

        // 저장 스텁
        when(categoryRepository.save(any(Category.class))).thenReturn(child);

        // 클로저: 부모의 조상 목록 = [parent self(0)]
        CategoryClosure parentSelf = CategoryClosure.create(root, root, 0);
        when(categoryClosureRepository.findAllAncestorsOf(parentId)).thenReturn(List.of(parentSelf));
        when(categoryClosureRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0)); // 저장된 목록 그대로 반환

        // when
        CreateCategoryResponse res = categoryService.create(request);

        // then
        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(2L);
        assertThat(res.parentId()).isEqualTo(1L);
        assertThat(res.name()).isEqualTo("상의");
        assertThat(res.slug()).isEqualTo("top");     // slug = "top"
        assertThat(res.path()).isEqualTo("/men/top"); // path = "/men/top"
        assertThat(res.depth()).isEqualTo(1);

        verify(categoryRepository).findById(parentId);
        verify(categoryRepository).existsByParentIdAndSlug(parentId, childSlug);
        verify(categoryRepository).existsByPath("/men/top");
        verify(categoryRepository).save(any(Category.class));
        verify(categoryClosureRepository).findAllAncestorsOf(parentId);
        verify(categoryClosureRepository).saveAll(anyList());
    }

    @DisplayName("성공: 직계의 자손(손자) 생성 -> 남성/상의/셔츠")
    @Test
    void create_grandchild_category_success() {
        // given
        long rootId = 1L;
        long childId = 2L;
        long grandId = 3L;

        Category root = Category.createRoot("남성", "men", 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(root, "id", rootId);

        Category child = Category.createChild("상의", "top", root, 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(child, "id", childId);

        String grandName = "셔츠";
        String grandSlug = "shirts";
        Category grandchild = Category.createChild(grandName, grandSlug, child, 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(grandchild, "id", grandId); // /men/top/shirts

        CreateCategoryRequest request = new CreateCategoryRequest(
                childId,
                grandName,
                grandSlug,
                0,
                CategoryStatus.ACTIVE,
                null
        );

        when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));
        when(categoryRepository.existsByParentIdAndSlug(childId, grandSlug)).thenReturn(false);
        when(categoryRepository.existsByPath("/men/top/shirts")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(grandchild);

        CategoryClosure childSelf = CategoryClosure.create(child, child, 0);
        CategoryClosure rootToChild = CategoryClosure.create(root, child, 1);
        when(categoryClosureRepository.findAllAncestorsOf(childId))
                .thenReturn(List.of(childSelf, rootToChild));

        when(categoryClosureRepository.saveAll(anyList()))
                .thenReturn(List.of());

        // when
        CreateCategoryResponse res = categoryService.create(request);

        // then
        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(grandId);
        assertThat(res.parentId()).isEqualTo(childId);
        assertThat(res.name()).isEqualTo(grandName);
        assertThat(res.slug()).isEqualTo(grandSlug);
        assertThat(res.path()).isEqualTo("/men/top/shirts");
        assertThat(res.depth()).isEqualTo(2); // root(0) -> child(1) -> grand(2)

        // 호출 검증
        verify(categoryRepository).findById(childId);
        verify(categoryRepository).existsByParentIdAndSlug(childId, grandSlug);
        verify(categoryRepository).existsByPath("/men/top/shirts");
        verify(categoryRepository).save(any(Category.class));
        verify(categoryClosureRepository).findAllAncestorsOf(childId);

        // saveAll 인자 캡처해서 조상 링크가 두 개(root, child) 들어가는지 확인
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CategoryClosure>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryClosureRepository).saveAll(captor.capture());
        List<CategoryClosure> links = captor.getValue();

        // self(0)은 서비스가 별도로 추가, parentAncestors 2개 → grandchild에 대해 2개의 조상 링크가 추가되어 총 3개가 saveAll로 갈 수도 있음
        // (서비스 구현에 따라 self는 saveAll과 함께 들어갈 수도 있고, save(...)로 따로 넣을 수도 있으니
        //  최소한 조상 2개의 링크가 포함되는지만 보자)
        assertThat(links.stream()
                .filter(c -> c.getDescendant().getId().equals(grandId))
                .count()).isGreaterThanOrEqualTo(2);

        // 조상 id와 depth 검증: child(1), root(2)
        boolean hasChildDepth1 = links.stream().anyMatch(c ->
                c.getAncestor().getId().equals(childId)
                        && c.getDescendant().getId().equals(grandId)
                        && c.getDepth() == 1
        );
        boolean hasRootDepth2 = links.stream().anyMatch(c ->
                c.getAncestor().getId().equals(rootId)
                        && c.getDescendant().getId().equals(grandId)
                        && c.getDepth() == 2
        );
        assertThat(hasChildDepth1).isTrue();
        assertThat(hasRootDepth2).isTrue();
    }

    @DisplayName("실패: 직계 생성 시 부모가 없음")
    @Test
    void create_direct_category_fail_parent_not_found() {
        // given
        long missingParentId = 999L;
        CreateCategoryRequest request = new CreateCategoryRequest(
                missingParentId,
                "상의",
                "top",
                0,
                null,
                null
        );

        // 부모 조회 결과 없음
        when(categoryRepository.findById(missingParentId)).thenReturn(Optional.empty());

        // when
        CategoryException ex = assertThrows(CategoryException.class,
                () -> categoryService.create(request));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND);

        // 부모 확인만 하고, 그 이후 로직(중복 체크/저장/클로저 저장)은 전혀 호출되지 않아야 함
        verify(categoryRepository).findById(missingParentId);
        verify(categoryRepository, never()).existsByParentIdAndSlug(any(), any());
        verify(categoryRepository, never()).existsByPath(any());
        verify(categoryRepository, never()).save(any());
        verify(categoryClosureRepository, never()).saveAll(any());
    }

    @DisplayName("성공: parent.depth=5 -> child.depth=6 생성 허용")
    @Test
    void create_child_at_max_depth_success() {
        // given
        Category parent = Category.createRoot("레벨5", "lv5", 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(parent, "id", 10L);
        ReflectionTestUtils.setField(parent, "depth", 5);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsByParentIdAndSlug(10L, "leaf")).thenReturn(false);
        when(categoryRepository.existsByPath("/lv5/leaf")).thenReturn(false);

        Category child = Category.createChild("리프", "leaf", parent, 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(child, "id", 11L);

        // when & then
        when(categoryRepository.save(any(Category.class))).thenReturn(child);
        when(categoryClosureRepository.findAllAncestorsOf(10L))
                .thenReturn(List.of(CategoryClosure.create(parent, parent, 0)));
        when(categoryClosureRepository.saveAll(anyList())).thenReturn(List.of());

        CreateCategoryResponse response = categoryService.create(
                new CreateCategoryRequest(10L, "리프", "leaf", 0, null, null));
        assertThat(response.depth()).isEqualTo(6);
    }

    @DisplayName("실패: parent.depth=6 -> child.depth=7 생성 불가")
    @Test
    void create_child_exceeds_max_depth_fail() {
        // given
        Category parent = Category.createRoot("레벨6", "lv6", 0, CategoryStatus.READY, null);
        ReflectionTestUtils.setField(parent, "id", 20L);
        ReflectionTestUtils.setField(parent, "depth", 6);

        when(categoryRepository.findById(20L)).thenReturn(Optional.of(parent));

        // when & then
        CategoryException exception = assertThrows(CategoryException.class, () ->
                categoryService.create(new CreateCategoryRequest(20L, "X", "x", 0, null, null))
        );
        assertThat(exception.getErrorCode()).isEqualTo(CategoryErrorCode.MAX_DEPTH_EXCEEDED);
    }
}
