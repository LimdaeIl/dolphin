package com.book.dolphin.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.dolphin.category.application.dto.response.CategoryDetailResponse;
import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.exception.CategoryErrorCode;
import com.book.dolphin.category.domain.exception.CategoryException;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("카테고리 서비스 - 상세 조회(getDetail)")
@ExtendWith(MockitoExtension.class)
class CategoryGetDetailServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    // 생성자 주입 맞추기용 (이번 테스트에서는 직접 사용하지 않음)
    @Mock
    private CategoryClosureRepository categoryClosureRepository;

    private Category root;
    private Category child;
    private Category grand1;
    private Category grand2;

    @BeforeEach
    void setUp() {
        // 루트 /men
        root = Category.createRoot("남성", "men", 0, CategoryStatus.ACTIVE, "men.jpg");
        ReflectionTestUtils.setField(root, "id", 1L);

        // 자식 /men/top
        child = Category.createChild("상의", "top", root, 0, CategoryStatus.ACTIVE, "top.jpg");
        ReflectionTestUtils.setField(child, "id", 10L);

        // 손자들 /men/top/shirts, /men/top/knit
        grand1 = Category.createChild("셔츠", "shirts", child, 0, CategoryStatus.ACTIVE,
                "shirts.jpg");
        ReflectionTestUtils.setField(grand1, "id", 100L);

        grand2 = Category.createChild("니트", "knit", child, 1, CategoryStatus.ACTIVE, "knit.jpg");
        ReflectionTestUtils.setField(grand2, "id", 101L);
    }

    @Test
    @DisplayName("성공: 기본 상세(노드 + breadcrumb + children)")
    void get_detail_with_breadcrumb_and_children() {
        boolean activeOnly = true;
        Long targetId = 10L; // child

        // 스텁
        when(categoryRepository.findOneForDetail(eq(targetId), eq(activeOnly)))
                .thenReturn(Optional.of(child));
        when(categoryRepository.findBreadcrumbAncestors(eq(targetId), eq(activeOnly)))
                .thenReturn(List.of(root));
        when(categoryRepository.findDirectChildren(eq(targetId), eq(activeOnly)))
                .thenReturn(List.of(grand1, grand2));

        // 실행
        CategoryDetailResponse res = categoryService.getDetail(
                targetId,
                activeOnly,
                Set.of("children", "breadcrumb") // siblings/roots 제외
        );

        // 검증: 코어 노드
        assertThat(res.category().id()).isEqualTo(10L);
        assertThat(res.category().name()).isEqualTo("상의");
        assertThat(res.category().slug()).isEqualTo("top");
        assertThat(res.category().path()).isEqualTo("/men/top");
        assertThat(res.category().depth()).isEqualTo(1);
        assertThat(res.category().imageUrl()).isEqualTo("top.jpg");

        // breadcrumb (루트만 포함)
        assertThat(res.breadcrumb()).hasSize(1);
        assertThat(res.breadcrumb().get(0).id()).isEqualTo(1L);
        assertThat(res.breadcrumb().get(0).name()).isEqualTo("남성");

        // children
        assertThat(res.children()).extracting(CategoryDetailResponse.Node::name)
                .containsExactlyInAnyOrder("셔츠", "니트");

        // siblings/roots는 요청하지 않았으므로 비어있음
        assertThat(res.siblings()).isEmpty();
        assertThat(res.roots()).isEmpty();

        // 호출 검증
        verify(categoryRepository).findOneForDetail(eq(targetId), eq(activeOnly));
        verify(categoryRepository).findBreadcrumbAncestors(eq(targetId), eq(activeOnly));
        verify(categoryRepository).findDirectChildren(eq(targetId), eq(activeOnly));
        verify(categoryRepository, never()).findSiblings(any(), any(), anyBoolean());
        verify(categoryRepository, never()).findRoots(anyBoolean());
    }

    @Test
    @DisplayName("성공: siblings 포함 시 형제 목록 반환(자기 자신 제외)")
    void get_detail_with_siblings() {
        boolean activeOnly = true;
        Long targetId = 10L; // child

        Category sibling = Category.createChild("아우터", "outer", root, 2, CategoryStatus.ACTIVE,
                "outer.jpg");
        ReflectionTestUtils.setField(sibling, "id", 11L);

        // 스텁
        when(categoryRepository.findOneForDetail(eq(targetId), eq(activeOnly)))
                .thenReturn(Optional.of(child));
        when(categoryRepository.findBreadcrumbAncestors(eq(targetId), eq(activeOnly)))
                .thenReturn(List.of(root));
        // parentId=1L, excludeId=10L
        when(categoryRepository.findSiblings(eq(1L), eq(10L), eq(activeOnly)))
                .thenReturn(List.of(sibling));

        // 실행
        CategoryDetailResponse res = categoryService.getDetail(
                targetId,
                activeOnly,
                Set.of("siblings", "breadcrumb") // siblings 요청
        );

        // 검증
        assertThat(res.siblings()).hasSize(1);
        assertThat(res.siblings().get(0).id()).isEqualTo(11L);
        assertThat(res.siblings().get(0).name()).isEqualTo("아우터");

        // children/roots는 비어있어야 함
        assertThat(res.children()).isEmpty();
        assertThat(res.roots()).isEmpty();

        verify(categoryRepository).findOneForDetail(eq(targetId), eq(activeOnly));
        verify(categoryRepository).findBreadcrumbAncestors(eq(targetId), eq(activeOnly));
        verify(categoryRepository).findSiblings(eq(1L), eq(10L), eq(activeOnly));
        verify(categoryRepository, never()).findDirectChildren(anyLong(), anyBoolean());
        verify(categoryRepository, never()).findRoots(anyBoolean());
    }

    @Test
    @DisplayName("성공: roots 포함 시 전체 루트 반환")
    void get_detail_with_roots() {
        boolean activeOnly = true;
        Long targetId = 10L;

        Category women = Category.createRoot("여성", "women", 1, CategoryStatus.ACTIVE, "women.jpg");
        ReflectionTestUtils.setField(women, "id", 2L);

        // 스텁
        when(categoryRepository.findOneForDetail(eq(targetId), eq(activeOnly)))
                .thenReturn(Optional.of(child));
        when(categoryRepository.findBreadcrumbAncestors(eq(targetId), eq(activeOnly)))
                .thenReturn(List.of(root));
        when(categoryRepository.findRoots(eq(activeOnly)))
                .thenReturn(List.of(root, women));

        // 실행
        CategoryDetailResponse res = categoryService.getDetail(
                targetId,
                activeOnly,
                Set.of("roots", "breadcrumb")
        );

        // 검증
        assertThat(res.roots()).hasSize(2);
        assertThat(res.roots()).extracting(CategoryDetailResponse.Node::name)
                .containsExactlyInAnyOrder("남성", "여성");

        // children/siblings는 비어야 함
        assertThat(res.children()).isEmpty();
        assertThat(res.siblings()).isEmpty();

        verify(categoryRepository).findOneForDetail(eq(targetId), eq(activeOnly));
        verify(categoryRepository).findBreadcrumbAncestors(eq(targetId), eq(activeOnly));
        verify(categoryRepository).findRoots(eq(activeOnly));
        verify(categoryRepository, never()).findDirectChildren(anyLong(), anyBoolean());
        verify(categoryRepository, never()).findSiblings(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("실패: 대상 카테고리 없음 -> CategoryException(CATEGORY_NOT_FOUND)")
    void get_detail_not_found() {
        boolean activeOnly = true;
        Long targetId = 999L;

        when(categoryRepository.findOneForDetail(eq(targetId), eq(activeOnly)))
                .thenReturn(Optional.empty());

        CategoryException ex = assertThrows(
                CategoryException.class,
                () -> categoryService.getDetail(targetId, activeOnly, Set.of("children", "breadcrumb"))
        );

        // 에러코드와 메시지 검증
        assertThat(ex.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
        // 메시지 템플릿: "카테고리: 카테고리를 찾을 수 없습니다: %s"
        assertThat(ex.getMessage()).contains("카테고리: 카테고리를 찾을 수 없습니다: " + targetId);

        verify(categoryRepository).findOneForDetail(eq(targetId), eq(activeOnly));
        verify(categoryRepository, never()).findBreadcrumbAncestors(anyLong(), anyBoolean());
        verify(categoryRepository, never()).findDirectChildren(anyLong(), anyBoolean());
        verify(categoryRepository, never()).findSiblings(any(), any(), anyBoolean());
        verify(categoryRepository, never()).findRoots(anyBoolean());
    }

}

