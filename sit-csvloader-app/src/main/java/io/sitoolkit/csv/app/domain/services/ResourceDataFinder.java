package io.sitoolkit.csv.app.domain.services;

import io.sitoolkit.csv.core.TableDataResource;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceDataFinder {

  private static final String TABLE_LIST_FILE_NAME = "table-list.txt";

  public List<TableDataResource> findTableDataResources(String tableListFilePath)
      throws IOException {
    Path tableListPath = Paths.get(tableListFilePath);
    List<TableDataResource> tableDataResources = new ArrayList<>();

    try (Stream<Path> stream = Files.walk(tableListPath)) {
      stream
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().equals(TABLE_LIST_FILE_NAME))
          .forEach(tableList -> tableDataResources.addAll(readTableDataResources(tableList)));
    } catch (IOException e) {
      throw new IOException(e);
    }

    return tableDataResources;
  }

  private List<TableDataResource> readTableDataResources(Path tableListPath) {
    try {
      return Files.readAllLines(tableListPath).stream()
          .map(tablePath -> createTableDataResource(tableListPath.getParent(), tablePath))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Failed to read" + TABLE_LIST_FILE_NAME + " : " + tableListPath, e);
    }
  }

  private TableDataResource createTableDataResource(Path directory, String tablePath) {
    Path filePath = directory.resolve(tablePath + ".csv");
    try {
      URL csvUrl = filePath.toUri().toURL();
      String tableName = filePath.getFileName().toString().replace(".csv", "");
      return new TableDataResource(tableName, csvUrl);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Failed to create URL for CSV file: " + filePath, e);
    }
  }
}
