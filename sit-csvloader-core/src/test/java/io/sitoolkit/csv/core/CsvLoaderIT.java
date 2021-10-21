package io.sitoolkit.csv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class CsvLoaderIT {

  @Test
  void loadTest() throws Exception {

    Properties prop = new Properties();
    prop.load(getClass().getResourceAsStream("/connection.properties"));

    Connection connection = DriverManager.getConnection(prop.getProperty("url"), prop.getProperty("user"),
        prop.getProperty("password"));

    String createTable = Files.readString(Path.of(getClass().getResource("CREATE_TABLE.sql").toURI()));
    String idenfifierQuateString = connection.getMetaData().getIdentifierQuoteString();
    createTable = createTable.replace("\"", idenfifierQuateString);
    connection.createStatement().executeUpdate(createTable);

    CsvLoader.load(connection, getClass(), (line) -> System.out.println(line));

    String selectFronOrder = "SELECT * FROM \"ORDER\"".replace("\"", connection.getMetaData().getIdentifierQuoteString());
    ResultSet rs = connection.createStatement().executeQuery(selectFronOrder);

    while (rs.next()) {
      assertEquals(1, rs.getInt("FROM"));
      assertEquals("one", rs.getString("COL_VARCHAR"));
      assertEquals("2020-12-29", rs.getString("COL_DATE"));
      assertEquals("12:30:00", rs.getString("COL_TIME"));
      assertEquals(true, rs.getBoolean("COL_BOOLEAN"));
    }
  }
}
