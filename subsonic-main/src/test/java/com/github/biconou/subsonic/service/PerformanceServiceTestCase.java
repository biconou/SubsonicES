package com.github.biconou.subsonic.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.context.ApplicationContext;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
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

    int i = 0;
    while (i < 10000000) {
      Timer.Context loopTimerContext = loopTimer.time();

      // TODO mediaFolders ?
      List<MediaFile> foundAlbums = searchService.getRandomAlbums(10,null);

      foundAlbums.stream().forEach(album -> {
        System.out.println("++++++ "+album.getAlbumName());
        mediaFileDao.getSongsForAlbum(album.getArtist(),album.getAlbumName())
                .stream().forEach(song -> System.out.println(song.getName()));
      });


      loopTimerContext.stop();
      i++;
    }

    globalTimerContext.stop();
    reporter.report();

    System.out.print("End");
  }

}
