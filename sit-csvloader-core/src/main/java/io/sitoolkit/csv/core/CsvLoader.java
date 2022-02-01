package io.sitoolkit.csv.core;

import java.io.IOException;
import java.math.BigDecimal;
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
import java.util.StringJoiner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvLoader {

  private static final CSVFormat DEFAULT_FORMAT =
      CSVFormat.DEFAULT.withSystemRecordSeparator().withFirstRecordAsHeader();

  private CsvLoader() {
    // NOP
  }

  public static void load(Connection connection, Class<?> migrationClass, LogCallback log)
      throws IOException, SQLException {
    List<TableDataResource> resources =
        ResourceFinder.findTableDataResources(migrationClass, new ArrayList<>());
    load(connection, resources, log);
  }

  public static void load(Connection connection, List<TableDataResource> tableDataResources,
      LogCallback log) throws IOException, SQLException {
    String identifierQuoteString = connection.getMetaData().getIdentifierQuoteString();

    for (TableDataResource tableDataResource : tableDataResources) {
      TabbleMetaData metaData = extractMetaData(connection, tableDataResource.getTableName(), log);

      try (CSVParser csvParser =
          CSVParser.parse(tableDataResource.getCsvPath(), StandardCharsets.UTF_8, DEFAULT_FORMAT)) {
        String insertStatement = buildInsertStatement(tableDataResource.getTableName(),
            csvParser.getHeaderNames(), identifierQuoteString);

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
