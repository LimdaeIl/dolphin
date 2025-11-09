package com.book.dolphin.product.domain.repository;

import com.book.dolphin.product.domain.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1) 상세: 카테고리/미디어/가격까지 한 번에 가져오기 (JPQL + fetch join)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findDetailById(@Param("id") Long id);

    // 2) 상태 전환용: 카테고리 상태만 보면 됨 (JPQL + fetch join)
    @Query("""
        select distinct p
        from Product p
          left join fetch p.categories pc
          left join fetch pc.category c
        where p.id = :id
        """)
    Optional<Product> findForStatusChange(@Param("id") Long id);

    // 3) 목록/검색/정렬/페이지 (네이티브, 최대한 단순한 서브쿼리만 사용)
    //
    // - current_price: 유효한 SALE가 우선, 없으면 LIST가
    // - 대표이미지: sort_key ASC, id ASC 우선 1장
    // - 정렬: PRICE_ASC / PRICE_DESC / RECENT(기본)
    @Query(
            value = """
        SELECT
          p.product_id                AS id,
          p.name                      AS name,
          p.product_status            AS status,
          COALESCE(
            (
              SELECT pr.amount
              FROM product_prices pr
              WHERE pr.product_id = p.product_id
                AND pr.type = 'SALE'
                AND pr.active = 1
                AND (pr.valid_from IS NULL OR pr.valid_from <= NOW())
                AND (pr.valid_until IS NULL OR pr.valid_until >= NOW())
              ORDER BY pr.valid_from DESC, pr.product_price_id DESC
              LIMIT 1
            ),
            (
              SELECT pr.amount
              FROM product_prices pr
              WHERE pr.product_id = p.product_id
                AND pr.type = 'LIST'
                AND pr.active = 1
              ORDER BY pr.product_price_id DESC
              LIMIT 1
            )
          )                           AS currentPrice,
          (
            SELECT pm.url
            FROM product_media pm
            WHERE pm.product_id = p.product_id
              AND pm.type = 'REPRESENTATIVE'
            ORDER BY pm.sort_key ASC, pm.product_media_id ASC
            LIMIT 1
          )                           AS repImageUrl
        FROM products p
        WHERE (:status IS NULL OR p.product_status = :status)
          AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
          AND (:categoryId IS NULL OR EXISTS (
                SELECT 1 FROM product_categories pc
                WHERE pc.product_id = p.product_id
                  AND pc.category_id = :categoryId
          ))
        ORDER BY
          CASE WHEN :sort = 'PRICE_ASC'  THEN currentPrice END ASC,
          CASE WHEN :sort = 'PRICE_DESC' THEN currentPrice END DESC,
          CASE WHEN :sort IS NULL OR :sort = 'RECENT' THEN p.product_id END DESC
        LIMIT :limit OFFSET :offset
        """,
            nativeQuery = true
    )
    List<ProductListRow> findListSimple(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            @Param("sort") String sort,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // (선택) 페이징 total 용 간단 count (필요하면 사용)
    @Query(
            value = """
        SELECT COUNT(*)
        FROM products p
        WHERE (:status IS NULL OR p.product_status = :status)
          AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
          AND (:categoryId IS NULL OR EXISTS (
                SELECT 1 FROM product_categories pc
                WHERE pc.product_id = p.product_id
                  AND pc.category_id = :categoryId
          ))
        """,
            nativeQuery = true
    )
    long countListSimple(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") String status
    );

    // 목록 결과용 프로젝션(인터페이스 기반)
    interface ProductListRow {
        Long getId();
        String getName();
        String getStatus();
        Long getCurrentPrice();
        String getRepImageUrl();
    }
}
