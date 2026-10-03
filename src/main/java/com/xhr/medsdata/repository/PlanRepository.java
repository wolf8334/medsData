package com.xhr.medsdata.repository;

import com.xhr.medsdata.common.JdbcUtils;
import com.xhr.medsdata.domain.Plan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PlanRepository {

    private static final RowMapper<Plan> MAP = (rs, i) -> new Plan(
            rs.getLong("id"),
            rs.getLong("person_id"),
            rs.getInt("version_no"),
            rs.getDate("effective_from").toLocalDate(),
            rs.getDate("effective_to") == null ? null : rs.getDate("effective_to").toLocalDate(),
            rs.getString("status"),
            rs.getString("remark"),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Plan> findByPersonId(long personId) {
        return jdbc.query("SELECT * FROM med_plan WHERE person_id = ? ORDER BY version_no DESC", MAP, personId);
    }

    public Optional<Plan> findById(long id) {
        return jdbc.query("SELECT * FROM med_plan WHERE id = ?", MAP, id).stream().findFirst();
    }

    /**
     * 查询某用药人在指定日期生效的方案。
     */
    public Optional<Plan> findEffective(long personId, LocalDate date) {
        String sql = """
                SELECT * FROM med_plan
                WHERE person_id = ? AND effective_from <= ?
                  AND (effective_to IS NULL OR effective_to >= ?)
                ORDER BY effective_from DESC, version_no DESC
                LIMIT 1
                """;
        return jdbc.query(sql, MAP, personId, date, date).stream().findFirst();
    }

    public int maxVersion(long personId) {
        Integer v = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version_no), 0) FROM med_plan WHERE person_id = ?", Integer.class, personId);
        return v == null ? 0 : v;
    }

    public long insert(long personId, int versionNo, LocalDate effectiveFrom, String status, String remark) {
        return JdbcUtils.insert(jdbc,
                "INSERT INTO med_plan(person_id, version_no, effective_from, effective_to, status, remark) VALUES (?, ?, ?, NULL, ?, ?)",
                personId, versionNo, effectiveFrom, status, remark);
    }

    /**
     * 将某用药人在 fromDate 之前仍然有效的方案置为失效并标记 HISTORY。
     */
    public int closeOpenPlansBefore(long personId, LocalDate fromDate, long excludePlanId) {
        return jdbc.update("""
                UPDATE med_plan
                SET effective_to = GREATEST(effective_from, DATE_SUB(?, INTERVAL 1 DAY)), status = 'HISTORY'
                WHERE person_id = ? AND effective_to IS NULL AND id <> ? AND effective_from <= ?
                """, fromDate, personId, excludePlanId, fromDate);
    }

    public int updatePending(long id, LocalDate effectiveFrom, String remark) {
        return jdbc.update("UPDATE med_plan SET effective_from = ?, remark = ? WHERE id = ?",
                effectiveFrom, remark, id);
    }

    public int updateStatus(long id, String status) {
        return jdbc.update("UPDATE med_plan SET status = ? WHERE id = ?", status, id);
    }

    public int updateEffectiveToStatus(long id, LocalDate effectiveTo, String status) {
        if (effectiveTo == null) {
            return jdbc.update("UPDATE med_plan SET effective_to = NULL, status = ? WHERE id = ?", status, id);
        }
        return jdbc.update("UPDATE med_plan SET effective_to = ?, status = ? WHERE id = ?",
                java.sql.Date.valueOf(effectiveTo), status, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM med_plan WHERE id = ?", id);
    }

    public boolean hasRecords(long planId) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(1) FROM med_take_record WHERE plan_id = ?", Integer.class, planId);
        return c != null && c > 0;
    }
}
