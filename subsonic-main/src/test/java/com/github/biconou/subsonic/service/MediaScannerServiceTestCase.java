package com.github.biconou.subsonic.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationContext;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.biconou.subsonic.TestCaseUtils;
import com.github.biconou.subsonic.dao.ElasticSearchDaoHelper;
import com.github.biconou.subsonic.dao.MediaFileDao;
import junit.framework.Assert;
import junit.framework.TestCase;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.service.MediaFileService;

/**
 * Created by remi on 01/05/2016.
 */
public class MediaScannerServiceTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/";

  private final MetricRegistry metrics = new MetricRegistry();

  private MediaScannerService mediaScannerService = null;
  private MediaFileService mediaFileService = null;
  private MediaFileDao mediaFileDao = null;
  private MusicFolderDao musicFolderDao = null;
  ElasticSearchDaoHelper elasticSearchDaoHelper = null;

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
    mediaFileService = (MediaFileService) context.getBean("mediaFileService");
    elasticSearchDaoHelper = (ElasticSearchDaoHelper)context.getBean("elasticSearchDaoHelper");

    // delete index
    TestCaseUtils.deleteIndexes(context);
  }

  private String resolveRealPath(String path) {
    String returnPath = MusicFolderDaoMock.resolveMusicFolderPath();
    String modifiedPath = path;
    if (returnPath.contains("\\")) {
      modifiedPath = modifiedPath.replace("/","\\");
    }
    returnPath += modifiedPath;
    return  returnPath;
  }

  private String resolveReal2Path(String path) {
    String returnPath = MusicFolderDaoMock.resolveMusic2FolderPath();
    String modifiedPath = path;
    if (returnPath.contains("\\")) {
      modifiedPath = modifiedPath.replace("/","\\");
    }
    returnPath += modifiedPath;
    return  returnPath;
  }

  private static String basePath(String baseResources) {
    String basePath = MediaScannerServiceTestCase.class.getResource(baseResources).toString();
    if (basePath.startsWith("file:")) {
      return TestCaseUtils.class.getResource(baseResources).toString().replace("file:","");
    }
    return basePath;
  }




  /**
   *
   */
  private void deleteAddedAlbum() throws IOException {
    //File destDir = new File(MediaScannerServiceTestCase.class.getResource("/MEDIAS/Music2/chrome hoof - Album added").getFile());
    File destDir = null;
    try {
      destDir = new File(new URI(MediaScannerServiceTestCase.class.getResource("/MEDIAS/Music2/chrome hoof - Album added").toString()).getPath());
    }
    catch (URISyntaxException e) {
      // TODO: Le traitement de cette exception a été générée automatiquement. Merci de vérifier
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    if (destDir.exists()) {
      FileUtils.deleteDirectory(destDir);
    }
  }


  /**
   *
   */
  private void copyAddedMedias() throws IOException {
    File toAddDir = new File(MediaScannerServiceTestCase.class.getResource("/MEDIAS/ToAdd").getFile());
    File destDir = new File(MediaScannerServiceTestCase.class.getResource("/MEDIAS/Music2").getFile());
    /* if (toAddDir.exists()) {
      FileUtils.deleteDirectory(toAddDir);
    }*/
    FileUtils.copyDirectory(toAddDir,destDir,true);
  }

  /**
   *
   * @throws IOException
   */
  public void testScanLibrary() throws IOException {

    String log4jFile = MediaScannerServiceTestCase.class.getResource("/log4j.xml").toString();
    System.out.println(log4jFile);

    ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    String musicFolderPath = MusicFolderDaoMock.resolveMusicFolderPath();

    deleteAddedAlbum();

    Timer globalTimer = metrics.timer(MetricRegistry.name(MediaScannerServiceTestCase.class, "Timer.global"));
    Timer.Context globalTimerContext =  globalTimer.time();
    TestCaseUtils.execScan(mediaScannerService);
    globalTimerContext.stop();

    reporter.report();

    // Wait for end of index
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Count the number of media_files
    SearchResponse countResponse = elasticSearchDaoHelper.getClient().prepareSearch()
      .setQuery(QueryBuilders.typeQuery("MEDIA_FILE")).get();

    assertEquals(20,countResponse.getHits().getTotalHits());



    ///
    List<MediaFile> liste = mediaFileDao.getChildrenOf(musicFolderPath);
    Assert.assertEquals(3,liste.size());

    ///
    List<MediaFile> listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental",0,0,musicFolderDao.getAllMusicFolders());
    Assert.assertEquals(2,listeSongs.size());
    listeSongs.stream().forEach(mediaFile1 -> {
      MediaFile mf = mediaFileDao.getMediaFile(mediaFile1.getPath());
      Assert.assertNotNull(mf);
      Assert.assertEquals("Baroque Instrumental",mf.getGenre());
    });

    //
    List<MusicFolder> musicFolders = musicFolderDao.getAllMusicFolders();
    musicFolders.remove(0);
    listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental",0,0,musicFolders);
    Assert.assertEquals(0,listeSongs.size());

    //
    musicFolders = musicFolderDao.getAllMusicFolders();
    musicFolders.remove(1);
    listeSongs = mediaFileDao.getSongsByGenre("Baroque Instrumental",0,0,musicFolders);
    Assert.assertEquals(2,listeSongs.size());

    //
    String path = "/Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]/01 - Bach- Goldberg Variations, BWV 988 - Aria.flac";
    path = resolveRealPath(path);
    MediaFile mediaFile = mediaFileDao.getMediaFile(path);
    Assert.assertNotNull(mediaFile);
    Assert.assertEquals("Céline Frisch: Café Zimmermann",mediaFile.getAlbumArtist());
    Assert.assertEquals("Céline Frisch: Café Zimmermann",mediaFile.getArtist());
    Assert.assertEquals("Bach: Goldberg Variations, Canons [Disc 1]",mediaFile.getAlbumName());
    Assert.assertEquals(new Integer(2001),mediaFile.getYear());
    Assert.assertEquals(MediaFile.MediaType.MUSIC,mediaFile.getMediaType());
    Assert.assertEquals("flac",mediaFile.getFormat());

    //
    String music2Path = resolveReal2Path("");
    File music2File = new File(music2Path);
    MediaFile music2mediaFile = mediaFileService.getMediaFile(music2File);

    //
    List<MediaFile> newestAlbums = mediaFileDao.getNewestAlbums(0,10,musicFolderDao.getAllMusicFolders());

    //
    MediaFile ravelArtist = mediaFileDao.getArtistByName("Ravel",musicFolderDao.getAllMusicFolders());
    Assert.assertEquals("Ravel",ravelArtist.getArtist());


    //
    mediaFileDao.getGenres(false);


    //
    copyAddedMedias();

    //


    System.out.print("End");
  }

}
