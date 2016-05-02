package com.github.biconou.subsonic.service;

import junit.framework.TestCase;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.service.SettingsService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

/**
 * Created by remi on 01/05/2016.
 */
public class MediaScannerServiceTestCase extends TestCase {

  public void testScan () {

    String applicationContextService = "/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/applicationContext-service.xml";
    String applicationContextCache = "/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/applicationContext-cache.xml";

    String subsoncicHome = this.getClass().getResource("/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/").toString().replace("file:/","");
    System.setProperty("subsonic.home",subsoncicHome);

    String[] configLocations = new String[]{
            this.getClass().getResource(applicationContextCache).toString(),
            this.getClass().getResource(applicationContextService).toString()
    };
    ApplicationContext context = new ClassPathXmlApplicationContext(configLocations);

    net.sourceforge.subsonic.service.MediaScannerService mediaScannerService = (net.sourceforge.subsonic.service.MediaScannerService)context.getBean("mediaScannerService");
    mediaScannerService.scanLibrary();

    //SettingsService settingsService = (SettingsService)context.getBean("settingsService");
    //List<MusicFolder> liste = settingsService.getAllMusicFolders();


    while (mediaScannerService.isScanning()) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    System.out.print("toto");
  }
}
