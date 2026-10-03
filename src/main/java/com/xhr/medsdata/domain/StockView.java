package com.xhr.medsdata.domain;

import java.math.BigDecimal;

/**
 * 当前库存视图（含药品信息与低库存标记）。
 */
public record StockView(
        Long drugId,
        String drugName,
        String specification,
        String stockUnit,
        String packUnit,
        BigDecimal packSize,
        BigDecimal quantity,
        BigDecimal minStock,
        boolean low,
        String drugStatus
) {
}
