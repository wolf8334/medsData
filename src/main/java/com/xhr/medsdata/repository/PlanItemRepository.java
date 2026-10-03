package com.xhr.medsdata.repository;

import com.xhr.medsdata.common.Period;
import com.xhr.medsdata.domain.Views.PlanItemView;
import com.xhr.medsdata.dto.Requests.PlanItemReq;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PlanItemRepository {

    private static final RowMapper<PlanItemView> MAP = (rs, i) -> new PlanItemView(
            rs.getLong("id"),
            rs.getLong("drug_id"),
            rs.getString("drug_name"),
            rs.getString("specification"),
            rs.getString("period"),
            Period.labelOf(rs.getString("period")),
            rs.getBigDecimal("dose"),
            rs.getString("dose_unit"),
            rs.getInt("sort_no"),
            rs.getString("remark"));

    private static final String SELECT_VIEW = """
            SELECT i.id, i.drug_id, i.period, i.dose, i.dose_unit, i.sort_no, i.remark,
                   d.name AS drug_name, d.specification
            FROM med_plan_item i
            JOIN med_drug d ON d.id = i.drug_id
            WHERE i.plan_id = ?
            ORDER BY FIELD(i.period, 'EARLY', 'EVENING'), i.sort_no, i.id
            """;

    private final JdbcTemplate jdbc;

    public PlanItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PlanItemView> findViews(long planId) {
        return jdbc.query(SELECT_VIEW, MAP, planId);
    }

    public void insertBatch(long planId, List<PlanItemReq> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                        INSERT INTO med_plan_item(plan_id, drug_id, period, dose, dose_unit, sort_no, remark)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                items, items.size(), (ps, item) -> {
                    ps.setLong(1, planId);
                    ps.setLong(2, item.drugId());
                    ps.setString(3, Period.of(item.period()).name());
                    ps.setBigDecimal(4, item.dose());
                    ps.setString(5, item.doseUnit());
                    ps.setInt(6, item.sortNo() == null ? 0 : item.sortNo());
                    ps.setString(7, item.remark());
                });
    }

    public int deleteByPlanId(long planId) {
        return jdbc.update("DELETE FROM med_plan_item WHERE plan_id = ?", planId);
    }
}
