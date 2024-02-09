package io.sitoolkit.csv.app;

import io.sitoolkit.csv.app.domain.services.ExternalResourceFinder;
import io.sitoolkit.csv.app.domain.services.PropertyLoader;
import io.sitoolkit.csv.app.domain.services.SqlStatementExecutor;
import io.sitoolkit.csv.app.domain.webdriver.WebDriver;
import io.sitoolkit.csv.app.infra.log.Logging;
import io.sitoolkit.csv.core.CsvLoader;
import io.sitoolkit.csv.core.TableDataResource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class Main {

  private final PropertyLoader propertyLoader = new PropertyLoader();
  private final SqlStatementExecutor sqlFileExecutor = new SqlStatementExecutor();
  private final ExternalResourceFinder externalResourceFinder = new ExternalResourceFinder();
  private static final Logging log = new Logging();

  public static void main(String[] args) {
    if (args.length < 2) {
      log.error(
          "usage: java -cp \"target/lib/*;target/classes\" io.sitoolkit.csv.app.Main"
              + " <jdbcPropertyFilePath> <resourcesDirectoryPath>");
      return;
    }
    new Main().execute(args);
  }

  public void execute(String[] args) {
    String jdbcPropPath = args[0];
    String resDirPath = args[1];

    try {
      Properties connectionProps = propertyLoader.loadProperties(jdbcPropPath);
      try (Connection connection = new WebDriver().createDatabaseConnection(connectionProps)) {
        sqlFileExecutor.executeSqlStatement(connection, resDirPath);
        List<TableDataResource> tableDataResources =
            externalResourceFinder.findExternalTableDataResources(resDirPath);
        CsvLoader.load(connection, tableDataResources, log::info);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
