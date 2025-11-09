package com.book.dolphin.category.domain.repository;

import com.book.dolphin.category.domain.entity.CategoryClosure;
import com.book.dolphin.category.domain.entity.CategoryClosureId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 서브트리 외부 조상 링크만 삭제 (self/내부-내부 링크는 유지)
    // 즉: descendant ∈ 서브트리 AND ancestor ∉ 서브트리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from CategoryClosure cc
        where cc.descendant.id in :subtreeIds
          and cc.ancestor.id not in :subtreeIds
    """)
    int deleteLinksOutsideSubtree(List<Long> subtreeIds);

    @Query("""
        select cc.ancestor, cc.depth
        from CategoryClosure cc
        where cc.descendant.id = :descId
        order by cc.depth asc
    """)
    List<Object[]> findAncestorsWithDepth(Long descId);
}
