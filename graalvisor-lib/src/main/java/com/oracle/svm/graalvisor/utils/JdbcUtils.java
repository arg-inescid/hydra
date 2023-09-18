package com.oracle.svm.graalvisor.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class JdbcUtils {

    public static String resultSetToString(ResultSet rs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(System.getProperty("line.separator"));
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int nColumns = metaData.getColumnCount();
            while (rs.next()) {
                sb.append("[");
                for (int i = 1; i <= nColumns; ++i) {
                    sb.append(metaData.getColumnName(i));
                    sb.append(" = ");
                    sb.append(rs.getString(i));
                    if (i < nColumns) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                sb.append(System.getProperty("line.separator"));
            }
        } catch (SQLException e) {
            sb.append(e.getMessage());
            e.printStackTrace();
        }
        sb.append("}");
        return sb.toString();
    }

}
