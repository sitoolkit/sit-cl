package io.sitoolkit.csv.flyway;

import io.sitoolkit.csv.core.CsvLoader;
import io.sitoolkit.csv.core.ResourceFinder;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class BaseJavaCsvMigration extends BaseJavaMigration {

  @Override
  public void migrate(Context ctx) throws Exception {
    Log log = LogFactory.getLog(getClass());
    List<String> resDirPaths = Arrays.stream(ctx.getConfiguration().getLocations())
        .map(BaseJavaCsvMigration::prefixLocation).collect(Collectors.toList());
    URL tableListUrl = ResourceFinder.findTableListUrl(getClass(), resDirPaths);
    CsvLoader.load(ctx.getConnection(), tableListUrl, log::info);
  }

  static String prefixLocation(Location location) {
    if (location.isClassPath()) {
      return "/" + location.getPath();
    }

    return location.getPath();
  }
}
