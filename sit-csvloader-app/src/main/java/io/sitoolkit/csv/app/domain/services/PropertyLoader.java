package io.sitoolkit.csv.app.domain.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertyLoader {

  public Properties loadProperties(String filePath) throws IOException {
    Properties props = new Properties();
    try (InputStream input = Files.newInputStream(Paths.get(filePath))) {
      props.load(input);
    }
    return props;
  }
}
