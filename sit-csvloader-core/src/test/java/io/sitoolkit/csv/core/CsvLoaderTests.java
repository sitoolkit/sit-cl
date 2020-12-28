package io.sitoolkit.csv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class CsvLoaderTests {

  @Test
  void loadTest() throws Exception {

    Properties prop = new Properties();
    prop.load(getClass().getResourceAsStream("/connection.properties"));

    Connection connection = DriverManager.getConnection(prop.getProperty("url"), prop.getProperty("user"),
        prop.getProperty("password"));

    connection.createStatement().executeUpdate("CREATE TABLE \"ORDER\" (\"FROM\" INT, COL_VARCHAR VARCHAR)");

    CsvLoader.load(connection, getClass(), (line) -> System.out.println(line));

    ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM \"ORDER\"");

    while (rs.next()) {
      assertEquals(1, rs.getInt("FROM"));
      assertEquals("one", rs.getString("COL_VARCHAR"));
    }
  }
}
