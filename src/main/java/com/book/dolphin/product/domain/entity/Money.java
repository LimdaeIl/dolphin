package com.book.dolphin.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Money {

    // 소수 처리 이슈를 피하려면 minor unit 저장(예: KRW는 정수)도 가능
    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency; // "KRW", "USD"...

    public static Money of(long won) {
        return new Money(BigDecimal.valueOf(won), "KRW");
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public Money plus(Money other) {
        validateCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    private void validateCurrency(Money other) {
        if (!Objects.equals(this.currency, other.currency)) {
            throw new IllegalArgumentException("화폐 단위가 다릅니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return Objects.equals(amount, money.amount)
                && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}
