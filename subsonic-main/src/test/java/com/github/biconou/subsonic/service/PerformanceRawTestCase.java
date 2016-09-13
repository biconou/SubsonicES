package com.github.biconou.subsonic.service;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.context.ApplicationContext;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.biconou.subsonic.TestCaseUtils;
import com.github.biconou.subsonic.dao.ElasticSearchDaoHelper;
import junit.framework.TestCase;
import net.sourceforge.subsonic.dao.AlbumDao;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.service.SearchService;

/**
 * Created by remi on 01/05/2016.
 */
public class PerformanceRawTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/performanceServiceTestCase/";


  private final MetricRegistry metrics = new MetricRegistry();
  private ConsoleReporter reporter = null;
  private JmxReporter jmxReporter = null;


  private MediaFileDao mediaFileDao = null;
  private ElasticSearchDaoHelper daoHelper = null;
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

    /* jmxReporter = JmxReporter.forRegistry(metrics).build();
    jmxReporter.start();
    */


    // Prepare database
    TestCaseUtils.prepareDataBase(baseResources);

    // load spring context
    ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    mediaFileDao = (MediaFileDao) context.getBean("mediaFileDao");
    albumDao = (AlbumDao) context.getBean("albumDao");
    daoHelper = (ElasticSearchDaoHelper) context.getBean("elasticSearchDaoHelper");
    musicFolderDao = (MusicFolderDao) context.getBean("musicFolderDao");
    searchService = (SearchService) context.getBean("searchService");
    mediaScannerService = (MediaScannerService) context.getBean("mediaScannerService");

    // delete index
    TestCaseUtils.deleteIndexes(context);

  }

  /**
   *
   * @param source
   * @return
   */
  private String escapeForJson(String source) {
    if (source == null) {
      return "";
    } else {
      String target = source.replace("\\", "\\\\");
      target = target.replace("\"", "\\\"");
      return target;
    }
  }

  /**
   *
   */
  public void testPerformanceRawAlbumBrowse() throws Exception {


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

    int i = 0;
    boolean stop = false;

    while (!stop) {


      Timer.Context execNewestAlbumsQueryTimerContext = execNewestAlbumsQueryTimer.time();

      String jsonSearch = "{\n" +
        "    \"constant_score\" : {\n" +
        "        \"filter\" : {\n" +
        "            \"bool\" : {\n" +
        "                \"must\" : [\n" +
        "                    {\"term\" : { \"mediaType\" : \"ALBUM\" }},\n" +
        "                    {\"type\" : { \"value\" : \"MEDIA_FILE\" }}\n" +
        "                ]\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "}";

      SearchRequestBuilder searchRequestBuilder = daoHelper.getClient().prepareSearch(daoHelper.indexNames())
        .setQuery(jsonSearch).setVersion(true).setFrom(i * 10).setSize(10).addSort("created", SortOrder.DESC);
      SearchResponse response = searchRequestBuilder.execute().actionGet();
      SearchHits hits = response.getHits();
      execNewestAlbumsQueryTimerContext.stop();

      Iterator<SearchHit> hitIt = hits.iterator();
      if (!hitIt.hasNext()) {
        stop = true;
      }
      while (hitIt.hasNext()) {

        Timer.Context fetch1Context = fetchNewestAlbumsQueryTimer.time();
        SearchHit oneHit = hitIt.next();
        fetch1Context.stop();

        Timer.Context readRS1Context = readRSNewestAlbumsQueryTimer.time();
        String album = oneHit.getSource().get("albumName").toString();
        String album_artist = oneHit.getSource().get("artist").toString();
        readRS1Context.stop();
        if (album_artist == null) {
          album_artist = "";
        }
        else {
          album_artist = album_artist.replace("'", "''");
        }


        Timer.Context execSongsForAlbumQueryTimerContext = execSongsForAlbumQueryTimer.time();

        String jsonSongsForAlbum = "{\n" +
          "    \"constant_score\" : {\n" +
          "        \"filter\" : {\n" +
          "            \"bool\" : {\n" +
          "                \"must\" : [\n" +
          "                    {\"term\" : {\"albumArtist\" : \"" + escapeForJson(album_artist) + "\"}},\n" +
          "                    {\"term\" : {\"albumName\" : \"" + escapeForJson(album) + "\"}},\n" +
          "                    {\"type\" : { \"value\" : \"MEDIA_FILE\" }}\n" +
          "                ],\n" +
          "                \"should\" : [\n" +
          "                    {\"term\" : {\"mediaType\" : \"MUSIC\"}},\n" +
          "                    {\"term\" : {\"mediaType\" : \"AUDIOBOOK\"}},\n" +
          "                    {\"term\" : {\"mediaType\" : \"PODCAST\"}}\n" +
          "                ]\n" +
          "            }\n" +
          "        }\n" +
          "    }\n" +
          "}";

        SearchRequestBuilder searchRequestBuilderSongsForAlbum = daoHelper.getClient().prepareSearch(daoHelper.indexNames())
          .setQuery(jsonSongsForAlbum).setVersion(true);
        SearchResponse responseSongsForAlbum = searchRequestBuilderSongsForAlbum.execute().actionGet();
        SearchHits hitsSongsForAlbum = responseSongsForAlbum.getHits();
        Iterator<SearchHit> itSongsForAlbum = hitsSongsForAlbum.iterator();
        execSongsForAlbumQueryTimerContext.stop();


        while (itSongsForAlbum.hasNext()) {
          Timer.Context fetch2Context = fetchSongsForAlbumQueryTimer.time();
          SearchHit songHit = itSongsForAlbum.next();
          fetch2Context.stop();

          String path = songHit.getSource().get("path").toString();
          System.out.println(path);

        }

        //
        i++;
      }

      globalTimerContext.stop();
      reporter.report();
      System.out.print("End");

    }
  }
}
