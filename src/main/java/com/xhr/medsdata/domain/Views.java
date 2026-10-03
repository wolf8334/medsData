package com.xhr.medsdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 页面聚合视图。
 */
public final class Views {

    private Views() {
    }

    public record PlanItemView(
            Long id,
            Long drugId,
            String drugName,
            String specification,
            String period,
            String periodLabel,
            BigDecimal dose,
            String doseUnit,
            Integer sortNo,
            String remark
    ) {
    }

    public record PeriodView(
            String period,
            String label,
            List<PlanItemView> items,
            boolean taken,
            Long recordId,
            LocalDateTime takenAt,
            String remark
    ) {
    }

    public record PersonDayView(
            Long personId,
            String personName,
            Long planId,
            Integer versionNo,
            String planStatus,
            LocalDate planEffectiveFrom,
            List<PeriodView> periods
    ) {
    }

    public record DayView(
            LocalDate date,
            List<PersonDayView> persons
    ) {
    }

    public record CalendarDayView(
            LocalDate date,
            int expected,
            int taken,
            boolean completed
    ) {
    }

    public record PlanDetail(
            Plan plan,
            List<PlanItemView> items
    ) {
    }
}
