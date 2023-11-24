package io.sitoolkit.csv.app.domain.webdriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class WebDriverImpl implements WebDriver {

  @Override
  public Connection createDatabaseConnection(Properties props) throws SQLException {
    String url = props.getProperty("url");
    String user = props.getProperty("user");
    String password = props.getProperty("password");
    return DriverManager.getConnection(url, user, password);
  }
}
