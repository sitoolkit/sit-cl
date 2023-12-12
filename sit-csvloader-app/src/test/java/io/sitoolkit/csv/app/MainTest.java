package io.sitoolkit.csv.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
  void firstRowTest() throws Exception {
    setupDatabase();
    executeMain();
    ResultSet rs = executeSelectFromOrder();
    assertTrue(rs.next());
    assertRow(rs, 1, "one", "2020-12-29", "12:30:00", true);
    rs.close();
  }

  @Test
  void secondRowTest() throws Exception {
    setupDatabase();
    executeMain();
    ResultSet rs = executeSelectFromOrder();
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertEquals(2, rs.getInt("FROM"));
    assertEquals("", rs.getString("COL_VARCHAR"));
    assertEquals("2020-12-30", rs.getString("COL_DATE"));
    rs.close();
  }

  @Test
  void thirdRowTest() throws Exception {
    setupDatabase();
    executeMain();
    ResultSet rs = executeSelectFromOrder();
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertEquals(null, rs.getObject("FROM"));
    assertEquals(null, rs.getObject("COL_DECIMAL"));
    assertEquals(null, rs.getObject("COL_VARCHAR"));
    assertEquals(null, rs.getObject("COL_DATE"));
    assertEquals(null, rs.getObject("COL_TIMESTAMP"));
    assertEquals(null, rs.getObject("COL_TIME"));
    assertEquals(null, rs.getObject("COL_JSON"));
    assertEquals(null, rs.getObject("COL_BOOLEAN"));
    rs.close();
  }

  @Test
  void fourthRowTest() throws Exception {
    setupDatabase();
    executeMain();
    ResultSet rs = executeSelectFromOrder();
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertRow(rs, 1, "one", "2020-12-29", "12:30:00", true);
    rs.close();
  }

  @Test
  void fifthRowTest() throws Exception {
    setupDatabase();
    executeMain();
    ResultSet rs = executeSelectFromOrder();
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertRow(rs, 3, "three", "2020-12-31", "12:30:00", true);
    rs.close();
  }

  @Test
  void sixthRowTest() throws Exception {
    setupDatabase();
    executeMain();
    ResultSet rs = executeSelectFromOrder();
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertTrue(rs.next());
    assertRow(rs, 4, "four", "2021-01-01", "12:30:00", false);
    rs.close();
  }

  @Test
  void insufficientArgumentsTest() throws URISyntaxException {
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(outContent));

    URL connectionPropUrl = getClass().getClassLoader().getResource("connection.properties");
    String connectionPropertiesPath = Paths.get(connectionPropUrl.toURI()).toString();
    Main.main(new String[] {connectionPropertiesPath});

    assertTrue(outContent.toString().contains("usage: java -cp"));
    System.setOut(originalOut);
  }

  @Test
  void loadInvalidPropertyFileTest() throws Exception {
    String invalidPath = "noexist/connection.properties";
    URL csvLoaderDirUrl = getClass().getClassLoader().getResource("csvloader");
    String csvLoaderDirPath = Paths.get(csvLoaderDirUrl.toURI()).toString();

    assertThrows(
        UncheckedIOException.class,
        () -> main.execute(new String[] {invalidPath, csvLoaderDirPath}));
  }

  @Test
  void findTableDataResourcesIOExceptionTest() throws Exception {
    setupDatabase();
    URL connectionPropUrl = getClass().getClassLoader().getResource("connection.properties");
    String connectionPropertiesPath = Paths.get(connectionPropUrl.toURI()).toString();
    String invalidDirPath = "csvLoaderDirPath";

    assertThrows(
        UncheckedIOException.class,
        () -> {
          main.execute(new String[] {connectionPropertiesPath, invalidDirPath});
        });
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

  private void executeMain() throws Exception {
    URL connectionPropUrl = getClass().getClassLoader().getResource("connection.properties");
    String connectionPropertiesPath = Paths.get(connectionPropUrl.toURI()).toString();
    URL csvLoaderDirUrl = getClass().getClassLoader().getResource("csvloader");
    String csvLoaderDirPath = Paths.get(csvLoaderDirUrl.toURI()).toString();
    main.execute(new String[] {connectionPropertiesPath, csvLoaderDirPath});
  }

  private ResultSet executeSelectFromOrder() throws SQLException {
    String selectFromOrder =
        "SELECT * FROM \"ORDER\""
            .replace("\"", connection.getMetaData().getIdentifierQuoteString());
    return connection.createStatement().executeQuery(selectFromOrder);
  }

  private void assertRow(
      ResultSet rs,
      Integer id,
      String varcharCol,
      String dateCol,
      String timeCol,
      Boolean booleanCol)
      throws SQLException {
    assertEquals(id, rs.getInt("FROM"));
    assertEquals(varcharCol, rs.getString("COL_VARCHAR"));
    assertEquals(dateCol, rs.getString("COL_DATE"));
    assertEquals(timeCol, rs.getString("COL_TIME"));
    assertEquals(booleanCol, rs.getBoolean("COL_BOOLEAN"));
  }
}
