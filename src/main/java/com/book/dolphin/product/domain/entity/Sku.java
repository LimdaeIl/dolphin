package com.book.dolphin.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Sku {

    @Column(name = "sku_code", nullable = false, length = 64, unique = true)
    private String code;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Sku sku)) {
            return false;
        }
        return Objects.equals(code, sku.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
