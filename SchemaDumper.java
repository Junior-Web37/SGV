import java.sql.*;

public class SchemaDumper {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mariadb://localhost:3306/sgv";
        String user = "root";
        String pass = "";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData meta = conn.getMetaData();
            StringBuilder out = new StringBuilder();
            out.append("-- SGV Baseline Schema\n");
            out.append("-- Generated from existing MariaDB database\n\n");

            try (ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    out.append("\n-- Table: ").append(tableName).append("\n");
                    out.append("CREATE TABLE ").append(tableName).append(" (\n");

                    try (ResultSet cols = meta.getColumns(null, null, tableName, null)) {
                        java.util.List<String> colDefs = new java.util.ArrayList<>();
                        while (cols.next()) {
                            String col = "  " + cols.getString("COLUMN_NAME")
                                + " " + cols.getString("TYPE_NAME")
                                + "(" + cols.getInt("COLUMN_SIZE") + ")"
                                + (cols.getInt("NULLABLE") == 0 ? " NOT NULL" : "")
                                + (cols.getString("COLUMN_DEF") != null ? " DEFAULT " + cols.getString("COLUMN_DEF") : "");
                            colDefs.add(col);
                        }
                        out.append(String.join(",\n", colDefs));
                    }

                    // Primary key
                    try (ResultSet pks = meta.getPrimaryKeys(null, null, tableName)) {
                        java.util.List<String> pkCols = new java.util.ArrayList<>();
                        while (pks.next()) pkCols.add(pks.getString("COLUMN_NAME"));
                        if (!pkCols.isEmpty()) {
                            out.append(",\n  PRIMARY KEY (").append(String.join(", ", pkCols)).append(")");
                        }
                    }

                    // Foreign keys
                    try (ResultSet fks = meta.getImportedKeys(null, null, tableName)) {
                        while (fks.next()) {
                            String fk = "  FOREIGN KEY (" + fks.getString("FKCOLUMN_NAME")
                                + ") REFERENCES " + fks.getString("PKTABLE_NAME")
                                + "(" + fks.getString("PKCOLUMN_NAME") + ")";
                            out.append(",\n").append(fk);
                        }
                    }

                    out.append("\n);\n");

                    // Indexes
                    try (ResultSet idx = meta.getIndexInfo(null, null, tableName, false, false)) {
                        java.util.Set<String> idxSet = new java.util.LinkedHashSet<>();
                        while (idx.next()) {
                            String idxName = idx.getString("INDEX_NAME");
                            if (idxName != null && !idxName.equals("PRIMARY")) {
                                idxSet.add(idxName);
                            }
                        }
                    }
                }
            }

            System.out.println(out.toString());
        }
    }
}
