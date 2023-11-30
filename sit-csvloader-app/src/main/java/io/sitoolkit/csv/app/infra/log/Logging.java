package io.sitoolkit.csv.app.infra.log;

import io.sitoolkit.csv.core.LogCallback;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Logging implements LogCallback {
  @Override
  public void info(String logMessage) {
    log.info(logMessage);
  }

  public void error(String errorMessage) {
    log.error(errorMessage);
  }
}
