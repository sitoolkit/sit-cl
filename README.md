[日本語](README_ja.md)

# CSV Loader

The CSV Loader is a tool for loading CSV files into the DB.

As of v0.8, the CSV Loader will be used for "Flyway Java-based Migration" (specialized for https://flywaydb.org/documentation/concepts/migrations#java-based-migrations).



## Required Software

- Java 11+
- Maven / Gradle

## Usage (Flyway Java-based Migration)

This section describes how to use the CSV Loader within Flyway java-based Migration.
First, configure the project with the Flyway Maven Plugin (with the https://flywaydb.org/documentation/usage/maven/) operational.

- Directory configuration

```
project
  pom.xml
  src/main
    java
      db/migration
        V2__AddRecords.java
    resources
      db/migration
        V1__CreateTables.sql
        V2__AddRecords
          tabe-list.txt
          TABLE_1.csv
          TABLE_2.csv
```


Suppose you create a table in V1 _ _ CreateTables. sql and load it from a CSV file in V2 _ _ AddRecords. java.

- V1__CreateTables.sql

```sql
CREATE TABLE TABLE_1 (
  COLUMN_1 INT,
  COLUMN_2 VARCHAR(10)
);

:
```



Next, add the CSV Loader dependency to pom. xml.

- pom.xml

```xml
  <dependencies>
    <dependency>
      <groupId>io.sitoolkit.csv</groupId>
      <artifactId>sit-csvloader-core</artifactId>
      <version>0.8</version>
    </dependency>
  </dependences>
```


Next, create a Migration class to load the data-inherit BaseJavaMigration and run CsvLoader. load in migrate.

- V2__AddRecords.java

```java

package db.migration;

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import io.sitoolkit.csv.core.CsvLoader;

@SuppressWarnings("squid:S101")
public class V2__AddRecords extends BaseJavaMigration {

private final Log log = LogFactory.getLog(V2__AddRecords.class);

@Override
public void migrate(Context ctx) throws Exception {
CsvLoader.load(ctx.getConnection(), getClass(), log::info);
}
  
}
```

The table-list. txt contains the table names of the tables to be loaded in one version, one row at a time, from top to bottom.

- table-list.txt

```
TABLE_1
TABLE_2
```


The CSV file contains the column names on the first line and the data to be loaded from the second line.

- TABLE_1.csv

```csv
COLUMN_1,COLUMN_2
1,One
2,Two
3,Three
```


Once this is done, run Flyway Migration with the following command :

```
mvn flyway:migrate
```


If the migration is successful, CSV data is loaded into TABLE _ 1.