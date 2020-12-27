package io.sitoolkit.csv.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Types;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class CsvLoaderTests {


  @Test
  void extractMetaDataTest() throws Exception {

    Properties prop = new Properties();
    prop.load(getClass().getResourceAsStream("/connection.properties"));

    Connection connection = DriverManager
        .getConnection(prop.getProperty("url"), prop.getProperty("user"), prop.getProperty("password"));

    connection.createStatement().executeUpdate("CREATE TABLE TAB_1(COL_INT INT, COL_VARCHAR VARCHAR)");

    TabbleMetaData metaData = CsvLoader.extractMetaData(connection, "TAB_1");

    System.out.println(metaData);

    assertEquals(Types.INTEGER, metaData.getDataType("COL_INT"));
    assertEquals(Types.VARCHAR, metaData.getDataType("COL_VARCHAR"));
  }
}
