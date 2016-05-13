package com.github.biconou.subsonic;

import net.sourceforge.subsonic.service.MediaScannerService;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by remi on 07/05/2016.
 */
public class TestCaseUtils {


  public static void prepareDataBase(String baseResources) throws IOException {
    String baseDir = TestCaseUtils.class.getResource(baseResources).toString().replace("file:/","");
    String initDbDir = baseDir + "init_db";
    String dbDir = baseDir + "db";
    File dbDirectory = new File(dbDir);
    if (dbDirectory.exists()) {
      FileUtils.deleteDirectory(dbDirectory);
    }
    FileUtils.copyDirectory(new File(initDbDir),dbDirectory,true);

    // delete logs
    String[] filesToDelete = new String[]{"subsonic.log","subsonic.properties","cmus.log","mediaScanner.log"};
    Arrays.stream(filesToDelete).forEach(f -> {
      File file = new File(baseDir + f);
      if (file.exists()) {
        try {
          FileUtils.forceDelete(file);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

  }

  public static void setSubsonicHome(String baseResources) {
    String subsoncicHome = TestCaseUtils.class.getResource(baseResources).toString().replace("file:/","");
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
