package io.sitoolkit.csv.app.domain.services;

import io.sitoolkit.csv.core.TableDataResource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceDataFinder {

  public List<TableDataResource> findTableDataResources(File tableListFile) {
    List<TableDataResource> tableDataResources = new ArrayList<>();
    searchTableListFiles(tableListFile, tableDataResources);
    return tableDataResources;
  }

  private void searchTableListFiles(
      File tableListFile, List<TableDataResource> tableDataResources) {
    File[] tableListFiles = tableListFile.listFiles();
    if (tableListFiles == null) return;
    Arrays.stream(tableListFiles)
        .forEach(
            tableList -> {
              try {
                if (tableList.isDirectory()) {
                  searchTableListFiles(tableList, tableDataResources);
                } else if ("table-list.txt".equals(tableList.getName())) {
                  tableDataResources.addAll(readTableDataResources(tableList));
                }
              } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException();
              }
            });
  }

  private List<TableDataResource> readTableDataResources(File tableList) throws IOException {
    return Files.readAllLines(tableList.toPath(), StandardCharsets.UTF_8).stream()
        .map(tablePath -> createTableDataResource(tableList, tablePath))
        .collect(Collectors.toList());
  }

  private TableDataResource createTableDataResource(File tableList, String tablePath) {
    String tableName = new File(tablePath).getName();
    try {
      URL csvUrl = new URL("file", null, tableList.getParent() + "/" + tablePath + ".csv");
      return new TableDataResource(tableName, csvUrl);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException();
    }
  }
}
