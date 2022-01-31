package io.sitoolkit.csv.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResourceFinder {

  private ResourceFinder() {
    // NOP
  }

  public static URL findTableListUrl(Class<?> owner, List<String> resDirPaths)
      throws IOException {
    String versionName = owner.getSimpleName();
    if (resDirPaths.isEmpty()) {
      return owner.getResource(versionName + "/" + CsvLoader.TABLE_LIST_FILE_NAME);
    }

    List<URL> tableLists = resDirPaths.stream()
        .map(location -> owner.getResource(
            location + "/" + versionName + "/" + CsvLoader.TABLE_LIST_FILE_NAME))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (tableLists.isEmpty()) {
      throw new FileNotFoundException(
          "Not found " + CsvLoader.TABLE_LIST_FILE_NAME + " with version name "
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

}
