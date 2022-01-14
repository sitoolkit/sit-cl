package io.sitoolkit.csv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CsvLoaderIT {
  private Connection connection;

  @BeforeEach
  void setup() throws Exception {
    Properties prop = new Properties();
    prop.load(getClass().getResourceAsStream("/connection.properties"));

    connection = DriverManager.getConnection(prop.getProperty("url"), prop.getProperty("user"),
        prop.getProperty("password"));

    String createTable = Files.readString(Path.of(getClass().getResource("CREATE_TABLE.sql").toURI()));
    String identifierQuoteString = connection.getMetaData().getIdentifierQuoteString();
    createTable = createTable.replace("\"", identifierQuoteString);
    connection.createStatement().executeUpdate(createTable);
  }

  @Test
  void loadTest() throws Exception {
    CsvLoader.load(connection, getClass(), (line) -> System.out.println(line));

    String selectFronOrder = "SELECT * FROM \"ORDER\"".replace("\"",
        connection.getMetaData().getIdentifierQuoteString());
    ResultSet rs = connection.createStatement().executeQuery(selectFronOrder);

    assertTrue(rs.next());

    assertEquals(1, rs.getInt("FROM"));
    assertEquals("one", rs.getString("COL_VARCHAR"));
    assertEquals("2020-12-29", rs.getString("COL_DATE"));
    assertEquals("12:30:00", rs.getString("COL_TIME"));
    assertEquals(true, rs.getBoolean("COL_BOOLEAN"));

    assertFalse(rs.next());
  }

  @Test
  void loadMultipleLocationTest() throws Exception {
    CsvLoader.load(connection, getClass(), (line) -> System.out.println(line), "one", "two");

    String selectFromOrder = "SELECT * FROM \"ORDER\"".replace("\"",
        connection.getMetaData().getIdentifierQuoteString());
    ResultSet rs = connection.createStatement().executeQuery(selectFromOrder);

    assertTrue(rs.next());

    assertEquals(1, rs.getInt("FROM"));
    assertEquals("one", rs.getString("COL_VARCHAR"));
    assertEquals("2020-12-29", rs.getString("COL_DATE"));
    assertEquals("12:30:00", rs.getString("COL_TIME"));
    assertEquals(true, rs.getBoolean("COL_BOOLEAN"));

    assertFalse(rs.next());

    String selectFromOrder2 = "SELECT * FROM \"ORDER_2\"".replace("\"",
        connection.getMetaData().getIdentifierQuoteString());
    rs = connection.createStatement().executeQuery(selectFromOrder2);

    assertTrue(rs.next());

    assertEquals(2, rs.getInt("FROM"));
    assertEquals("two", rs.getString("COL_VARCHAR"));
    assertEquals("2022-01-14", rs.getString("COL_DATE"));
    assertEquals("12:30:00", rs.getString("COL_TIME"));
    assertEquals(false, rs.getBoolean("COL_BOOLEAN"));

    assertFalse(rs.next());
  }
}
