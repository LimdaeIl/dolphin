package support;

import com.book.dolphin.category.domain.entity.Category;
import com.book.dolphin.category.domain.entity.CategoryClosure;
import com.book.dolphin.category.domain.entity.CategoryStatus;
import com.book.dolphin.category.domain.repository.CategoryClosureRepository;
import com.book.dolphin.category.domain.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;

public class CategoryTestSeeder {

    private final CategoryRepository categoryRepository;
    private final CategoryClosureRepository closureRepository;

    public CategoryTestSeeder(CategoryRepository categoryRepository,
            CategoryClosureRepository closureRepository) {
        this.categoryRepository = categoryRepository;
        this.closureRepository = closureRepository;
    }

    /**
     * 루트 생성 + 클로저(self) 저장
     */
    public Category seedRoot(String name, String slug, int sortOrder, CategoryStatus status,
            String imageUrl) {
        Category root = Category.createRoot(name, slug, sortOrder, status, imageUrl);
        Category saved = categoryRepository.save(root);

        List<CategoryClosure> closures = new ArrayList<>();
        closures.add(CategoryClosure.create(saved, saved, 0)); // self
        closureRepository.saveAll(closures);

        return saved;
    }

    /**
     * 자식 생성 + self + (부모의 모든 조상 → depth+1) 클로저 저장
     */
    public Category seedChild(Category parent, String name, String slug, int sortOrder,
            CategoryStatus status, String imageUrl) {
        Category child = Category.createChild(name, slug, parent, sortOrder, status, imageUrl);
        Category saved = categoryRepository.save(child);

        List<CategoryClosure> closures = new ArrayList<>();
        closures.add(CategoryClosure.create(saved, saved, 0)); // self

        var parentAncestors = closureRepository.findAllAncestorsOf(
                parent.getId()); // ancestor, depth
        for (var pa : parentAncestors) {
            closures.add(CategoryClosure.create(pa.getAncestor(), saved, pa.getDepth() + 1));
        }
        closureRepository.saveAll(closures);

        return saved;
    }
}
