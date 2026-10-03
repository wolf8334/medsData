package com.xhr.medsdata.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TakeRecord(
        Long id,
        Long personId,
        Long planId,
        LocalDate takeDate,
        String period,
        LocalDateTime takenAt,
        String status,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
