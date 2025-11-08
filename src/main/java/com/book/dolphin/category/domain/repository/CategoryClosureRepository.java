package com.book.dolphin.category.domain.repository;

import com.book.dolphin.category.domain.entity.CategoryClosure;
import com.book.dolphin.category.domain.entity.CategoryClosureId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryClosureRepository extends
        JpaRepository<CategoryClosure, CategoryClosureId> {

    @Query(value = """
            select cc.*
            from category_closures as cc
            where cc.descendant_id = :descendantId
            """, nativeQuery = true)
    List<CategoryClosure> findAllAncestorsOf(@Param("descendantId") Long descendantId);
}
