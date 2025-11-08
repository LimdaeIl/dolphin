package com.book.dolphin.category.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "category_closures",
        indexes = {
                @Index(name = "idx_cc_ancestor", columnList = "ancestor_id"),
                @Index(name = "idx_cc_descendant", columnList = "descendant_id"),
                @Index(name = "idx_cc_ancestor_depth", columnList = "ancestor_id,depth"),
                @Index(name = "idx_cc_descendant_depth", columnList = "descendant_id,depth")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryClosure {

    @EmbeddedId
    private CategoryClosureId id;

    @MapsId("ancestorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ancestor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cc_ancestor"))
    // @OnDelete(action = OnDeleteAction.CASCADE) // (Hibernate 전용, 선택)
    private Category ancestor;

    @MapsId("descendantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "descendant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cc_descendant"))
    // @OnDelete(action = OnDeleteAction.CASCADE) // (Hibernate 전용, 선택)
    private Category descendant;

    /**
     * 거리: 자기자신=0, 직계=1, ...
     */
    @Column(name = "depth", nullable = false)
    private int depth;

    private CategoryClosure(CategoryClosureId id, Category ancestor, Category descendant,
            int depth) {
        this.id = id;
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.depth = depth;
    }

    public static CategoryClosure create(Category ancestor, Category descendant, int depth) {
        return new CategoryClosure(
                CategoryClosureId.create(ancestor.getId(), descendant.getId()),
                ancestor,
                descendant,
                depth
        );
    }
}
