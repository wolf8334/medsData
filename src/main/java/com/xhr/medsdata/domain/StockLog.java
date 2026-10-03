package com.xhr.medsdata.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockLog(
        Long id,
        Long drugId,
        BigDecimal changeQty,
        BigDecimal beforeQty,
        BigDecimal afterQty,
        String changeType,
        String remark,
        LocalDateTime createdAt
) {
}
