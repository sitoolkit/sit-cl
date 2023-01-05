[English](README.md)

# CSV Loader

CSV Loader is a tool that loads CSV files into the DB.

As of v0. 8, CSV Loader will be used for [Java-based Migration of Flyway] (It is specialized in https://flywaydb.org/documentation/concepts/migrations#java-based-migrations). In the next version migration, we are planning to support jar independent execution, Maven Plugin, etc.



## Required Software

- Java 11+
- Maven / Gradle

## How to use (Flyway Java-based Migration)

This section describes how to use the CSV Loader within Flyway java-based Migration.
First, the project is configured with the https://flywaydb.org/documentation/usage/maven/) Maven Plugin (The directory configuration is as follows:

- Directory Configuration

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
      <artifactId>sit-csvloader-flyway</artifactId>
      <version>0.8</version>
    </dependency>
  </dependences>
```


Next, you create a data-loading Migration class that inherits from BaseJavaCsvMigration.

- V2__AddRecords.java

```java

package db.migration;

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import io.sitoolkit.csv.flyway;

@SuppressWarnings("squid:S101")
public class V2__AddRecords extends BaseJavaCsvMigration {  
}
```

The table-list. txt file contains the names of the tables to be loaded in one version, one row at a time. Loading is performed from top to bottom.

- table-list.txt

```
TABLE_1
TABLE_2
```


The first line of the CSV file contains the column names, and the second and subsequent lines contain the data to be loaded.
Set to null to register a null.

- TABLE_1.csv

```csv
COLUMN_1,COLUMN_2
1,One
2,Two
3,Three
4,[null]
```


After these preparations, run the Flyway Migration with the following command:

```
mvn flyway:migrate
```


If Migration is successful, CSV data is loaded into TABLE _ 1.