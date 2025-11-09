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

    // ancestor 기준 서브트리 모든 노드의 ID (자기 자신 포함)
    @Query("""
        select cc.descendant.id
        from CategoryClosure cc
        where cc.ancestor.id = :ancestorId
        order by cc.depth asc
    """)
    List<Long> findSubtreeIds(Long ancestorId);

    @Query("""
        select cc.descendant.id
        from CategoryClosure cc
        where cc.ancestor.id = :ancestorId
        order by cc.depth desc
    """)
    List<Long> findSubtreeIdsOrderByDepthDesc(Long ancestorId);

    // 서브트리 id + depth (자식이 더 큰 depth). ancestor 기준 단일행이라 depth는 고유
    @Query("""
        select cc.descendant.id as id, cc.depth as depth
        from CategoryClosure cc
        where cc.ancestor.id = :ancestorId
        order by cc.depth desc
    """)
    List<Object[]> findSubtreeIdWithDepthDesc(Long ancestorId);

    // 서브트리와 '맞닿은' 모든 클로저 링크 제거 (내부 링크 + 외부 -> 내부 링크 + 내부 -> 외부 링크 전부)
    @Modifying
    @Query("""
        delete from CategoryClosure cc
        where cc.ancestor.id in :ids
           or cc.descendant.id in :ids
    """)
    int deleteAllTouchingIds(List<Long> ids);
}
