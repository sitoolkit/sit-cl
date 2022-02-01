package io.sitoolkit.csv.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ResourceFinder {
  private static final String TABLE_LIST_FILE_NAME = "table-list.txt";

  private ResourceFinder() {
    // NOP
  }

  public static List<TableDataResource> findTableDataResources(Class<?> owner,
      List<String> resDirPaths) throws IOException {
    URL tableListUrl = findTableListUrl(owner, resDirPaths);
    Path tableListDirPath;
    try {
      tableListDirPath = Paths.get(tableListUrl.toURI()).getParent();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Could not parse table-list.txt URL", e);
    }
    List<String> tableNames = readTableList(tableListUrl);
    List<TableDataResource> tableDataResources = new ArrayList<>();
    for (String tableName : tableNames) {
      tableDataResources.add(buildTableDataResource(tableListDirPath, tableName));
    }
    return tableDataResources;
  }

  static URL findTableListUrl(Class<?> owner, List<String> resDirPaths) throws IOException {
    String versionName = owner.getSimpleName();
    if (resDirPaths.isEmpty()) {
      return owner.getResource(versionName + "/" + TABLE_LIST_FILE_NAME);
    }

    List<URL> tableLists = resDirPaths.stream()
        .map(location -> owner.getResource(
            location + "/" + versionName + "/" + TABLE_LIST_FILE_NAME))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (tableLists.isEmpty()) {
      throw new FileNotFoundException(
          "Not found " + TABLE_LIST_FILE_NAME + " with version name "
              + versionName);
    } else if (tableLists.size() > 1) {
      throwMultipleTableListException(versionName, tableLists);
    }
    return tableLists.get(0);
  }

  static void throwMultipleTableListException(String versionName, List<URL> tableListUrls)
      throws IOException {
    StringBuilder sb = new StringBuilder(
        "Found more than one tableList with version name " + versionName + "\nFiles:\n");
    for (URL tableList : tableListUrls) {
      sb.append("-> ").append(tableList).append('\n');
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

  static TableDataResource buildTableDataResource(Path tableListDirPath, String tableName)
      throws IOException {
    Path csvPath = tableListDirPath.resolve(tableName + ".csv");
    if (!Files.exists(csvPath)) {
      throw new FileNotFoundException("Not found csv file\nExpected Path:\n-> " + csvPath);
    }
    return new TableDataResource(tableName, csvPath);
  }

}
