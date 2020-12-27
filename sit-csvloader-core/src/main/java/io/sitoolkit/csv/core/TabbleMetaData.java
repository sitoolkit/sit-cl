package io.sitoolkit.csv.core;

import java.util.HashMap;
import java.util.Map;

import lombok.ToString;

@ToString
public class TabbleMetaData {

  private Map<String, Integer> dataTypeMap = new HashMap<>();

  public int getDataType(String columnName) {
    return dataTypeMap.get(columnName);
  }

  public void addDataType(String columnName, int dataType) {
    dataTypeMap.put(columnName, dataType);
  }
}
