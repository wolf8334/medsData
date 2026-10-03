package com.xhr.medsdata.domain;

import java.math.BigDecimal;

public record PlanItem(
        Long id,
        Long planId,
        Long drugId,
        String period,
        BigDecimal dose,
        String doseUnit,
        Integer sortNo,
        String remark
) {
}
