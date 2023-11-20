package io.sitoolkit.csv.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

  public static void main(String[] args) {
    new Main().execute(args);
  }

  public void execute(String[] args) {
    if (args.length < 2) {
      return;
    }

    String connectionPropertiesPath = args[0];
    String resourceDirectoryPath = args[1];

    Properties connectionProps;
    try {
      connectionProps = loadProperties(connectionPropertiesPath);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    try (Connection connection = createDatabaseConnection(connectionProps)) {
      processResources(connection, resourceDirectoryPath);
    } catch (SQLException | IOException e) {
      e.printStackTrace();
    }
  }

  private Properties loadProperties(String filePath) throws IOException {
    Properties props = new Properties();
    try (FileInputStream input = new FileInputStream(filePath)) {
      props.load(input);
    }
    return props;
  }

  private Connection createDatabaseConnection(Properties props) throws SQLException {
    String url = props.getProperty("url");
    String user = props.getProperty("user");
    String password = props.getProperty("password");
    return DriverManager.getConnection(url, user, password);
  }

  private void processResources(Connection connection, String resourceDirectoryPath)
      throws IOException, SQLException {
    File dir = new File(resourceDirectoryPath);
    File[] files = dir.listFiles();

    if (files == null) {
      return;
    }

    for (File file : files) {
      if (file.getName().equals("table-list.txt")) {
        processTableList(connection, file);
      }
    }
  }

  private void processTableList(Connection connection, File tableListFile)
      throws IOException, SQLException {
    try (BufferedReader reader = new BufferedReader(new FileReader(tableListFile))) {
      String tableName;
      while ((tableName = reader.readLine()) != null) {
        String csvFilePath = tableListFile.getParent() + "/" + tableName + ".csv";
        File csvFile = new File(csvFilePath);
        if (csvFile.exists()) {
          processCsvFile(connection, csvFile, tableName);
        }
      }
    }
  }

  private void processCsvFile(Connection connection, File csvFile, String tableName)
      throws IOException, SQLException {
    LogCallback log = System.out::println;
    List<TableDataResource> resources = new ArrayList<>();
    resources.add(new TableDataResource(tableName, csvFile.toURI().toURL()));
    CsvLoader.load(connection, resources, log);
  }
}
