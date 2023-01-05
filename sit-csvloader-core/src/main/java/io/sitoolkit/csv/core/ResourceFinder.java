package io.sitoolkit.csv.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;

public class ResourceFinder {
  private static final String TABLE_LIST_FILE_NAME = "table-list.txt";

  private ResourceFinder() {
    // NOP
  }

  public static List<TableDataResource> findTableDataResources(
      Class<?> owner, List<String> resDirPaths, LogCallback log) throws IOException {
    TableListResource tableListResource = findTableListResource(owner, resDirPaths);
    log.info("Reading table list : " + tableListResource.getTableListUrl());
    List<String> tableNames = readTableList(tableListResource.getTableListUrl());
    List<TableDataResource> tableDataResources = new ArrayList<>();
    for (String tableName : tableNames) {
      tableDataResources.add(
          buildTableDataResource(owner, tableListResource.getTableListDir(), tableName));
    }
    return tableDataResources;
  }

  static TableListResource findTableListResource(Class<?> owner, List<String> resDirPaths)
      throws IOException {
    String versionName = owner.getSimpleName();
    if (resDirPaths.isEmpty()) {
      return new TableListResource(
          owner.getResource(versionName + "/" + TABLE_LIST_FILE_NAME), versionName);
    }

    List<TableListResource> tableLists =
        resDirPaths.stream()
            .map(
                location ->
                    new TableListResource(
                        owner.getResource(
                            location + "/" + versionName + "/" + TABLE_LIST_FILE_NAME),
                        location + "/" + versionName))
            .filter(item -> item.getTableListUrl() != null)
            .collect(Collectors.toList());

    if (tableLists.isEmpty()) {
      throw new FileNotFoundException(
          "Not found " + TABLE_LIST_FILE_NAME + " with version name " + versionName);
    } else if (tableLists.size() > 1) {
      throwMultipleTableListException(versionName, tableLists);
    }
    return tableLists.get(0);
  }

  static void throwMultipleTableListException(
      String versionName, List<TableListResource> tableListResourceList) throws IOException {
    StringBuilder sb =
        new StringBuilder(
            "Found more than one tableList with version name " + versionName + "\nFiles:\n");
    for (TableListResource tableListResource : tableListResourceList) {
      sb.append("-> ").append(tableListResource.getTableListUrl()).append('\n');
    }
    throw new IOException(sb.toString());
  }

  static List<String> readTableList(URL resource) throws IOException {
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

  static TableDataResource buildTableDataResource(
      Class<?> owner, String tableListDirPath, String tableName) throws IOException {
    URL csvUrl = owner.getResource(tableListDirPath + "/" + tableName + ".csv");
    if (csvUrl == null) {
      throw new FileNotFoundException(
          "Not found csv file\nExpected Path:\n-> " + tableListDirPath + "/" + tableName + ".csv");
    }
    return new TableDataResource(tableName, csvUrl);
  }

  @Data
  @AllArgsConstructor
  private static class TableListResource {
    private URL tableListUrl;

    private String tableListDir;
  }
}
