package com.github.biconou.subsonic.service;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.biconou.subsonic.TestCaseUtils;
import junit.framework.TestCase;
import net.sourceforge.subsonic.dao.AlbumDao;
import net.sourceforge.subsonic.dao.DaoHelper;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.SearchService;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.sourceforge.subsonic.domain.MediaFile.MediaType.AUDIOBOOK;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.MUSIC;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.PODCAST;

/**
 * Created by remi on 01/05/2016.
 */
public class PerformanceRawTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/performanceServiceTestCase/";

  private static final String COLUMNS = "id, path, folder, type, format, title, album, artist, album_artist, disc_number, " +
          "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, " +
          "parent_path, play_count, last_played, comment, created, changed, last_scanned, children_last_updated, present, version";


  private final MetricRegistry metrics = new MetricRegistry();
  private ConsoleReporter reporter = null;
  private JmxReporter jmxReporter = null;


  private MediaFileDao mediaFileDao = null;
  private DaoHelper daoHelper = null;
  private AlbumDao albumDao = null;
  private MusicFolderDao musicFolderDao = null;
  private SearchService searchService = null;
  private MediaScannerService mediaScannerService = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    TestCaseUtils.setSubsonicHome(baseResources);

    reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    jmxReporter = JmxReporter.forRegistry(metrics).build();
    jmxReporter.start();


    // Prepare database
    TestCaseUtils.prepareDataBase(baseResources);

    // load spring context
    ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    mediaFileDao = (MediaFileDao) context.getBean("mediaFileDao");
    albumDao = (AlbumDao) context.getBean("albumDao");
    daoHelper = (DaoHelper) context.getBean("daoHelper");
    musicFolderDao = (MusicFolderDao) context.getBean("musicFolderDao");
    searchService = (SearchService) context.getBean("searchService");
    mediaScannerService = (MediaScannerService) context.getBean("mediaScannerService");


  }


  /**
   *
   */
  public void testPerformanceRawAlbumBrowse() throws Exception {

    Connection cnx = daoHelper.getJdbcTemplate().getDataSource().getConnection();

    Timer globalTimer = metrics.timer(MetricRegistry.name("Timer.global"));
    Timer scanTimer = metrics.timer(MetricRegistry.name("Timer.scan"));

    Timer execNewestAlbumsQueryTimer = metrics.timer(MetricRegistry.name("Timer.execNewestAlbumsQuery"));
    Timer execSongsForAlbumQueryTimer = metrics.timer(MetricRegistry.name("Timer.execSongsForAlbumQuery"));

    Timer fetchNewestAlbumsQueryTimer = metrics.timer(MetricRegistry.name("Timer.fetchNewestAlbumsQuery"));
    Timer fetchSongsForAlbumQueryTimer = metrics.timer(MetricRegistry.name("Timer.fetchSongsForAlbumQuery"));

    Timer readRSNewestAlbumsQueryTimer = metrics.timer(MetricRegistry.name("Timer.readRSNewestAlbumsQuery"));
    Timer readRSSongsForAlbumQueryTimer = metrics.timer(MetricRegistry.name("Timer.readRSSongsForAlbumQuery"));

    Timer.Context globalTimerContext = globalTimer.time();

    Timer.Context scanTimerContext = scanTimer.time();
    TestCaseUtils.execScan(mediaScannerService);
    scanTimerContext.stop();

    Map<String,Integer> records = TestCaseUtils.recordsInAllTables(daoHelper);
    records.keySet().forEach(tableName -> System.out.println(tableName+" : "+records.get(tableName).toString() ));

    int i = 0;
    boolean stop = false;

    while (!stop) {


      Timer.Context execNewestAlbumsQueryTimerContext = execNewestAlbumsQueryTimer.time();
      String query = "select " + COLUMNS + " from media_file where type = 'ALBUM' and folder in ('"+MusicFolderDaoMock.resolveMusicFolderPath()+"','"+MusicFolderDaoMock.resolveMusic2FolderPath()+"') and present " +
              "order by created desc limit 10 offset "+i*10;
      Statement stmt = cnx.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      execNewestAlbumsQueryTimerContext.stop();

      stop = true;
      while (true)  {

        Timer.Context fetch1Context = fetchNewestAlbumsQueryTimer.time();
        boolean rsOk = rs.next();
        fetch1Context.stop();
        if (rsOk) {
          stop = false;
          Timer.Context readRS1Context = readRSNewestAlbumsQueryTimer.time();
          String album = rs.getString("album").replace("'", "''");
          String album_artist = rs.getString("artist");
          readRS1Context.stop();
          if (album_artist == null) {
            album_artist = "";
          } else {
            album_artist = album_artist.replace("'", "''");
          }



          Timer.Context execSongsForAlbumQueryTimerContext = execSongsForAlbumQueryTimer.time();
          String query2 = "select " + COLUMNS + " from media_file where album_artist='" + album_artist + "' and album='" + album + "' and present " +
                  "and type in ('MUSIC','AUDIOBOOK','PODCAST') order by disc_number, track_number";
          Statement stmt2 = cnx.createStatement();
          ResultSet rs2 = stmt2.executeQuery(query2);
          execSongsForAlbumQueryTimerContext.stop();


          while (true) {
            Timer.Context fetch2Context = fetchSongsForAlbumQueryTimer.time();
            boolean rs2Ok = rs2.next();
            fetch2Context.stop();
            if (rs2Ok) {
              Timer.Context readRS2Context = readRSSongsForAlbumQueryTimer.time();
              String path = rs2.getString("path");
              readRS2Context.stop();
              System.out.println(path);
            } else {
              break;
            }
          }

          stmt2.close();
          rs2.close();
        } else {
          break;
        }
      }

      stmt.close();
      rs.close();

      //
      i++;
    }

    globalTimerContext.stop();
    reporter.report();
    System.out.print("End");

  }
}
