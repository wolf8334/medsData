package com.xhr.medsdata.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Plan(
        Long id,
        Long personId,
        Integer versionNo,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status,
        String remark,
        LocalDateTime createdAt
) {
}
