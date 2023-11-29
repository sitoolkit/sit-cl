package io.sitoolkit.csv.app.domain.services;

import io.sitoolkit.csv.core.TableDataResource;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceDataFinder {

  public List<TableDataResource> findTableDataResources(String tableListFilePath)
      throws IOException {
    Path tableListPath = Paths.get(tableListFilePath);
    List<TableDataResource> tableDataResources = new ArrayList<>();
    searchTableListFiles(tableListPath, tableDataResources);
    return tableDataResources;
  }

  private void searchTableListFiles(Path tableListPath, List<TableDataResource> tableDataResources)
      throws IOException {
    try (Stream<Path> paths = Files.list(tableListPath)) {
      paths.forEach(tableList -> searchTableListFile(tableList, tableDataResources));
    }
  }

  private void searchTableListFile(Path tableList, List<TableDataResource> tableDataResources) {
    try {
      if (Files.isDirectory(tableList)) {
        searchTableListFiles(tableList, tableDataResources);
      } else if (tableList.getFileName().toString().equals("table-list.txt")) {
        tableDataResources.addAll(readTableDataResources(tableList));
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private List<TableDataResource> readTableDataResources(Path tableListPath) throws IOException {
    return Files.readAllLines(tableListPath).stream()
        .map(tablePath -> createTableDataResource(tableListPath, tablePath))
        .collect(Collectors.toList());
  }

  private TableDataResource createTableDataResource(Path tableListPath, String tablePath) {
    String tableName = Paths.get(tablePath).getFileName().toString();
    try {
      URL csvUrl = new URL("file", null, tableListPath.getParent() + "/" + tablePath + ".csv");
      return new TableDataResource(tableName, csvUrl);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
