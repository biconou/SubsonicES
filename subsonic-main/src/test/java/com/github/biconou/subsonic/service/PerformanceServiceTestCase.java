package com.github.biconou.subsonic.service;

import java.util.concurrent.TimeUnit;
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
  }


  public void testPerformaceAlbumBrowse() {

    String musicFolderPath = MusicFolderDaoMock.resolveMusicFolderPath();

    startReport();

    Timer globalTimer = metrics.timer(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.global"));
    Timer loopTimer = metrics.timer(MetricRegistry.name(PerformanceServiceTestCase.class, "Timer.loop"));

    TimerContext globalTimerContext =  globalTimer.time();

    while (true) {
      TimerContext loopTimerContext = loopTimer.time();

      // Apppel service de recherche album aléatoire.
      // Sélection d'un album
      // appel servide album pour lister les chansons
      loopTimerContext.stop();
    }

    globalTimerContext.stop();

    System.out.print("End");
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
