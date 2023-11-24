package io.sitoolkit.csv.app.interfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainTest {

  private Connection connection;
  private Main main;

  @BeforeEach
  void setup() throws Exception {
    main = new Main();
  }

  @AfterEach
  void connectionClose() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  void executeTest() throws Exception {
    setupDatabase();
    URL connectionPropUrl = getClass().getClassLoader().getResource("connection.properties");
    String connectionPropertiesPath = Paths.get(connectionPropUrl.toURI()).toString();

    URL csvLoaderDirUrl = getClass().getClassLoader().getResource("csvloader");
    String csvLoaderDirPath = Paths.get(csvLoaderDirUrl.toURI()).toString();

    main.execute(new String[] {connectionPropertiesPath, csvLoaderDirPath});
    String selectFromOrder =
        "SELECT * FROM \"ORDER\""
            .replace("\"", connection.getMetaData().getIdentifierQuoteString());
    ResultSet rs = connection.createStatement().executeQuery(selectFromOrder);
    assertTrue(rs.next());

    assertEquals(1, rs.getInt("FROM"));
    assertEquals("one", rs.getString("COL_VARCHAR"));
    assertEquals("2020-12-29", rs.getString("COL_DATE"));
    assertEquals("12:30:00", rs.getString("COL_TIME"));
    assertEquals(true, rs.getBoolean("COL_BOOLEAN"));

    assertTrue(rs.next());

    assertEquals(2, rs.getInt("FROM"));
    assertEquals("", rs.getString("COL_VARCHAR"));
    assertEquals("2020-12-30", rs.getString("COL_DATE"));

    assertTrue(rs.next());

    assertEquals(null, rs.getObject("FROM"));
    assertEquals(null, rs.getObject("COL_DECIMAL"));
    assertEquals(null, rs.getObject("COL_VARCHAR"));
    assertEquals(null, rs.getObject("COL_DATE"));
    assertEquals(null, rs.getObject("COL_TIMESTAMP"));
    assertEquals(null, rs.getObject("COL_TIME"));
    assertEquals(null, rs.getObject("COL_JSON"));
    assertEquals(null, rs.getObject("COL_BOOLEAN"));

    assertTrue(rs.next());

    assertEquals(1, rs.getInt("FROM"));
    assertEquals("one", rs.getString("COL_VARCHAR"));
    assertEquals("2020-12-29", rs.getString("COL_DATE"));
    assertEquals("12:30:00", rs.getString("COL_TIME"));
    assertEquals(true, rs.getBoolean("COL_BOOLEAN"));

    assertTrue(rs.next());

    assertEquals(3, rs.getInt("FROM"));
    assertEquals("three", rs.getString("COL_VARCHAR"));
    assertEquals("2020-12-31", rs.getString("COL_DATE"));
    assertEquals("12:30:00", rs.getString("COL_TIME"));
    assertEquals(true, rs.getBoolean("COL_BOOLEAN"));

    assertTrue(rs.next());

    assertEquals(4, rs.getInt("FROM"));
    assertEquals("four", rs.getString("COL_VARCHAR"));
    assertEquals("2021-01-01", rs.getString("COL_DATE"));
    assertEquals("12:30:00", rs.getString("COL_TIME"));
    assertEquals(false, rs.getBoolean("COL_BOOLEAN"));

    assertFalse(rs.next());
  }

  @Test
  void insufficientArgumentsTest() throws URISyntaxException {
    URL connectionPropUrl = getClass().getClassLoader().getResource("connection.properties");
    String connectionPropertiesPath = Paths.get(connectionPropUrl.toURI()).toString();
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> main.execute(new String[] {connectionPropertiesPath}));
    assertEquals("Missing arguments.", exception.getMessage());
  }

  @Test
  void loadInvalidPropertyFileTest() throws Exception {
    String invalidPath = "noexist/connection.properties";
    URL csvLoaderDirUrl = getClass().getClassLoader().getResource("csvloader");
    String csvLoaderDirPath = Paths.get(csvLoaderDirUrl.toURI()).toString();

    assertThrows(
        IllegalArgumentException.class,
        () -> main.execute(new String[] {invalidPath, csvLoaderDirPath}));
  }

  @Test
  void findTableDataResourcesIOExceptionTest() throws Exception {
    setupDatabase();
    URL connectionPropUrl = getClass().getClassLoader().getResource("connection.properties");
    String connectionPropertiesPath = Paths.get(connectionPropUrl.toURI()).toString();
    main.execute(new String[] {connectionPropertiesPath, "csvLoaderDirPath"});

    String selectFromOrder =
        "SELECT * FROM \"ORDER\""
            .replace("\"", connection.getMetaData().getIdentifierQuoteString());

    ResultSet rs = connection.createStatement().executeQuery(selectFromOrder);
    assertFalse(rs.next());
  }

  private void setupDatabase() throws Exception {
    Properties prop = new Properties();
    prop.load(getClass().getResourceAsStream("/connection.properties"));
    connection =
        DriverManager.getConnection(
            prop.getProperty("url"), prop.getProperty("user"), prop.getProperty("password"));
    String createTable =
        Files.readString(Path.of(getClass().getResource("/csvloader/CREATE_TABLE.sql").toURI()));
    String identifierQuoteString = connection.getMetaData().getIdentifierQuoteString();
    createTable = createTable.replace("\"", identifierQuoteString);
    connection.createStatement().executeUpdate(createTable);
  }
}
