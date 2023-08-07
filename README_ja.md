[English](README.md)

# CSV Loader

CSV LoaderはCSVファイルをDBにロードするツールです。

CSV Loaderのv0.8時点での用途は[FlywayのJava-based Migration](https://flywaydb.org/documentation/concepts/migrations#java-based-migrations)に特化しています。次バージョン移行でjar単独実行、Maven Plugin等のサポートを予定しています。


## 必要なソフトウェア

- Java 11+
- Maven / Gradle

## 使用方法 (Flyway Java-based Migration)

ここではCSV LoaderをFlyway java-based Migration内で使用する方法を説明します。
まず、プロジェクトを[Flyway Maven Plugin](https://flywaydb.org/documentation/usage/maven/)が動作可能な状態で構成します。ディレクトリ構成は以下の様になります。

- ディレクトリ構成

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
          table-list.txt
          TABLE_1.csv
          TABLE_2.csv
```

ここではV1__CreateTables.sqlでテーブルを作成し、V2__AddRecords.javaでデータをCSVファイルからロードするとします。

- V1__CreateTables.sql

```sql
CREATE TABLE TABLE_1 (
  COLUMN_1 INT,
  COLUMN_2 VARCHAR(10)
);

:
```


次に、pom.xmlにCSV Loaderのdependencyを追加します。

- pom.xml

```xml
  <dependencies>
    <dependency>
      <groupId>io.sitoolkit.csv</groupId>
      <artifactId>sit-csvloader-flyway</artifactId>
      <version>0.9</version>
    </dependency>
  </dependencies>
```

次に、データをロードするためのMigrationクラスを作成します。このクラスではBaseJavaCsvMigrationを継承します。

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

table-list.txtには1バージョンの中でロードするテーブルのテーブル名を1行ずつ記載します。ロードは上から順に実行されます。

- table-list.txt

```
TABLE_1
TABLE_2
```

CSVファイルには1行目にカラム名を、2行目以降にロードするデータを記載します。  
[null]を設定すると、nullを登録します。

- TABLE_1.csv

```csv
COLUMN_1,COLUMN_2
1,One
2,Two
3,Three
4,[null]
```

以上の準備が終わったら、以下のコマンドでFlywayのMigrationを実行します。

```
mvn flyway:migrate
```

Migrationが正常に終了すると、TABLE_1にCSVのデータがロードされます。
