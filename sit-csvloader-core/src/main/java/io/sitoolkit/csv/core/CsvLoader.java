package io.sitoolkit.csv.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvLoader {

  private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.withSystemRecordSeparator()
      .withFirstRecordAsHeader();

  private CsvLoader() {
    // NOP
  }

  public static void load(Connection connection, Class<?> migrationClass, LogCallback log)
      throws IOException, SQLException {

    URL tableList = migrationClass.getResource(migrationClass.getSimpleName() + "/table-list.txt");
    log.info("Reading table list : " + tableList);

    List<String> tableNames = readLines(tableList);

    for (String tableName : tableNames) {
      TabbleMetaData metaData = extractMetaData(connection, tableName);

      URL csvFile = migrationClass.getResource(migrationClass.getSimpleName() + "/" + tableName + ".csv");

      log.info("Loading csv file : " + csvFile);

      try (CSVParser csvParser = CSVParser.parse(csvFile, StandardCharsets.UTF_8, DEFAULT_FORMAT)) {

        String insertStatement = buildInsertStatement(tableName, csvParser.getHeaderNames());

        executeStatement(connection, insertStatement, csvParser, metaData);
      }

    }
  }

  static TabbleMetaData extractMetaData(Connection connection, String tableName) throws SQLException {
    TabbleMetaData metaData = new TabbleMetaData();

    try (ResultSet rs = connection.getMetaData().getColumns(null, connection.getSchema(), tableName, "%")) {

      while (rs.next()) {
        metaData.addDataType(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE"));
      }
    }

    return metaData;
  }

  static List<String> readLines(URL resource) throws IOException {

    List<String> lines = new ArrayList<>();
    try (InputStream is = resource.openStream()) {
      try (Scanner scanner = new Scanner(is)) {
        while (scanner.hasNextLine()) {
          lines.add(scanner.nextLine());
        }
      }
    }

    return lines;
  }

  static String buildInsertStatement(String tableName, List<String> columnNames) {

    StringJoiner columns = new StringJoiner(",");
    StringJoiner values = new StringJoiner(",");

    columnNames.stream().peek(columns::add).forEachOrdered(r -> values.add("?"));

    return String.format("INSERT INTO %1s (%2s) VALUES (%3s)", tableName, columns.toString(), values.toString());
  }

  static void executeStatement(Connection connection, String statement, CSVParser csvParser, TabbleMetaData metaData)
      throws SQLException, IOException {
    try (PreparedStatement pstmt = connection.prepareStatement(statement)) {
      for (CSVRecord record : csvParser.getRecords()) {
        int i = 1;
        for (String columnName : csvParser.getHeaderNames()) {
          String cellValue = record.get(columnName);

          switch (metaData.getDataType(columnName)) {
            case Types.INTEGER:
              pstmt.setInt(i++, Integer.parseInt(cellValue));
              break;
            case Types.VARCHAR:
              pstmt.setString(i++, cellValue);
              break;
            default:
          }

        }
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    }
  }
}