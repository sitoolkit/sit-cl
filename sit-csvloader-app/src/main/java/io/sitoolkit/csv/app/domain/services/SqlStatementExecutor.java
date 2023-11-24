package io.sitoolkit.csv.app.domain.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;

public class SqlStatementExecutor {

  public void executeSqlStatement(Connection connection, File resFileDir) {
    Optional.ofNullable(resFileDir.listFiles((dir, name) -> name.endsWith(".sql"))).stream()
        .flatMap(Arrays::stream)
        .forEach(sqlFile -> executeSqlFile(connection, sqlFile));
  }

  private void executeSqlFile(Connection connection, File sqlFile) {
    try {
      execute(connection, sqlFile);
    } catch (IOException | SQLException e) {
      e.printStackTrace();
      throw new IllegalArgumentException();
    }
  }

  private void execute(Connection connection, File sqlFile) throws IOException, SQLException {
    String sql = new String(Files.readAllBytes(sqlFile.toPath()), StandardCharsets.UTF_8);
    if (sql.trim().isEmpty()) {
      throw new SQLException("Invalid SQL file: " + sqlFile.getAbsolutePath());
    }
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(sql);
    }
  }
}
