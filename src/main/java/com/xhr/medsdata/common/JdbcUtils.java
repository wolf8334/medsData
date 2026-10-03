package com.xhr.medsdata.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JDBC 小工具：插入并返回自增主键。
 */
public final class JdbcUtils {

    private JdbcUtils() {
    }

    public static long insert(JdbcTemplate jdbc, String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, normalize(args[i]));
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    private static Object normalize(Object value) {
        if (value instanceof LocalDate d) {
            return java.sql.Date.valueOf(d);
        }
        if (value instanceof LocalDateTime dt) {
            return Timestamp.valueOf(dt);
        }
        return value;
    }
}
