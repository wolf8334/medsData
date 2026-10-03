package com.xhr.medsdata.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Drug(
        Long id,
        String name,
        String specification,
        String stockUnit,
        String packUnit,
        BigDecimal packSize,
        BigDecimal minStock,
        String status,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
