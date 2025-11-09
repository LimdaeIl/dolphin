package com.book.dolphin.product.application.service;

import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.exception.CategoryErrorCode;
import com.book.dolphin.category.domain.exception.CategoryException;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import com.book.dolphin.product.application.dto.request.ProductCreateRequest;
import com.book.dolphin.product.application.dto.request.ProductCreateRequest.CategoryAssign;
import com.book.dolphin.product.application.dto.response.ProductListItem;
import com.book.dolphin.product.application.dto.response.ProductListPage;
import com.book.dolphin.product.application.dto.response.ProductResponse;
import com.book.dolphin.product.domain.entity.DateRange;
import com.book.dolphin.product.domain.entity.MediaType;
import com.book.dolphin.product.domain.entity.Money;
import com.book.dolphin.product.domain.entity.Product;
import com.book.dolphin.product.domain.entity.ProductCategory;
import com.book.dolphin.product.domain.entity.ProductMedia;
import com.book.dolphin.product.domain.exception.ProductErrorCode;
import com.book.dolphin.product.domain.exception.ProductException;
import com.book.dolphin.product.domain.repository.ProductRepository;
import com.book.dolphin.product.domain.repository.ProductRepository.ProductListRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        // 1) Product 생성 (sku 제거)
        Product product = Product.builder()
                .name(request.name())
                .content(request.content())
                .build();

        // 2) 가격
        applyPrice(product, request);

        // 3) 카테고리 연결
        attachCategories(product, request.categories());

        // 4~5) 이미지
        attachMedia(product, request.representatives(), request.contents());

        // 6) 저장
        Product saved = productRepository.save(product);

        // 7) 응답
        return toResponse(saved);
    }

    /**
     * 요청으로 들어온 카테고리 목록을 검증한 뒤, 엔티티를 조회하여 상품에 연결한다. 규칙: - 대표(primary) 카테고리는 최대 1개 - 중복 categoryId 금지
     * - 생성/수정 단계(DRAFT)에서는 ACTIVE 또는 READY만 허용
     */
    private void attachCategories(Product product, List<CategoryAssign> assigns) {
        if (assigns == null || assigns.isEmpty()) {
            return;
        }

        validateCategoryAssigns(assigns);

        // 배치 조회
        List<Long> ids = assigns.stream()
                .map(CategoryAssign::categoryId)
                .toList();

        List<Category> categories = categoryRepository.findAllById(ids);

        // 누락 체크
        Set<Long> found = categories.stream().map(Category::getId).collect(Collectors.toSet());
        for (Long id : ids) {
            if (!found.contains(id)) {
                throw new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND, id);
            }
        }

        // 상태 검증 + 연결 (assign 순서 유지)
        for (CategoryAssign assign : assigns) {
            Long id = assign.categoryId();
            Category category = categories.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElseThrow(
                            () -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND, id));

            CategoryStatus status = category.getStatus();
            boolean allowed = (status == CategoryStatus.ACTIVE) || (status == CategoryStatus.READY);
            if (!allowed) {
                throw new CategoryException(CategoryErrorCode.CATEGORY_STATUS_NOT_ALLOWED, id);
            }

            product.addCategory(category, assign.primary(), assign.sortKey());
        }
    }

    /**
     * 대표 카테고리 단일성과 중복 categoryId를 검증한다.
     */
    private void validateCategoryAssigns(List<CategoryAssign> categoryAssigns) {
        int primaryCount = 0;
        Set<Long> seenCategoryIds = new HashSet<>();

        for (CategoryAssign assign : categoryAssigns) {
            Long categoryId = assign.categoryId();

            // 대표 1개 제한
            if (assign.primary()) {
                primaryCount++;
                if (primaryCount > 1) {
                    throw new ProductException(ProductErrorCode.REPRESENTATIVE_CATEGORY_ONLY_ONE);
                }
            }

            // 중복 금지
            boolean firstTimeSeen = seenCategoryIds.add(categoryId);
            if (!firstTimeSeen) {
                throw new ProductException(ProductErrorCode.DUPLICATE_CATEGORY, categoryId);
            }
        }
    }

    private ProductResponse toResponse(Product p) {
        Long currentWon = p.currentPrice().map(m -> m.getAmount().longValue()).orElse(null);

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getContent(),
                p.getProductStatus().name(),
                currentWon,
                p.getCategories().stream()
                        .sorted(Comparator.comparingInt(ProductCategory::getSortKey))
                        .map(pc -> new ProductResponse.CategoryBrief(
                                pc.getCategory().getId(),
                                pc.getCategory().getName(),
                                pc.isPrimary(),
                                pc.getSortKey()
                        )).collect(Collectors.toList()),
                p.getMediaList().stream()
                        .filter(m -> m.getType() == MediaType.REPRESENTATIVE)
                        .sorted(Comparator.comparingInt(ProductMedia::getSortKey))
                        .map(m -> new ProductResponse.MediaBrief(
                                m.getId(), m.getUrl(), m.getAltText(), m.getSortKey()
                        )).toList(),
                p.getMediaList().stream()
                        .filter(m -> m.getType() == MediaType.CONTENT)
                        .sorted(Comparator.comparingInt(ProductMedia::getSortKey))
                        .map(m -> new ProductResponse.MediaBrief(
                                m.getId(), m.getUrl(), m.getAltText(), m.getSortKey()
                        )).toList()
        );
    }

    private LocalDateTime parseOrNull(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(iso);
    }

    private void applyPrice(Product product, ProductCreateRequest request) {
        if (request.price() == null) {
            throw new ProductException(ProductErrorCode.PRICE_REQUIRED);
        }

        long listPriceWon = request.price().listPriceWon();
        Money listMoney = Money.of(BigDecimal.valueOf(listPriceWon), "KRW");
        product.setListPrice(listMoney);

        Long salePriceWon = request.price().salePriceWon();
        if (salePriceWon == null || salePriceWon <= 0L) {
            return; // 세일가 미지정
        }

        if (salePriceWon >= listPriceWon) {
            throw new ProductException(ProductErrorCode.SALE_NOT_CHEAPER_THAN_LIST);
        }

        LocalDateTime from = parseOrThrow(request.price().saleFrom());
        LocalDateTime until = parseOrThrow(request.price().saleUntil());
        if (from != null && until != null && until.isBefore(from)) {
            throw new ProductException(ProductErrorCode.INVALID_SALE_PERIOD);
        }

        Money saleMoney = Money.of(BigDecimal.valueOf(salePriceWon), "KRW");
        product.setSalePrice(saleMoney, new DateRange(from, until));
    }

    private LocalDateTime parseOrThrow(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            // 기본 ISO_LOCAL_DATE_TIME: 2025-11-10T00:00:00
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (DateTimeParseException offsetException) {
            try {
                return LocalDateTime.parse(iso);
            } catch (DateTimeParseException localException) {
                throw new ProductException(ProductErrorCode.INVALID_DATETIME_FORMAT, iso);
            }
        }
    }

    private void attachMedia(Product product,
            List<ProductCreateRequest.MediaPayload> representatives,
            List<ProductCreateRequest.MediaPayload> contents) {

        int repInputCount = (representatives == null) ? 0 : representatives.size();
        if (repInputCount > 8) {
            throw new ProductException(ProductErrorCode.REPRESENTATIVE_IMAGE_MAX_OVER,
                    repInputCount);
        }
        // 최소 5장 정책 켤 때:
        // if (repInputCount > 0 && repInputCount < 5) {
        //     throw new ProductException(ProductErrorCode.REPRESENTATIVE_IMAGE_MIN_UNDER, repInputCount);
        // }

        if (representatives != null) {
            for (ProductCreateRequest.MediaPayload m : representatives) {
                product.addRepresentativeImage(m.url(), m.altText(), m.sortKey());
            }
        }
        if (contents != null) {
            for (ProductCreateRequest.MediaPayload m : contents) {
                product.addContentImage(m.url(), m.altText(), m.sortKey());
            }
        }
    }

    // 1) 상세 조회
    @Transactional(readOnly = true)
    public ProductResponse getOne(Long id) {
        Product p = productRepository.findDetailById(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_PRODUCT, id));
        return toResponse(p);
    }

    // 2) 목록/검색/정렬/페이지
    @Transactional(readOnly = true)
    public ProductListPage list(String keyword, Long categoryId, String status, String sort,
            int page, int size) {
        int limit = Math.max(1, Math.min(size, 100));
        int offset = Math.max(0, page) * limit;

        List<ProductListRow> rows = productRepository.findListSimple(
                emptyToNull(keyword),
                categoryId,
                emptyToNull(status),
                emptyToNull(sort),
                limit,
                offset
        );

        long total = productRepository.countListSimple(
                emptyToNull(keyword),
                categoryId,
                emptyToNull(status)
        );

        List<ProductListItem> items = rows.stream().map(r ->
                new ProductListItem(
                        r.getId(),
                        r.getName(),
                        r.getStatus(),
                        r.getCurrentPrice(),
                        r.getRepImageUrl()
                )
        ).toList();

        return new ProductListPage(items, page, limit, total);
    }

    // 3) 상태 전환
    @Transactional
    public ProductResponse publish(Long id) {
        Product p = productRepository.findForStatusChange(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_PRODUCT, id));
        p.publish(); // 카테고리 ACTIVE 검증 내부 수행
        return toResponse(p);
    }

    @Transactional
    public ProductResponse archive(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_PRODUCT, id));
        p.archive();
        return toResponse(p);
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }


}
