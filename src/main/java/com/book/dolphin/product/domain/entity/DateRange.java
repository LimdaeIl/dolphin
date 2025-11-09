package com.book.dolphin.product.domain.entity;

import java.time.LocalDateTime;


public record DateRange(
        LocalDateTime from,
        LocalDateTime until
) {

}
