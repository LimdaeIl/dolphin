package com.book.dolphin.category.application.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.dolphin.category.application.dto.response.MegaMenuResponse;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("카테고리 서비스 - 메가메뉴 조회")
@ExtendWith(MockitoExtension.class)
class CategoryGetMegaMenuServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    // getMegaMenu 에서는 사용하지 않지만, 생성자 주입을 위해 목 정의
    @Mock
    private CategoryClosureRepository categoryClosureRepository;

    private Category men;
    private Category women;
    private Category menTop;

    @BeforeEach
    void setUp() {
        // 루트: 남성(정렬 0), 여성(정렬 1)
        men = Category.createRoot("남성", "men", 0, CategoryStatus.ACTIVE, "men.jpg");
        ReflectionTestUtils.setField(men, "id", 1L);

        women = Category.createRoot("여성", "women", 1, CategoryStatus.ACTIVE, "women.jpg");
        ReflectionTestUtils.setField(women, "id", 2L);

        // 남성 직계
        menTop = Category.createChild("상의", "top", men, 0, CategoryStatus.ACTIVE, "top.jpg");
        ReflectionTestUtils.setField(menTop, "id", 10L);
    }

    @Test
    @DisplayName("성공: selectedRootId=null → 첫 루트 선택, 직계 자식과 루트별 childCount 포함")
    void getMegaMenu_default_select_first_root() {
        boolean activeOnly = true;

        // 1) 루트 목록
        when(categoryRepository.findRoots(activeOnly))
                .thenReturn(List.of(men, women));

        // 2) 선택된 루트(남성)의 직계 자식
        when(categoryRepository.findDirectChildren(1L, activeOnly))
                .thenReturn(List.of(menTop));

        // 3) 루트 childCount 집계
        // countChildrenByParents 리턴은 [parentId(Long), count(Number)] 배열 리스트
        Object[] menRow = new Object[]{1L, 2};    // 남성의 직계 2개라고 가정
        Object[] womenRow = new Object[]{2L, 0};  // 여성 0개
        when(categoryRepository.countChildrenByParents(List.of(1L, 2L), activeOnly))
                .thenReturn(List.of(menRow, womenRow));

        MegaMenuResponse res = categoryService.getMegaMenu(null, activeOnly);

        // then
        assertThat(res).isNotNull();
        assertThat(res.roots()).hasSize(2);
        assertThat(res.selectedRoot()).isNotNull();
        assertThat(res.selectedRoot().id()).isEqualTo(1L);
        assertThat(res.selectedRoot().name()).isEqualTo("남성");
        assertThat(res.childrenOfSelectedRoot()).hasSize(1);
        assertThat(res.childrenOfSelectedRoot().get(0).name()).isEqualTo("상의");

        // 루트별 childCount 배지 검증
        Integer menCount = res.roots().stream()
                .filter(n -> n.id().equals(1L))
                .findFirst()
                .map(MegaMenuResponse.Node::childCount)
                .orElse(null);
        Integer womenCount = res.roots().stream()
                .filter(n -> n.id().equals(2L))
                .findFirst()
                .map(MegaMenuResponse.Node::childCount)
                .orElse(null);

        assertThat(menCount).isEqualTo(2);
        assertThat(womenCount).isEqualTo(0);

        verify(categoryRepository).findRoots(activeOnly);
        verify(categoryRepository).findDirectChildren(1L, activeOnly);
        verify(categoryRepository).countChildrenByParents(List.of(1L, 2L), activeOnly);
    }

    @Test
    @DisplayName("성공: selectedRootId가 존재하지 않으면 첫 루트로 폴백")
    void getMegaMenu_selected_root_not_found_fallback_first() {
        boolean activeOnly = true;

        when(categoryRepository.findRoots(activeOnly))
                .thenReturn(List.of(men, women));

        // 선택 실패 → 첫 루트(남성)
        when(categoryRepository.findDirectChildren(1L, activeOnly))
                .thenReturn(List.of(menTop));

        Object[] menRow = new Object[]{1L, 1};
        Object[] womenRow = new Object[]{2L, 0};
        when(categoryRepository.countChildrenByParents(List.of(1L, 2L), activeOnly))
                .thenReturn(List.of(menRow, womenRow));

        MegaMenuResponse res = categoryService.getMegaMenu(999L, activeOnly);

        assertThat(res.selectedRoot().id()).isEqualTo(1L);
        assertThat(res.childrenOfSelectedRoot()).extracting(MegaMenuResponse.Node::name)
                .containsExactly("상의");
    }

    @Test
    @DisplayName("성공: 루트가 비어있으면 빈 응답")
    void getMegaMenu_empty_roots() {
        boolean activeOnly = true;

        when(categoryRepository.findRoots(activeOnly)).thenReturn(List.of());

        MegaMenuResponse res = categoryService.getMegaMenu(null, activeOnly);

        assertThat(res).isNotNull();
        assertThat(res.roots()).isEmpty();
        assertThat(res.selectedRoot()).isNull();
        assertThat(res.childrenOfSelectedRoot()).isEmpty();

        verify(categoryRepository).findRoots(activeOnly);
    }
}
