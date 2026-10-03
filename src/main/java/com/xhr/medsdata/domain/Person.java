package com.xhr.medsdata.domain;

import java.time.LocalDateTime;

public record Person(
        Long id,
        String name,
        String status,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
