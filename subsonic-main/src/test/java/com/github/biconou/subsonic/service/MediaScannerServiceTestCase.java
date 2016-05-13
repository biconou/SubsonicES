package com.github.biconou.subsonic.service;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.biconou.subsonic.TestCaseUtils;
import com.github.biconou.subsonic.dao.MediaFileDao;
import junit.framework.Assert;
import junit.framework.TestCase;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MediaFile;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by remi on 01/05/2016.
 */
public class MediaScannerServiceTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/";

  private final MetricRegistry metrics = new MetricRegistry();

  private MediaScannerService mediaScannerService = null;
  private MediaFileDao mediaFileDao = null;
  private MusicFolderDao musicFolderDao = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Prepare database
    TestCaseUtils.prepareDataBase(baseResources);

    TestCaseUtils.setSubsonicHome(baseResources);

    // load spring context
    ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    mediaScannerService = (MediaScannerService)context.getBean("mediaScannerService");
    mediaFileDao = (MediaFileDao)context.getBean("mediaFileDao");
    musicFolderDao = (MusicFolderDao) context.getBean("musicFolderDao");

    // delete index
    TestCaseUtils.deleteIndex(context);
  }


  public void testScanLibrary() {

    ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    String musicFolderPath = MusicFolderDaoMock.resolveMusicFolderPath();

    Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceTestCase.class, "Timer.global"));
    Timer.Context globalTimerContext =  globalTimer.time();
    TestCaseUtils.execScan(mediaScannerService);
    globalTimerContext.stop();

    reporter.report();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<MediaFile> liste = mediaFileDao.getChildrenOf(musicFolderPath);
    Assert.assertEquals(3,liste.size());

    List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental",0,0,musicFolderDao.getAllMusicFolders());


    System.out.print("End");
  }




  public void testScanLibraryAndRenameAndScanAgain () {

    String musicFolderPath = MusicFolderDaoMock.resolveMusicFolderPath();

    TestCaseUtils.execScan(mediaScannerService);

    List<MediaFile> liste = mediaFileDao.getChildrenOf(musicFolderPath);
    Assert.assertEquals(3,liste.size());

    File dir = new File(musicFolderPath + "\\Ravel");
    if (dir.isDirectory()) {
      dir.renameTo(new File(musicFolderPath + "\\Ravel_renamed"));
    }

    TestCaseUtils.execScan(mediaScannerService);

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
