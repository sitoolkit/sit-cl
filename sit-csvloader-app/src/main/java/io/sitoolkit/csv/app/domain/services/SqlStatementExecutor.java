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
      throw new IOException("read SQL file is empty" + sqlFilePath);
    }
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(sql);
    }
  }
}
