package com.xhr.medsdata.repository;

import com.xhr.medsdata.common.JdbcUtils;
import com.xhr.medsdata.domain.StockLog;
import com.xhr.medsdata.domain.StockView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class StockRepository {

    private static final RowMapper<StockView> VIEW_MAP = (rs, i) -> {
        BigDecimal qty = rs.getBigDecimal("quantity");
        if (qty == null) {
            qty = BigDecimal.ZERO;
        }
        BigDecimal min = rs.getBigDecimal("min_stock");
        boolean low = min != null && qty.compareTo(min) < 0;
        return new StockView(
                rs.getLong("drug_id"),
                rs.getString("drug_name"),
                rs.getString("specification"),
                rs.getString("stock_unit"),
                rs.getString("pack_unit"),
                rs.getBigDecimal("pack_size"),
                qty,
                min,
                low,
                rs.getString("drug_status"));
    };

    private static final RowMapper<StockLog> LOG_MAP = (rs, i) -> new StockLog(
            rs.getLong("id"),
            rs.getLong("drug_id"),
            rs.getBigDecimal("change_qty"),
            rs.getBigDecimal("before_qty"),
            rs.getBigDecimal("after_qty"),
            rs.getString("change_type"),
            rs.getString("remark"),
            rs.getTimestamp("created_at").toLocalDateTime());

    private static final String BASE_VIEW = """
            SELECT d.id AS drug_id, d.name AS drug_name, d.specification, d.stock_unit,
                   d.pack_unit, d.pack_size, d.min_stock, d.status AS drug_status,
                   COALESCE(s.quantity, 0) AS quantity
            FROM med_drug d
            LEFT JOIN med_stock s ON s.drug_id = d.id
            """;

    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<BigDecimal> findQuantity(long drugId) {
        return jdbc.query("SELECT quantity FROM med_stock WHERE drug_id = ?",
                (rs, i) -> rs.getBigDecimal("quantity"), drugId).stream().findFirst();
    }

    public void upsertQuantity(long drugId, BigDecimal quantity) {
        int updated = jdbc.update("UPDATE med_stock SET quantity = ? WHERE drug_id = ?", quantity, drugId);
        if (updated == 0) {
            jdbc.update("INSERT INTO med_stock(drug_id, quantity) VALUES (?, ?)", drugId, quantity);
        }
    }

    public long insertLog(long drugId, BigDecimal changeQty, BigDecimal beforeQty, BigDecimal afterQty,
                          String changeType, String remark) {
        return JdbcUtils.insert(jdbc, """
                        INSERT INTO med_stock_log(drug_id, change_qty, before_qty, after_qty, change_type, remark)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                drugId, changeQty, beforeQty, afterQty, changeType, remark);
    }

    public List<StockView> findAllViews() {
        return jdbc.query(BASE_VIEW + " ORDER BY d.id", VIEW_MAP);
    }

    public Optional<StockView> findView(long drugId) {
        return jdbc.query(BASE_VIEW + " WHERE d.id = ?", VIEW_MAP, drugId).stream().findFirst();
    }

    public List<StockView> findLowStock() {
        return jdbc.query(BASE_VIEW + """
                 WHERE d.min_stock IS NOT NULL AND COALESCE(s.quantity, 0) < d.min_stock
                 ORDER BY d.id
                """, VIEW_MAP);
    }

    public List<StockLog> findLogs(Long drugId, int limit) {
        if (drugId == null) {
            return jdbc.query("SELECT * FROM med_stock_log ORDER BY id DESC LIMIT ?", LOG_MAP, limit);
        }
        return jdbc.query("SELECT * FROM med_stock_log WHERE drug_id = ? ORDER BY id DESC LIMIT ?",
                LOG_MAP, drugId, limit);
    }
}
