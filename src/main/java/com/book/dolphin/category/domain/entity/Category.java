package com.book.dolphin.category.domain.entity;

import com.book.dolphin.category.domain.exception.CategoryErrorCode;
import com.book.dolphin.category.domain.exception.CategoryException;
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

    @Column(name = "image_url", length = 1024)
    private String imageUrl = null;

    @Builder(access = AccessLevel.PRIVATE)
    private Category(
            String name,
            String slug,
            Category parent,
            String path,
            int sortOrder,
            CategoryStatus status,
            int depth,
            String imageUrl
    ) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
        this.path = path;
        this.sortOrder = sortOrder;
        this.status = status;
        this.depth = depth;
        this.imageUrl = imageUrl;
    }

    public static Category createRoot(
            String name,
            String slug,
            Integer sortOrderDefault,
            CategoryStatus statusDefault,
            String imageUrl
    ) {
        return Category.builder()
                .name(name)
                .slug(slug)
                .parent(null)
                .path("/" + slug)
                .sortOrder(sortOrderDefault == null ? 0 : sortOrderDefault)
                .status(statusDefault == null ? CategoryStatus.READY : statusDefault)
                .depth(0)
                .imageUrl(imageUrl)
                .build();
    }

    public static Category createChild(
            String name,
            String slug,
            Category parent,
            Integer sortOrderDefault,
            CategoryStatus statusDefault,
            String imageUrl
    ) {
        return Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .path(parent.getPath() + "/" + slug)
                .sortOrder(sortOrderDefault == null ? 0 : sortOrderDefault)
                .status(statusDefault == null ? CategoryStatus.READY : statusDefault)
                .depth(parent.getDepth() + 1)
                .imageUrl(imageUrl)
                .build();
    }

    public void changeName(String newName) {
        if (newName == null) {
            return;
        }
        String value = newName.trim();
        if (value.isEmpty()) {
            throw new CategoryException(CategoryErrorCode.NAME_NOT_NULL);
        }
        this.name = value;
    }

    public void changeImageUrl(String newImageUrl) {
        if (newImageUrl == null) {
            return;
        }
        String value = newImageUrl.trim();
        this.imageUrl = value.isEmpty() ? null : value; // 빈 문자열이면 '삭제'로 간주
    }

    public void changeSortOrder(Integer newSortOrder) {
        if (newSortOrder == null) {
            return;
        }
        if (newSortOrder < 0) {
            throw new CategoryException(CategoryErrorCode.SORT_ORDER_GREATER_OR_EQUAL_ZERO);
        }
        this.sortOrder = newSortOrder;
    }

    public void changeStatus(CategoryStatus newStatus) {
        if (newStatus == null) {
            return;
        }
        this.status = newStatus;
    }

    /**
     * INTERNAL: move() 과정에서만 사용. null 허용(루트 승격).
     * JPA dirty-checking을 위해 단순 대입만 하면 충분.
     */
    public void setParentUnsafe(Category newParent) {
        // 자기 자신을 부모로 설정하는 실수 방지 정도만 가드
        if (newParent == this) {
            throw new CategoryException(
                   CategoryErrorCode.INVALID_REPARENT_SELF);
        }
        this.parent = newParent; // null이면 루트
    }

    /**
     * INTERNAL: 서브트리 재배치 시 깊이 재계산에 사용.
     */
    public void setDepthUnsafe(int newDepth) {
        if (newDepth < 0) {
            throw new CategoryException(CategoryErrorCode.DEPTH_GREATER_OR_EQUAL_ZERO);
        }
        this.depth = newDepth;
    }

    /**
     * INTERNAL: 서브트리 경로 치환에 사용.
     * 서비스 레벨에서 전역 유니크(path) 검증을 끝내고 들어온 값만 대입합니다.
     */
    public void setPathUnsafe(String newPath) {
        if (newPath == null || newPath.isBlank()) {
            throw new CategoryException(CategoryErrorCode.PATH_NOT_NULL);
        }
        // 일관성: 항상 슬래시로 시작
        if (!newPath.startsWith("/")) {
            newPath = "/" + newPath;
        }
        // 컬럼 제약과 맞추기(1024)
        if (newPath.length() > 1024) {
            throw new CategoryException(CategoryErrorCode.PATH_LENGTH_MAX_OVER);
        }
        this.path = newPath;
    }
}

