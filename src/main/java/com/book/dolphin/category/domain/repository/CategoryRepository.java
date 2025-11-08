package com.book.dolphin.category.domain.repository;

import com.book.dolphin.category.domain.entity.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByParentIdAndSlug(Long parentId, String slug);

    boolean existsByParentIsNullAndSlug(String slug);

    boolean existsByPath(String path);
}