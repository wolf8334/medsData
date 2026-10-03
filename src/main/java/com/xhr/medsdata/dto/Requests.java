package com.xhr.medsdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 前端请求体。
 */
public final class Requests {

    private Requests() {
    }

    public record PersonReq(String name, String status, String remark) {
    }

    public record DrugReq(String name, String specification, String stockUnit,
                          String packUnit, BigDecimal packSize,
                          BigDecimal minStock, String status, String remark) {
    }

    public record PlanItemReq(Long drugId, String period, BigDecimal dose,
                              String doseUnit, Integer sortNo, String remark) {
    }

    public record PlanReq(Long personId, LocalDate effectiveFrom, String remark,
                          List<PlanItemReq> items) {
    }

    public record NewVersionReq(LocalDate effectiveFrom, String remark,
                                List<PlanItemReq> items) {
    }

    public record TakeReq(Long personId, LocalDate takeDate, String period,
                          LocalDateTime takenAt, String remark) {
    }

    public record TakeUpdateReq(LocalDateTime takenAt, String remark) {
    }

    public record StockOpReq(Long drugId, BigDecimal quantity, String changeType, String remark) {
    }
}
