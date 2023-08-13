package io.sitoolkit.csv.flyway;

import io.sitoolkit.csv.core.CsvLoader;
import io.sitoolkit.csv.core.LogCallback;
import io.sitoolkit.csv.core.ResourceFinder;
import io.sitoolkit.csv.core.TableDataResource;
import java.sql.Connection;
import java.util.List;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class BaseJavaCsvMigrationTest {

    private static final List<String> RES_DIR_PATHS = List.of("/classes", "/files");

    private BaseJavaCsvMigration migration;

    @BeforeEach
    void setUp() {
        this.migration = new V0__1_MigrationTest();
    }

    @Test
    void migrateTest() throws Exception {
        try (var resourceFinderMockedStatic = Mockito.mockStatic(ResourceFinder.class)) {

            try (var csvLoaderMockedStatic = Mockito.mockStatic(CsvLoader.class)) {
                Context context = Mockito.mock(Context.class, Mockito.RETURNS_DEEP_STUBS);
                Connection connection = Mockito.mock(Connection.class, Mockito.RETURNS_DEEP_STUBS);
                Mockito.when(context.getConnection()).thenReturn(connection);

                mockLocations(context);

                List<TableDataResource> tableDataResources = List.of(new TableDataResource());

                resourceFinderMockedStatic.when(() -> ResourceFinder.findTableDataResources(
                        ArgumentMatchers.eq(V0__1_MigrationTest.class),
                        ArgumentMatchers.eq(RES_DIR_PATHS),
                        ArgumentMatchers.any(LogCallback.class))
                ).thenReturn(tableDataResources);

                migration.migrate(context);

                csvLoaderMockedStatic.verify(() -> CsvLoader.load(
                        ArgumentMatchers.eq(connection),
                        ArgumentMatchers.eq(tableDataResources),
                        ArgumentMatchers.any(LogCallback.class)
                ));
            }
        }
    }

    private static void mockLocations(Context context) {
        Location[] locations = new Location[2];
        Location classPathLocation = Mockito.mock(Location.class);
        Mockito.when(classPathLocation.isClassPath()).thenReturn(true);
        Mockito.when(classPathLocation.getPath()).thenReturn(RES_DIR_PATHS.get(0).substring(1));
        locations[0] = classPathLocation;
        Location fileSystemLocation = Mockito.mock(Location.class);
        Mockito.when(fileSystemLocation.isClassPath()).thenReturn(false);
        Mockito.when(fileSystemLocation.getPath()).thenReturn(RES_DIR_PATHS.get(1));
        locations[1] = fileSystemLocation;
        Mockito.when(context.getConfiguration().getLocations()).thenReturn(locations);
    }

    static class V0__1_MigrationTest extends BaseJavaCsvMigration {

    }
}