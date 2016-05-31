package com.github.biconou.subsonic;

import net.sourceforge.subsonic.dao.DaoHelper;
import net.sourceforge.subsonic.service.MediaScannerService;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by remi on 07/05/2016.
 */
public class TestCaseUtils {

  public static Map<String, Integer> recordsInAllTables(DaoHelper daoHelper) {
    List<String> tableNames = daoHelper.getJdbcTemplate().queryForList("" +
                    "select table_name " +
                    "from information_schema.system_tables " +
                    "where table_name not like 'SYSTEM%'"
            , String.class);
    Map<String, Integer> nbRecords =
            tableNames.stream()
                    .collect(Collectors.toMap(table -> table, table -> recordsInTable(table,daoHelper)));

    return nbRecords;
  }

  public static Integer recordsInTable(String tableName, DaoHelper daoHelper) {
    return daoHelper.getJdbcTemplate().queryForInt("select count(1) from " + tableName);
  }


  private static String basePath(String baseResources) {
    String basePath = TestCaseUtils.class.getResource(baseResources).toString();
    if (basePath.startsWith("file:")) {
      return TestCaseUtils.class.getResource(baseResources).toString().replace("file:","");
    }
    return basePath;
  }

  public static void prepareDataBase(String baseResources) throws IOException {
    String baseDir = basePath(baseResources);
    String initDbDir = baseDir + "init_db";
    String dbDir = baseDir + "db";
    File dbDirectory = new File(dbDir);
    if (dbDirectory.exists()) {
      FileUtils.deleteDirectory(dbDirectory);
    }
    FileUtils.copyDirectory(new File(initDbDir),dbDirectory,true);

    // delete logs
   /* String[] filesToDelete = new String[]{"subsonic.log","subsonic.properties","cmus.log","mediaScanner.log"};
    Arrays.stream(filesToDelete).forEach(f -> {
      File file = new File(baseDir + f);
      if (file.exists()) {
        try {
          FileUtils.forceDelete(file);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }); */

  }

  public static void setSubsonicHome(String baseResources) {
    String subsoncicHome = basePath(baseResources);
    System.setProperty("subsonic.home",subsoncicHome);
  }

  public static ApplicationContext loadSpringApplicationContext(String baseResources) {
    String applicationContextService = baseResources + "applicationContext-service.xml";
    String applicationContextCache = baseResources + "applicationContext-cache.xml";

    String[] configLocations = new String[]{
            TestCaseUtils.class.getClass().getResource(applicationContextCache).toString(),
            TestCaseUtils.class.getClass().getResource(applicationContextService).toString()
    };
    return new ClassPathXmlApplicationContext(configLocations);
  }


  public static void execScan(MediaScannerService mediaScannerService) {
    mediaScannerService.scanLibrary();

    while (mediaScannerService.isScanning()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

}
