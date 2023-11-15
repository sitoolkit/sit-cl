package io.sitoolkit.csv.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

  public static void main(String[] args) {
    new Main().execute();
  }

  public void execute() {
    Properties props = loadProperties();
    try (Connection connection = createDatabaseConnection(props)) {
      String dbType = getDatabaseType(props.getProperty("url"));
      executeSqlScript(connection, dbType);
      CsvLoader.load(connection, loadTableDataResources(), log -> System.out.println(log));
    } catch (SQLException | IOException e) {
      e.printStackTrace();
    }
  }

  private Properties loadProperties() {
    Properties props = new Properties();
    try (InputStream input =
        getClass().getClassLoader().getResourceAsStream("connection.properties")) {
      if (input == null) {
        throw new IOException("Property file connection.properties is not found.");
      }
      props.load(input);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return props;
  }

  private Connection createDatabaseConnection(Properties props) throws SQLException {
    String url = props.getProperty("url");
    String user = props.getProperty("user");
    String password = props.getProperty("password");
    return DriverManager.getConnection(url, user, password);
  }

  private void executeSqlScript(Connection connection, String dbType)
      throws IOException, SQLException {
    String scriptToExecute = selectScriptForDatabase(dbType);
    try (Statement statement = connection.createStatement()) {
      statement.execute(scriptToExecute);
    }
  }

  private String getDatabaseType(String url) {
    if (url.contains("mysql")) {
      return "mysql";
    } else if (url.contains("postgresql")) {
      return "postgres";
    } else {
      return "h2";
    }
  }

  private String selectScriptForDatabase(String dbType) throws IOException {
    String sqlFileName = dbType.equals("mysql") ? "CREATE_TABLE_MySQL.sql" : "CREATE_TABLE.sql";
    try (InputStream sqlScript = getClass().getClassLoader().getResourceAsStream(sqlFileName)) {
      if (sqlScript == null) {
        throw new IOException("SQL script file '" + sqlFileName + "' is not found.");
      }
      return new String(sqlScript.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private List<TableDataResource> loadTableDataResources() throws IOException {
    List<String> mainResourcePath = Arrays.asList("/");
    List<String> testResourcePaths =
        Arrays.asList(
            "CsvLoaderIT/table-list.txt",
            "one/CsvLoaderIT/table-list.txt",
            "three/CsvLoaderIT/table-list.txt");

    URL mainResourceUrl = getClass().getClassLoader().getResource("table-list.txt");
    if (mainResourceUrl != null) {
      return ResourceFinder.findTableDataResources(
          getClass(), mainResourcePath, log -> System.out.println(log));
    } else {
      return testResourcePaths.stream()
          .flatMap(
              path -> {
                try {
                  return loadResourcesFromPath(path).stream();
                } catch (IOException e) {
                  e.printStackTrace();
                  return Stream.empty();
                }
              })
          .collect(Collectors.toList());
    }
  }

  private List<TableDataResource> loadResourcesFromPath(String path) throws IOException {
    InputStream tableListStream = getClass().getClassLoader().getResourceAsStream(path);
    if (tableListStream == null) {
      return Collections.emptyList();
    }

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(tableListStream))) {
      return reader
          .lines()
          .map(tableName -> createTableDataResource(path, tableName))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
  }

  private TableDataResource createTableDataResource(String path, String tableName) {
    String csvFilePath = path.substring(0, path.lastIndexOf('/')) + "/" + tableName + ".csv";
    URL csvUrl = getClass().getClassLoader().getResource(csvFilePath);
    if (csvUrl == null) {
      return null;
    }
    String baseTableName =
        tableName.contains("/") ? tableName.substring(tableName.lastIndexOf("/") + 1) : tableName;
    return new TableDataResource(baseTableName, csvUrl);
  }
}
