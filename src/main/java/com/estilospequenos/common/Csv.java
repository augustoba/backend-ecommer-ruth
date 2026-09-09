package com.estilospequenos.common;

import java.util.List;

/** Armador mínimo de CSV (RFC 4180: comillas dobles cuando hace falta). */
public final class Csv {

    private final StringBuilder sb = new StringBuilder();

    private Csv() {}

    public static Csv withHeader(String... header) {
        Csv c = new Csv();
        c.row((Object[]) header);
        return c;
    }

    public Csv row(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(values[i]));
        }
        sb.append("\r\n");
        return this;
    }

    public Csv rows(List<Object[]> rows) {
        rows.forEach(this::row);
        return this;
    }

    public String build() {
        return sb.toString();
    }

    private static String cell(Object v) {
        String s = v == null ? "" : v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
