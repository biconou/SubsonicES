package com.github.biconou.subsonic.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.*;
import org.springframework.context.ApplicationContext;
import com.github.biconou.subsonic.TestCaseUtils;
import com.github.biconou.subsonic.dao.AlbumDao;
import com.github.biconou.subsonic.dao.MediaFileDao;
import junit.framework.TestCase;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.SearchService;

/**
 * Created by remi on 01/05/2016.
 */
public class PerformanceServiceTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/performanceServiceTestCase/";

  private final MetricRegistry metrics = new MetricRegistry();
  private ConsoleReporter reporter = null;
  private JmxReporter jmxReporter = null;


  private MediaFileDao mediaFileDao = null;
  private AlbumDao albumDao = null;
  private MusicFolderDao musicFolderDao = null;
  private SearchService searchService = null;
  private MediaScannerService mediaScannerService = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    jmxReporter = JmxReporter.forRegistry(metrics).build();
    jmxReporter.start();


    // Prepare database
    TestCaseUtils.prepareDataBase(baseResources);

    TestCaseUtils.setSubsonicHome(baseResources);

    // load spring context
    ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    mediaFileDao = (MediaFileDao)context.getBean("mediaFileDao");
    albumDao = (AlbumDao) context.getBean("albumDao");
    musicFolderDao = (MusicFolderDao) context.getBean("musicFolderDao");
    searchService = (SearchService) context.getBean("searchService");
    mediaScannerService = (MediaScannerService)context.getBean("mediaScannerService");
  }




  public void testPerformaceAlbumBrowse() {

    Timer globalTimer = metrics.timer(MetricRegistry.name(this.getClass(), "Global.scan"));
    Timer.Context globalTimerContext =  globalTimer.time();

    Timer loopTimer = metrics.timer(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.loop"));
    Timer fetchSongsTimer = metrics.timer(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.fetchSongs"));
    Timer randomAlbumsTimer = metrics.timer(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.randomAlbums"));
    //Timer randomAlbumsTimer = metrics.register(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.randomAlbums"),new Timer(new UniformReservoir()));

    int i = 0;
    while (i < 100000) {

      final boolean fisrtIteration = i==0;

      Timer.Context loopTimerContext = null;
      if (!fisrtIteration) {
         loopTimerContext = loopTimer.time();
      }

      // TODO mediaFolders ?
      Timer.Context randomAlbumsTimerContext = null;
      // We do not take in consideration the time of the first iteration
      // because it could be much more slow than the other ones
      // due to the loading of the FreeMarker query templates.
      if (!fisrtIteration) {
        randomAlbumsTimerContext = randomAlbumsTimer.time();
      }
      List<MediaFile> foundAlbums = searchService.getRandomAlbums(10,null);
      if (!fisrtIteration) {
        randomAlbumsTimerContext.stop();
      }

      foundAlbums.stream().forEach(album -> {

        Timer.Context fetchSongsTimerContext = null;
        if (!fisrtIteration) {
          fetchSongsTimerContext = fetchSongsTimer.time();
        }
        mediaFileDao.getSongsForAlbum(album.getArtist(),album.getAlbumName())
                .stream().forEach(song -> System.out.println(song.getName()));
        if (!fisrtIteration) {
          fetchSongsTimerContext.stop();
        }
      });

      if (!fisrtIteration) {
        loopTimerContext.stop();
      }
      i++;
    }

    globalTimerContext.stop();
    reporter.report();

    System.out.print("End");
  }

}
