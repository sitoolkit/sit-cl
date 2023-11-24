package io.sitoolkit.csv.app.domain.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
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
    File sqlDir = new File(Paths.get(sqlDirUrl.toURI()).toString());

    sqlStatementExecutor.executeSqlStatement(connection, sqlDir);

    DatabaseMetaData dbMetaData = connection.getMetaData();
    try (ResultSet rs = dbMetaData.getTables(null, null, "ORDER", null)) {
      assertTrue(rs.next());
    }
  }

  @Test
  void executeInvalidSqlStatementTest() throws URISyntaxException {
    URL sqlDirUrl = getClass().getClassLoader().getResource("csvloader/invalidSql");
    assertNotNull(sqlDirUrl, "SQL file URL should not be null");

    File sqlDir = new File(Paths.get(sqlDirUrl.toURI()).toString());

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          sqlStatementExecutor.executeSqlStatement(connection, sqlDir);
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
