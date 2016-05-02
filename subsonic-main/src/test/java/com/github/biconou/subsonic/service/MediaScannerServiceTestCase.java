package com.github.biconou.subsonic.service;

import com.github.biconou.dao.ElasticSearchClient;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.service.*;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Created by remi on 01/05/2016.
 */
public class MediaScannerServiceTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/";

  private MediaScannerService mediaScannerService = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Prepare database
    String baseDir = this.getClass().getResource(baseResources).toString().replace("file:/","");
    String initDbDir = baseDir + "init_db";
    String dbDir = baseDir + "db";
    File dbDirectory = new File(dbDir);
    if (dbDirectory.exists()) {
      FileUtils.deleteDirectory(dbDirectory);
    }
    FileUtils.copyDirectory(new File(initDbDir),dbDirectory,true);

    // delete logs
    FileUtils.forceDelete(new File(baseDir + "subsonic.log"));
    FileUtils.forceDelete(new File(baseDir + "subsonic.properties"));
    FileUtils.forceDelete(new File(baseDir + "cmus.log"));
    FileUtils.forceDelete(new File(baseDir + "mediaScanner.log"));

    // load spring context
    String applicationContextService = baseResources + "applicationContext-service.xml";
    String applicationContextCache = baseResources + "applicationContext-cache.xml";

    String subsoncicHome = this.getClass().getResource(baseResources).toString().replace("file:/","");
    System.setProperty("subsonic.home",subsoncicHome);

    String[] configLocations = new String[]{
            this.getClass().getResource(applicationContextCache).toString(),
            this.getClass().getResource(applicationContextService).toString()
    };
    ApplicationContext context = new ClassPathXmlApplicationContext(configLocations);

    mediaScannerService = (MediaScannerService)context.getBean("mediaScannerService");

    // delete index
    ElasticSearchClient ESClient = (ElasticSearchClient)context.getBean("elasticSearchClient");
    ESClient.deleteIndex();

  }

  public void testScanLibrary () {

    mediaScannerService.scanLibrary();

    while (mediaScannerService.isScanning()) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    System.out.print("End");
  }
}
