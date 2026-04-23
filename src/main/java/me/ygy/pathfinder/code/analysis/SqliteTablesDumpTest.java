package me.ygy.pathfinder.code.analysis;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteTablesDumpTest {

    private static Path resolveServiceUsageAnalyzerDbPath() {
        // 默认从 pathfinder 模块根目录运行：../data/service-usage-analyzer.db
        return Paths.get(System.getProperty("user.dir"))
                .resolve("../data/service-usage-analyzer.db")
                .normalize()
                .toAbsolutePath();
    }

    public static void main(String[] args) throws Exception {
        Path dbPath = resolveServiceUsageAnalyzerDbPath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            dumpTable(conn, "backend_service_define");
//            dumpTable(conn, "frontend_service_usage");
        }
    }

    private static void dumpTable(Connection conn, String tableName) throws SQLException {
        System.out.println("\n\n===== TABLE: " + tableName + " =====");
        try (Statement st = conn.createStatement()) {
            st.setQueryTimeout(30);
            try (ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();

                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) System.out.print(" | ");
                    System.out.print(md.getColumnName(i));
                }
                System.out.println();

                long rowNum = 0;
                while (rs.next()) {
                    rowNum++;
                    for (int i = 1; i <= colCount; i++) {
                        if (i > 1) System.out.print(" | ");
                        Object v = rs.getObject(i);
                        System.out.print(v == null ? "NULL" : String.valueOf(v));
                    }
                    System.out.println();
                }
                System.out.println("----- rows: " + rowNum + " -----");
            }
        } catch (SQLException e) {
            System.out.println("查询失败（表可能不存在）: " + e.getMessage());
            throw e;
        }
    }
}

