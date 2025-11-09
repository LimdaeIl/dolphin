package com.book.dolphin.category.domain.repository;

import com.book.dolphin.category.domain.entity.Category;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByParentIdAndSlug(Long parentId, String slug);

    boolean existsByParentIsNullAndSlug(String slug);

    boolean existsByPath(String path);

    // 루트들 (고객 노출: ACTIVE만; 관리화면은 activeOnly=false로 분기)
    @Query("""
                select c from Category c
                where c.parent is null
                  and (:activeOnly = false or c.status = com.book.dolphin.category.domain.entity.CategoryStatus.ACTIVE)
                order by c.sortOrder asc, c.name asc
            """)
    List<Category> findRoots(boolean activeOnly);

    // 특정 부모의 직계
    @Query("""
                select c from Category c
                where c.parent.id = :parentId
                  and (:activeOnly = false or c.status = com.book.dolphin.category.domain.entity.CategoryStatus.ACTIVE)
                order by c.sortOrder asc, c.name asc
            """)
    List<Category> findDirectChildren(Long parentId, boolean activeOnly);

    // 직계 자식 수(뱃지용)
    @Query("""
                select c.parent.id as parentId, count(c.id) as cnt
                from Category c
                where c.parent.id in :parentIds
                  and (:activeOnly = false or c.status = com.book.dolphin.category.domain.entity.CategoryStatus.ACTIVE)
                group by c.parent.id
            """)
    List<Object[]> countChildrenByParents(Collection<Long> parentIds, boolean activeOnly);

    // 형제들(현재 제외). 부모가 없으면 “다른 루트들”
    @Query("""
                select c from Category c
                where (
                         (:parentId is null and c.parent is null)
                      or (c.parent.id = :parentId)
                      )
                  and c.id <> :selfId
                  and (:activeOnly = false or c.status = com.book.dolphin.category.domain.entity.CategoryStatus.ACTIVE)
                order by c.sortOrder asc, c.name asc
            """)
    List<Category> findSiblings(Long parentId, Long selfId, boolean activeOnly);

    // 브레드크럼: 조상들(루트→현재) 정렬
    @Query("""
                select cc.ancestor
                from CategoryClosure cc
                where cc.descendant.id = :descId
                  and cc.depth > 0
                  and (:activeOnly = false or cc.ancestor.status = com.book.dolphin.category.domain.entity.CategoryStatus.ACTIVE)
                order by cc.depth desc
            """)
    List<Category> findBreadcrumbAncestors(Long descId, boolean activeOnly);

    @Query("""
                select c from Category c
                where c.id = :id
                  and (:activeOnly = false or c.status = com.book.dolphin.category.domain.entity.CategoryStatus.ACTIVE)
            """)
    Optional<Category> findOneForDetail(Long id, boolean activeOnly);

    @Query("""
        select cc.descendant
        from CategoryClosure cc
        where cc.ancestor.id = :ancestorId
        order by cc.depth asc
    """)
    List<Category> findSubtreeDescendants(Long ancestorId);

    @Query("""
        select c
        from Category c
        where c.path = :rootPath
           or c.path like concat(:rootPath, '/%')
        order by c.depth asc
    """)
    List<Category> findSubtreeDescendantsByPath(String rootPath);

    @Query("""
        select max(cc.depth)
        from CategoryClosure cc
        where cc.ancestor.id = :ancestorId
    """)
    Integer findMaxDepthOffsetInSubtree(Long ancestorId); // null → leaf


    @Query("""
        select cc.ancestor
        from CategoryClosure cc
        where cc.descendant.id = :descId
        order by cc.depth asc
    """)
    List<Category> findAllAncestors(Long descId);

    @Modifying
    @Query("""
        delete from CategoryClosure cc
        where cc.descendant.id in :subtreeIds
          and cc.ancestor.id not in :subtreeIds
    """)
    int deleteLinksOutsideSubtree(List<Long> subtreeIds);

    @Query("""
            select count(c) > 0
            from Category c
            where c.path = :newPath
            and (c.path <> :oldPath)
            """)
    boolean existsByPathOtherThan(String newPath, String oldPath);

    @Modifying
    @Query("delete from Category c where c.id in :ids")
    int deleteAllByIdsIn(List<Long> ids);
}