package com.book.dolphin.product.domain.entity;


import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.product.domain.exception.ProductErrorCode;
import com.book.dolphin.product.domain.exception.ProductException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_status", columnList = "product_status")
        }
)
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    @Comment("상품명")
    private String name;

    @Lob
    @Column(name = "content", columnDefinition = "text", nullable = false)
    @Comment("상세 본문(마크다운/HTML 허용)")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status", nullable = false, length = 30)
    private ProductStatus productStatus = ProductStatus.DRAFT;

    /**
     * 배리언트
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductVariant> variants = new ArrayList<>();

    /**
     * 카테고리 연결(정렬/대표 여부를 포함)
     */

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @BatchSize(size = 100)
    private final List<ProductCategory> categories = new ArrayList<>();

    /**
     * 이미지(대표/본문용)
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @BatchSize(size = 100)
    private final List<ProductMedia> mediaList = new ArrayList<>();

    /**
     * 가격(정가/할인가 + 기간/상태)
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductPrice> prices = new ArrayList<>();

    @Builder
    private Product(String name, String content) {
        this.name = Objects.requireNonNull(name);
        this.content = Objects.requireNonNullElse(content, "");
    }

    // 카테고리 연결
    public void addCategory(Category category, boolean primary, int sortKey) {
        // 대표 카테고리는 한 개만 허용
        if (primary && categories.stream().anyMatch(ProductCategory::isPrimary)) {
            throw new ProductException(ProductErrorCode.REPRESENTATIVE_CATEGORY_ONLY_ONE);
        }

        ProductCategory pc = new ProductCategory(this, category, primary, sortKey);
        categories.add(pc);
    }

    public void removeCategory(Long categoryId) {
        categories.removeIf(pc -> pc.getCategory().getId().equals(categoryId));
    }

    // 미디어(이미지)
    public void addRepresentativeImage(String url, String altText, int sortKey) {
        long repCount = mediaList.stream()
                .filter(m -> m.getType() == MediaType.REPRESENTATIVE)
                .count();
        if (repCount >= 8) { // 상한 8장 (운영 정책에 맞게 5~8 중 택1)
            throw new ProductException(ProductErrorCode.REPRESENTATIVE_IMAGE_MAX_OVER, repCount);
        }
        mediaList.add(ProductMedia.representative(this, url, altText, sortKey));
        validateRepresentativeImageLowerBound();
    }

    public void addContentImage(String url, String altText, int sortKey) {
        mediaList.add(ProductMedia.content(this, url, altText, sortKey));
    }

    public void reorderMedia(Long mediaId, int newSortKey) {
        if (mediaId == null) {
            throw new ProductException(ProductErrorCode.MEDIA_ID_NULL);
        }
        mediaList.stream()
                .filter(m -> m.getId().equals(mediaId))
                .findFirst()
                .ifPresent(m -> m.changeSortKey(newSortKey));
    }

    private void validateRepresentativeImageLowerBound() {
        long repCount = mediaList.stream()
                .filter(m -> m.getType() == MediaType.REPRESENTATIVE)
                .count();
        if (repCount < 5) {
            // 필요 시 경고/검증 로직(저장 시점 Validator로 강제할 수도 있음)
        }
    }

    // 가격(정가/할인)
    public void setListPrice(Money amount) {
        // type=LIST인 기존 active 비활성화(단일 활성 정책)
        prices.stream().filter(p -> p.getType() == PriceType.LIST && p.isActive())
                .forEach(ProductPrice::deactivate);
        prices.add(ProductPrice.listPrice(this, amount));
    }

    public void setSalePrice(Money amount, DateRange period) {
        // 기간 겹침 검증(간단 버전)
        boolean overlap = prices.stream()
                .filter(p -> p.getType() == PriceType.SALE && p.isActive())
                .anyMatch(p -> p.overlaps(period));
        if (overlap) {
            throw new ProductException(ProductErrorCode.DUPLICATE_SALE_PERIOD);
        }
        prices.add(ProductPrice.salePrice(this, amount, period));
    }

    public Optional<Money> currentPrice() {
        // 우선순위: 유효한 SALE > LIST
        return prices.stream()
                .filter(ProductPrice::isEffectiveNow)
                .sorted(Comparator.comparing(ProductPrice::getType)) // SALE가 LIST보다 앞서도록 enum 순서 설계
                .map(ProductPrice::getAmount)
                .findFirst();
    }

    // 상태/내용 변경
    public void publish() {
        boolean invalid = categories.stream()
                .map(ProductCategory::getCategory)
                .anyMatch(cat -> cat.getStatus() != CategoryStatus.ACTIVE);
        if (invalid) {
            throw new ProductException(ProductErrorCode.CANNOT_PUBLISH_WITH_INACTIVE_CATEGORY);
        }
        this.productStatus = ProductStatus.PUBLISHED;
    }

    public void archive() {
        this.productStatus = ProductStatus.ARCHIVED;
    }

    public void changeContent(String content) {
        this.content = Objects.requireNonNull(content);
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name);
    }
}
