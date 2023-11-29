package io.sitoolkit.csv.app.domain.services;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlStatementExecutorTest {

  private Connection connection;
  private SqlStatementExecutor sqlStatementExecutor;

  @BeforeEach
  void setup() throws Exception {
    sqlStatementExecutor = new SqlStatementExecutor();
    setupDatabase();
  }

  @AfterEach
  void connectionClose() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  void executeSqlStatementTest() throws Exception {
    URL sqlDirUrl = getClass().getClassLoader().getResource("csvloader");
    Path sqlDirPath = Paths.get(sqlDirUrl.toURI());

    sqlStatementExecutor.executeSqlStatement(connection, sqlDirPath.toString());

    DatabaseMetaData dbMetaData = connection.getMetaData();
    try (ResultSet rs = dbMetaData.getTables(null, null, "ORDER", null)) {
      assertTrue(rs.next());
    }
  }

  @Test
  void executeInvalidSqlStatementTest() throws URISyntaxException {
    URL sqlDirUrl = getClass().getClassLoader().getResource("invalidSql");
    assertNotNull(sqlDirUrl, "SQL file URL should not be null");

    Path sqlDirPath = Paths.get(sqlDirUrl.toURI());

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          sqlStatementExecutor.executeSqlStatement(connection, sqlDirPath.toString());
        });
  }

  private void setupDatabase() throws Exception {
    Properties prop = new Properties();
    prop.load(getClass().getResourceAsStream("/connection.properties"));
    connection =
        DriverManager.getConnection(
            prop.getProperty("url"), prop.getProperty("user"), prop.getProperty("password"));
  }
}
