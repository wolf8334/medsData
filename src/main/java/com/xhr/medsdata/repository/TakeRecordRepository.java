package com.xhr.medsdata.repository;

import com.xhr.medsdata.common.JdbcUtils;
import com.xhr.medsdata.common.Period;
import com.xhr.medsdata.domain.TakeRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TakeRecordRepository {

    private static final RowMapper<TakeRecord> MAP = (rs, i) -> new TakeRecord(
            rs.getLong("id"),
            rs.getLong("person_id"),
            rs.getLong("plan_id"),
            rs.getDate("take_date").toLocalDate(),
            rs.getString("period"),
            rs.getTimestamp("taken_at").toLocalDateTime(),
            rs.getString("status"),
            rs.getString("remark"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public TakeRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<TakeRecord> findById(long id) {
        return jdbc.query("SELECT * FROM med_take_record WHERE id = ?", MAP, id).stream().findFirst();
    }

    public Optional<TakeRecord> findActive(long personId, LocalDate date, String period) {
        return jdbc.query("""
                        SELECT * FROM med_take_record
                        WHERE person_id = ? AND take_date = ? AND period = ? AND status = 'TAKEN'
                        ORDER BY id DESC LIMIT 1
                        """,
                MAP, personId, date, Period.of(period).name()).stream().findFirst();
    }

    public List<TakeRecord> findByPersonAndDate(long personId, LocalDate date) {
        return jdbc.query("SELECT * FROM med_take_record WHERE person_id = ? AND take_date = ? ORDER BY period",
                MAP, personId, date);
    }

    public List<TakeRecord> findByDate(LocalDate date) {
        return jdbc.query("SELECT * FROM med_take_record WHERE take_date = ? ORDER BY person_id, period",
                MAP, date);
    }

    public List<TakeRecord> findByRange(LocalDate from, LocalDate to) {
        return jdbc.query("SELECT * FROM med_take_record WHERE take_date BETWEEN ? AND ? ORDER BY take_date",
                MAP, from, to);
    }

    public List<TakeRecord> findByPersonAndRange(long personId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                        SELECT * FROM med_take_record
                        WHERE person_id = ? AND take_date BETWEEN ? AND ?
                        ORDER BY take_date, period
                        """,
                MAP, personId, from, to);
    }

    public long insert(long personId, long planId, LocalDate takeDate, String period,
                       LocalDateTime takenAt, String remark) {
        return JdbcUtils.insert(jdbc, """
                        INSERT INTO med_take_record(person_id, plan_id, take_date, period, taken_at, status, remark)
                        VALUES (?, ?, ?, ?, ?, 'TAKEN', ?)
                        """,
                personId, planId, takeDate, Period.of(period).name(), takenAt, remark);
    }

    public int cancel(long id) {
        return jdbc.update("UPDATE med_take_record SET status = 'CANCELLED' WHERE id = ?", id);
    }

    public int update(long id, LocalDateTime takenAt, String remark) {
        return jdbc.update("UPDATE med_take_record SET taken_at = ?, remark = ? WHERE id = ?",
                takenAt, remark, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM med_take_record WHERE id = ?", id);
    }
}
