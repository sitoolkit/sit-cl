package io.sitoolkit.csv.core;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class TabbleMetaData {

  private final String tableName;

  private Map<String, TypeDetail> dataTypeMap = new HashMap<>();

  public int getDataType(String columnName) {

    TypeDetail type = dataTypeMap.get(columnName);
    if (type == null) {
      throw new IllegalArgumentException(
          "Column:" + columnName + " does not exist in table:" + tableName);
    }
    return type.getDataType();
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
