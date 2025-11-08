package com.book.dolphin.category.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryClosureId implements Serializable {

    @Column(name = "ancestor_id", nullable = false)
    private Long ancestorId;

    @Column(name = "descendant_id", nullable = false)
    private Long descendantId;

    private CategoryClosureId(Long ancestorId, Long descendantId) {
        this.ancestorId = ancestorId;
        this.descendantId = descendantId;
    }

    public static CategoryClosureId create(Long ancestorId, Long descendantId) {
        return new CategoryClosureId(ancestorId, descendantId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoryClosureId that)) {
            return false;
        }
        return Objects.equals(ancestorId, that.ancestorId)
                && Objects.equals(descendantId, that.descendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ancestorId, descendantId);
    }
}
