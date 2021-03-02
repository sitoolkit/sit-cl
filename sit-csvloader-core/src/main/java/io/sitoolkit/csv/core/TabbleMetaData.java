package io.sitoolkit.csv.core;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@ToString
public class TabbleMetaData {

  private Map<String, TypeDetail> dataTypeMap = new HashMap<>();

  public int getDataType(String columnName) {
    return dataTypeMap.get(columnName).getDataType();
  }

  public String getTypeName(String columnName) {
    return dataTypeMap.get(columnName).getTypeName();
  }

  public void addDataType(String columnName, TypeDetail dataTypeDetail) {
    dataTypeMap.put(columnName, dataTypeDetail);
  }

  @AllArgsConstructor
  @Data
  public class TypeDetail {
    private int dataType;
    private String typeName;
  }
}
