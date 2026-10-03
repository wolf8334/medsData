package com.xhr.medsdata.repository;

import com.xhr.medsdata.common.JdbcUtils;
import com.xhr.medsdata.domain.Drug;
import com.xhr.medsdata.dto.Requests.DrugReq;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DrugRepository {

    private static final RowMapper<Drug> MAP = (rs, i) -> new Drug(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("specification"),
            rs.getString("stock_unit"),
            rs.getString("pack_unit"),
            rs.getBigDecimal("pack_size"),
            rs.getBigDecimal("min_stock"),
            rs.getString("status"),
            rs.getString("remark"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public DrugRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Drug> findAll(String status) {
        if (status == null || status.isBlank()) {
            return jdbc.query("SELECT * FROM med_drug ORDER BY id", MAP);
        }
        return jdbc.query("SELECT * FROM med_drug WHERE status = ? ORDER BY id", MAP, status);
    }

    public Optional<Drug> findById(long id) {
        return jdbc.query("SELECT * FROM med_drug WHERE id = ?", MAP, id).stream().findFirst();
    }

    public boolean existsById(long id) {
        Integer c = jdbc.queryForObject("SELECT COUNT(1) FROM med_drug WHERE id = ?", Integer.class, id);
        return c != null && c > 0;
    }

    public long insert(DrugReq r) {
        return JdbcUtils.insert(jdbc,
                "INSERT INTO med_drug(name, specification, stock_unit, pack_unit, pack_size, min_stock, status, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                r.name(), r.specification(), r.stockUnit(), r.packUnit(), r.packSize(), r.minStock(),
                r.status() == null ? "ENABLED" : r.status(), r.remark());
    }

    public int update(long id, DrugReq r) {
        return jdbc.update(
                "UPDATE med_drug SET name = ?, specification = ?, stock_unit = ?, pack_unit = ?, pack_size = ?, min_stock = ?, status = ?, remark = ? WHERE id = ?",
                r.name(), r.specification(), r.stockUnit(), r.packUnit(), r.packSize(), r.minStock(), r.status(), r.remark(), id);
    }

    public int updateStatus(long id, String status) {
        return jdbc.update("UPDATE med_drug SET status = ? WHERE id = ?", status, id);
    }
}
