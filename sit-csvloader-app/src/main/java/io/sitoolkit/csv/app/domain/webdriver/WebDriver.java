package io.sitoolkit.csv.app.domain.webdriver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public interface WebDriver {
  Connection createDatabaseConnection(Properties props) throws SQLException;
}
