package io.sitoolkit.csv.app.domain.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

public class SqlStatementExecutor {

  public void executeSqlStatement(Connection connection, String resFileDirPath) throws IOException {
    Path resFileDir = Paths.get(resFileDirPath);
    try (Stream<Path> paths = Files.walk(resFileDir)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".sql"))
          .forEach(
              sqlFile -> {
                try {
                  executeSqlFile(connection, sqlFile);
                } catch (IOException | SQLException e) {
                  throw new IllegalArgumentException(e);
                }
              });
    }
  }

  private void executeSqlFile(Connection connection, Path sqlFilePath)
      throws IOException, SQLException {
    String sql = Files.readString(sqlFilePath);
    if (sql.trim().isEmpty()) {
      throw new IOException("SQL file is empty: " + sqlFilePath);
    }

    if (isDdl(sql)) {
      try (Statement stmt = connection.createStatement()) {
        stmt.execute(sql);
      }
    } else {
      throw new SQLException(
          "SQL statement is invalid. \n The only SQL statements that can be executed are DDL.");
    }
  }

  private boolean isDdl(String sql) {
    String trimmedSql = sql.trim().toLowerCase();
    return trimmedSql.startsWith("create")
        || trimmedSql.startsWith("alter")
        || trimmedSql.startsWith("drop");
  }
}
