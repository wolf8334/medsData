package com.xhr.medsdata.common;

import java.util.List;

/**
 * 服药时段。第一版仅早、晚，后续可扩展。
 */
public enum Period {
    EARLY("早"),
    EVENING("晚");

    private final String label;

    Period(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static List<Period> ordered() {
        return List.of(EARLY, EVENING);
    }

    public static Period of(String code) {
        if (code == null) {
            throw new BusinessException("服药时段不能为空");
        }
        try {
            return Period.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的服药时段: " + code);
        }
    }

    public static String labelOf(String code) {
        try {
            return of(code).label();
        } catch (BusinessException e) {
            return code;
        }
    }
}
