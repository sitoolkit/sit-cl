package io.sitoolkit.csv.app.interfaces;

import io.sitoolkit.csv.app.domain.services.PropertyLoader;
import io.sitoolkit.csv.app.domain.services.ResourceDataFinder;
import io.sitoolkit.csv.app.domain.services.SqlStatementExecutor;
import io.sitoolkit.csv.app.domain.webdriver.WebDriver;
import io.sitoolkit.csv.app.domain.webdriver.WebDriverImpl;
import io.sitoolkit.csv.core.CsvLoader;
import io.sitoolkit.csv.core.TableDataResource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;

public class Main {

  private final PropertyLoader propertyLoader = new PropertyLoader();
  private final SqlStatementExecutor sqlFileExecutor = new SqlStatementExecutor();
  private final ResourceDataFinder resourceDataFinder = new ResourceDataFinder();

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
    Log log = LogFactory.getLog(getClass());

    try {
      Properties connectionProps = propertyLoader.loadProperties(jdbcPropPath);
      WebDriver webDriver = new WebDriverImpl();
      connection = webDriver.createDatabaseConnection(connectionProps);

      sqlFileExecutor.executeSqlStatement(connection, new File(resDirPath));

      List<TableDataResource> tableDataResources =
          resourceDataFinder.findTableDataResources(new File(resDirPath));
      CsvLoader.load(
          connection,
          tableDataResources,
          message -> {
            log.info(message);
            System.out.println(message);
          });
    } catch (SQLException | IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException();
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
