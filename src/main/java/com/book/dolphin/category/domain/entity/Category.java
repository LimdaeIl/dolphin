package com.book.dolphin.category.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_category_parent", columnList = "parent_id"),
                @Index(name = "idx_category_parent_sort", columnList = "parent_id,sort_order"),
                @Index(name = "idx_category_status", columnList = "status"),
                @Index(name = "idx_category_status_sort", columnList = "status,sort_order"),
                @Index(name = "idx_category_depth", columnList = "depth")
        },
        uniqueConstraints = {
                // 전역 slug 고유 → 제거
                // @UniqueConstraint(name = "uk_categories_slug", columnNames = {"slug"}),

                // 형제 단위 slug 고유 (부모가 같을 때만 중복 금지)
                // 주의: 루트(NULL parent) 중복은 이 제약만으로는 차단되지 않음.
                // MySQL 8 함수 기반 UNIQUE로 (COALESCE(parent_id,-1), slug) 인덱스를 별도 DDL로 추가해야 함.
                // CREATE UNIQUE INDEX uk_categories_parent_slug
                //  ON categories ((COALESCE(parent_id, -1)), slug);
                @UniqueConstraint(name = "uk_categories_parent_slug",
                        columnNames = {"parent_id", "slug"}),

                // path는 전역 고유 유지 (경로식별/SEO/리다이렉트)
                @UniqueConstraint(name = "uk_categories_path", columnNames = {"path"})

                // (선택) 이름도 형제 단위로 고유하게: parent+name
                // @UniqueConstraint(name = "uk_categories_parent_name", columnNames = {"parent_id", "name"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 사람이 읽는 URL 조각(형제 단위 고유)
     */
    @Column(name = "slug", nullable = false, updatable = false, length = 255)
    private String slug;

    /**
     * 인접 리스트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_category_parent"))
    private Category parent;

    /**
     * 머터리얼라이즈드 경로 (전역 고유)
     */
    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CategoryStatus status = CategoryStatus.READY;

    @Column(nullable = false)
    private int depth = 0;

    @Builder(access = AccessLevel.PRIVATE)
    private Category(
            String name,
            String slug,
            Category parent,
            String path,
            int sortOrder,
            CategoryStatus status,
            int depth) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
        this.path = path;
        this.sortOrder = sortOrder;
        this.status = status;
        this.depth = depth;
    }

    public static Category createRoot(
            String name,
            String slug,
            Integer sortOrderDefault,
            CategoryStatus statusDefault
    ) {
        return Category.builder()
                .name(name)
                .slug(slug)
                .parent(null)
                .path("/" + slug)
                .sortOrder(sortOrderDefault == null ? 0 : sortOrderDefault)
                .status(statusDefault == null ? CategoryStatus.READY : statusDefault)
                .depth(0)
                .build();
    }

    public static Category createChild(
            String name,
            String slug,
            Category parent,
            String path,
            Integer sortOrderDefault,
            CategoryStatus statusDefault
    ) {
        return Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .path(path)
                .sortOrder(sortOrderDefault == null ? 0 : sortOrderDefault)
                .status(statusDefault == null ? CategoryStatus.READY : statusDefault)
                .depth(parent.getDepth() + 1)
                .build();
    }
}

