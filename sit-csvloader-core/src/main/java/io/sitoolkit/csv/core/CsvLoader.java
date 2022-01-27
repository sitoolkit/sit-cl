package io.sitoolkit.csv.core;

import java.io.FileNotFoundException;
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

  private static final String TABLE_LIST_FILE_NAME = "table-list.txt";
  private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.withSystemRecordSeparator()
      .withFirstRecordAsHeader();

  private CsvLoader() {
    // NOP
  }

  public static void load(
      Connection connection, Class<?> migrationClass, LogCallback log, String... locations)
      throws IOException, SQLException {
    String tableListDirPath = searchTableListDirPath(migrationClass, locations);
    URL tableListUrl = migrationClass.getResource(tableListDirPath + "/" + TABLE_LIST_FILE_NAME);

    log.info("Reading table list : " + tableListUrl);

    List<String> tableNames = readLines(tableListUrl);
    String identifierQuoteString = connection.getMetaData().getIdentifierQuoteString();

    for (String tableName : tableNames) {
      TabbleMetaData metaData = extractMetaData(connection, tableName, log);
      URL csvFile = migrationClass.getResource(tableListDirPath + "/" + tableName + ".csv");
      log.info("Loading csv file : " + csvFile);

      try (CSVParser csvParser = CSVParser.parse(csvFile, StandardCharsets.UTF_8, DEFAULT_FORMAT)) {
        String insertStatement = buildInsertStatement(tableName, csvParser.getHeaderNames(), identifierQuoteString);

        executeStatement(connection, insertStatement, csvParser, metaData);
      }
    }
  }

  static String searchTableListDirPath(Class<?> migrationClass, String... locations)
      throws IOException {
    String versionName = migrationClass.getSimpleName();
    if (locations.length == 0) {
      return versionName;
    }

    String tableListDirPath = null;

    for (String location : locations) {
      String resourceDir = location + "/" + versionName;
      URL tableListUrl = migrationClass.getResource(resourceDir + "/" + TABLE_LIST_FILE_NAME);
      if (tableListUrl == null) {
        continue;
      }
      if (tableListDirPath != null) {
        throwMultipleTableListException(versionName, tableListDirPath, resourceDir);
      }
      tableListDirPath = resourceDir;
    }

    if (tableListDirPath == null) {
      throw new FileNotFoundException(
          "Not found " + TABLE_LIST_FILE_NAME + " with version name " + versionName);
    }

    return tableListDirPath;
  }

  static void throwMultipleTableListException(String versionName, String... tableListDirPaths)
      throws IOException {
    StringBuilder sb = new StringBuilder(
        "Found more than one tableList with version name " + versionName + "\nFiles:\n");
    for (String tableList : tableListDirPaths) {
      sb.append("-> ").append(tableList).append("/").append(TABLE_LIST_FILE_NAME).append('\n');
    }
    throw new IOException(sb.toString());
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

    for (String columnName : columnNames) {
      String r = idenfifierQuateString + columnName + idenfifierQuateString;
      columns.add(r);
      values.add("?");
    }

    return String.format("INSERT INTO %1$s%2$s%1$s (%3$s) VALUES (%4$s)", idenfifierQuateString, tableName,
        columns.toString(), values.toString());
  }

  static void executeStatement(Connection connection, String statement, CSVParser csvParser, TabbleMetaData metaData)
      throws SQLException {
    try (PreparedStatement pstmt = connection.prepareStatement(statement)) {

      Iterator<CSVRecord> itr = csvParser.iterator();

      while(itr.hasNext()) {

        CSVRecord csvRecord = itr.next();
        int i = 1;
        
        for (String columnName : csvParser.getHeaderNames()) {
          String cellValue = csvRecord.get(columnName);
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
            case Types.BINARY:
              pstmt.setBytes(columnIndex, cellValue.getBytes());
              break;
            case Types.BOOLEAN:
            case Types.BIT:
              pstmt.setBoolean(columnIndex, Boolean.valueOf(cellValue));
              break;
            case Types.OTHER:
              if (isPgJsonColumn(connection.getMetaData().getDatabaseProductName(),
                  metaData.getTypeName(columnName))) {
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

  static boolean isPgJsonColumn(String databaseName, String columnTypeName) {
    return ("PostgreSQL".equalsIgnoreCase(databaseName)
        && ("json".equalsIgnoreCase(columnTypeName) || "jsonb".equalsIgnoreCase(columnTypeName)));
  }

}
