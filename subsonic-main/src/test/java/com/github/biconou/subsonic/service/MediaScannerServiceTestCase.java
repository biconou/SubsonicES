package com.github.biconou.subsonic.service;

import com.github.biconou.dao.ElasticSearchClient;
import com.github.biconou.subsonic.dao.MediaFileDao;
import junit.framework.Assert;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.service.*;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by remi on 01/05/2016.
 */
public class MediaScannerServiceTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/";

  private MediaScannerService mediaScannerService = null;
  private MediaFileDao mediaFileDao = null;
  private MusicFolderDao musicFolderDao = null;

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
    mediaFileDao = (MediaFileDao)context.getBean("mediaFileDao");
    musicFolderDao = (MusicFolderDao) context.getBean("musicFolderDao");

    // delete index
    ElasticSearchClient ESClient = (ElasticSearchClient)context.getBean("elasticSearchClient");
    ESClient.deleteIndex();

  }

  private void execScan() {
    mediaScannerService.scanLibrary();

    while (mediaScannerService.isScanning()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  public void testScanLibrary() {

    String musicFolderPath = MusicFolderDaoMock.resolveMusicFolderPath().replace("/","\\");

    execScan();

    List<MediaFile> liste = mediaFileDao.getChildrenOf(musicFolderPath);
    Assert.assertEquals(3,liste.size());

    List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental",0,0,musicFolderDao.getAllMusicFolders());

    System.out.print("End");
  }




  public void testScanLibraryAndRenameAndScanAgain () {

    String musicFolderPath = MusicFolderDaoMock.resolveMusicFolderPath().replace("/","\\");

    execScan();

    List<MediaFile> liste = mediaFileDao.getChildrenOf(musicFolderPath);
    Assert.assertEquals(3,liste.size());

    File dir = new File(musicFolderPath + "\\Ravel");
    if (dir.isDirectory()) {
      dir.renameTo(new File(musicFolderPath + "\\Ravel_renamed"));
    }

    execScan();

    dir = new File(musicFolderPath + "\\Ravel_renamed");
    if (dir.isDirectory()) {
      dir.renameTo(new File(musicFolderPath + "\\Ravel"));
    }

    liste = mediaFileDao.getChildrenOf(musicFolderPath);
    Assert.assertEquals(4,liste.size());

    MediaFile renamed = mediaFileDao.getMediaFile(musicFolderPath + "\\Ravel");
    Assert.assertEquals(false,renamed.isPresent());
    System.out.print("End");
  }
}
