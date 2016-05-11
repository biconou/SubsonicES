package com.github.biconou.subsonic.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.SearchService;
import org.springframework.context.ApplicationContext;
import com.github.biconou.subsonic.TestCaseUtils;
import com.github.biconou.subsonic.dao.MediaFileDao;
import junit.framework.TestCase;
import net.sourceforge.subsonic.dao.MusicFolderDao;

/**
 * Created by remi on 01/05/2016.
 */
public class PerformanceServiceTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/performanceServiceTestCase/";

  private final MetricRegistry metrics = new MetricRegistry();


  private MediaFileDao mediaFileDao = null;
  private MusicFolderDao musicFolderDao = null;
  private SearchService searchService = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Prepare database
    TestCaseUtils.prepareDataBase(baseResources);

    TestCaseUtils.setSubsonicHome(baseResources);

    // load spring context
    ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    mediaFileDao = (MediaFileDao)context.getBean("mediaFileDao");
    musicFolderDao = (MusicFolderDao) context.getBean("musicFolderDao");
    searchService = (SearchService) context.getBean("searchService");

  }


  public void testPerformaceAlbumBrowse() {

    String musicFolderPath = MusicFolderDaoMock.resolveMusicFolderPath();

    startReport();

    Timer globalTimer = metrics.timer(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.global"));
    Timer loopTimer = metrics.timer(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.loop"));

   // Timer.Context globalTimerContext =  globalTimer.time();

    while (true) {
      Timer.Context loopTimerContext = loopTimer.time();

      // TODO mediaFolders ?
      List<MediaFile> foundAlbums = searchService.getRandomAlbums(10,null);

      /*
      MediaFile oneAlbum = foundAlbums.get(5);
      Album album = albumDao.getAlbum(id);
      for (MediaFile mediaFile : mediaFileDao.getSongsForAlbum(album.getArtist(), album.getName())) {
        result.getSong().add(createJaxbChild(player, mediaFile, username));
      }
      */

      loopTimerContext.stop();
    }

    //globalTimerContext.stop();

    //System.out.print("End");
  }

  private void startReport() {

    // Reporter console
    ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
    reporter.start(10, TimeUnit.SECONDS);

    // Jmx reporter
    final JmxReporter jmxReporter = JmxReporter.forRegistry(metrics).build();
    jmxReporter.start();
  }


}
