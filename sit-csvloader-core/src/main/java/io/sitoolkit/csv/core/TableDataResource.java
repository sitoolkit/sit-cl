package io.sitoolkit.csv.core;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDataResource {
  private String tableName;
  private Path csvPath;
}
