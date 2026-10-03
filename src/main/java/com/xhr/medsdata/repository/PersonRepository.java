package com.xhr.medsdata.repository;

import com.xhr.medsdata.common.JdbcUtils;
import com.xhr.medsdata.domain.Person;
import com.xhr.medsdata.dto.Requests.PersonReq;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PersonRepository {

    private static final RowMapper<Person> MAP = (rs, i) -> new Person(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("status"),
            rs.getString("remark"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> findAll(String status) {
        if (status == null || status.isBlank()) {
            return jdbc.query("SELECT * FROM med_person ORDER BY id", MAP);
        }
        return jdbc.query("SELECT * FROM med_person WHERE status = ? ORDER BY id", MAP, status);
    }

    public Optional<Person> findById(long id) {
        return jdbc.query("SELECT * FROM med_person WHERE id = ?", MAP, id).stream().findFirst();
    }

    public boolean existsById(long id) {
        Integer c = jdbc.queryForObject("SELECT COUNT(1) FROM med_person WHERE id = ?", Integer.class, id);
        return c != null && c > 0;
    }

    public long insert(PersonReq r) {
        return JdbcUtils.insert(jdbc,
                "INSERT INTO med_person(name, status, remark) VALUES (?, ?, ?)",
                r.name(), r.status() == null ? "ENABLED" : r.status(), r.remark());
    }

    public int update(long id, PersonReq r) {
        return jdbc.update("UPDATE med_person SET name = ?, status = ?, remark = ? WHERE id = ?",
                r.name(), r.status(), r.remark(), id);
    }

    public int updateStatus(long id, String status) {
        return jdbc.update("UPDATE med_person SET status = ? WHERE id = ?", status, id);
    }
}
