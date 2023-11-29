package io.sitoolkit.csv.app.interfaces;

import io.sitoolkit.csv.app.domain.services.PropertyLoader;
import io.sitoolkit.csv.app.domain.services.ResourceDataFinder;
import io.sitoolkit.csv.app.domain.services.SqlStatementExecutor;
import io.sitoolkit.csv.app.domain.webdriver.WebDriver;
import io.sitoolkit.csv.app.domain.webdriver.WebDriverImpl;
import io.sitoolkit.csv.app.infra.log.Logging;
import io.sitoolkit.csv.core.CsvLoader;
import io.sitoolkit.csv.core.TableDataResource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class Main {

  private final PropertyLoader propertyLoader = new PropertyLoader();
  private final SqlStatementExecutor sqlFileExecutor = new SqlStatementExecutor();
  private final ResourceDataFinder resourceDataFinder = new ResourceDataFinder();
  // Log log = LogFactory.getLog(getClass());
  private final Logging log = new Logging();

  public static void main(String[] args) {
    new Main().execute(args);
  }

  public void execute(String[] args) {
    if (args.length < 2) {
      throw new IllegalArgumentException("Missing arguments.");
    }

    String jdbcPropPath = args[0];
    String resDirPath = args[1];
    Connection connection = null;

    try {
      Properties connectionProps = propertyLoader.loadProperties(jdbcPropPath);
      WebDriver webDriver = new WebDriverImpl();
      connection = webDriver.createDatabaseConnection(connectionProps);

      sqlFileExecutor.executeSqlStatement(connection, resDirPath);

      List<TableDataResource> tableDataResources =
          resourceDataFinder.findTableDataResources(resDirPath);
      CsvLoader.load(connection, tableDataResources, log::info);
    } catch (SQLException | IOException e) {
      throw new IllegalArgumentException(e);
    } finally {
      closeConnection(connection);
    }
  }

  private void closeConnection(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
