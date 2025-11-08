package com.book.dolphin.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import support.CategoryTestSeeder;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(false)
class CategoryDummyDataTest {

    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    CategoryClosureRepository closureRepository;

    @DisplayName("더미 트리 데이터 심기: 남성/여성 2루트, 각 2뎁스 자식")
    @Test
    void seed_dummy_category_tree() {
        CategoryTestSeeder seeder = new CategoryTestSeeder(categoryRepository,
                closureRepository);

        // 루트
        Category men   = seeder.seedRoot("남성", "men", 0, CategoryStatus.ACTIVE, null);
        Category women = seeder.seedRoot("여성", "women", 1, CategoryStatus.ACTIVE, null);

        // 남성 하위
        Category menTop    = seeder.seedChild(men, "상의", "top", 0, CategoryStatus.ACTIVE, null);
        Category menBottom = seeder.seedChild(men, "하의", "bottom", 1, CategoryStatus.ACTIVE, null);
        seeder.seedChild(menTop, "셔츠", "shirts", 0, CategoryStatus.ACTIVE, null);
        seeder.seedChild(menBottom, "청바지", "jeans", 0, CategoryStatus.ACTIVE, null);

        // 여성 하위
        Category womenTop   = seeder.seedChild(women, "상의", "top", 0, CategoryStatus.ACTIVE, null);
        Category womenOuter = seeder.seedChild(women, "아우터", "outer", 1, CategoryStatus.ACTIVE, null);
        seeder.seedChild(womenTop, "셔츠", "shirts", 0, CategoryStatus.ACTIVE, null);
        seeder.seedChild(womenOuter, "코트", "coat", 0, CategoryStatus.ACTIVE, null);

        // 간단 검증
        assertThat(categoryRepository.count()).isGreaterThanOrEqualTo(10);
        // 브레드크럼/직계 자식 조회 쿼리들 인덱스 타는지 확인하려면, 서비스/리포지토리 레벨 테스트에서 더 검증
    }
}
