package io.sitoolkit.csv.core;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
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

    String idenfifierQuateString = connection.getMetaData().getIdentifierQuoteString();

    for (String tableName : tableNames) {
      TabbleMetaData metaData = extractMetaData(connection, tableName, log);

      URL csvFile = migrationClass.getResource(migrationClass.getSimpleName() + "/" + tableName + ".csv");

      log.info("Loading csv file : " + csvFile);

      try (CSVParser csvParser = CSVParser.parse(csvFile, StandardCharsets.UTF_8, DEFAULT_FORMAT)) {

        String insertStatement = buildInsertStatement(tableName, csvParser.getHeaderNames(), idenfifierQuateString);

        executeStatement(connection, insertStatement, csvParser, metaData);
      }

    }
  }

  static TabbleMetaData extractMetaData(Connection connection, String tableName, LogCallback log) throws SQLException {
    TabbleMetaData metaData = new TabbleMetaData();

    try (ResultSet rs = connection.getMetaData().getColumns(null, connection.getSchema(), tableName, "%")) {

      while (rs.next()) {
        metaData.addDataType(
          rs.getString("COLUMN_NAME"), 
          metaData.new TypeDetail(
            rs.getInt("DATA_TYPE"),
            rs.getString("TYPE_NAME"))
        );
      }
    }

    log.info("Extracted " + tableName + " : " + metaData);
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

  static String buildInsertStatement(String tableName, List<String> columnNames, String idenfifierQuateString) {

    StringJoiner columns = new StringJoiner(",");
    StringJoiner values = new StringJoiner(",");

    columnNames.stream().map(columnName -> idenfifierQuateString + columnName + idenfifierQuateString)
        .peek(columns::add).forEachOrdered(r -> values.add("?"));

    return String.format("INSERT INTO %1$s%2$s%1$s (%3$s) VALUES (%4$s)", idenfifierQuateString, tableName,
        columns.toString(), values.toString());
  }

  static void executeStatement(Connection connection, String statement, CSVParser csvParser, TabbleMetaData metaData)
      throws SQLException {
    try (PreparedStatement pstmt = connection.prepareStatement(statement)) {

      Iterator<CSVRecord> itr = csvParser.iterator();

      while(itr.hasNext()) {

        CSVRecord record = itr.next();
        int i = 1;
        
        for (String columnName : csvParser.getHeaderNames()) {
          String cellValue = record.get(columnName);
          int columnIndex = i++;

          switch (metaData.getDataType(columnName)) {
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
              pstmt.setLong(columnIndex, Long.parseLong(cellValue));
              break;
            case Types.NUMERIC:
            case Types.DECIMAL:
              pstmt.setBigDecimal(columnIndex, new BigDecimal(cellValue));
              break;
            case Types.DOUBLE:
            case Types.FLOAT:
              pstmt.setDouble(columnIndex, Double.parseDouble(cellValue));
              break;
            case Types.DATE:
              pstmt.setDate(columnIndex, Date.valueOf(LocalDate.parse(cellValue)));
              break;
            case Types.TIMESTAMP:
              pstmt.setTimestamp(columnIndex, Timestamp.valueOf(LocalDateTime.parse(cellValue)));
              break;
            case Types.TIME:
              pstmt.setTime(columnIndex, Time.valueOf(LocalTime.parse(cellValue)));
              break;
            case Types.OTHER:
              if ("json".equals(metaData.getTypeName(columnName))) {
                pstmt.setObject(columnIndex, cellValue, Types.OTHER);
              } else {
                pstmt.setString(columnIndex, cellValue);
              }
              break;
            default:
              pstmt.setString(columnIndex, cellValue);
          }

        }
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    }
  }
}