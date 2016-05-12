/**
 * Paquet de d�finition
 **/
package com.github.biconou.subsonic.dao;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.metadata.MetaData;
import net.sourceforge.subsonic.service.metadata.MetaDataParser;
import net.sourceforge.subsonic.service.metadata.MetaDataParserFactory;
import net.sourceforge.subsonic.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static net.sourceforge.subsonic.domain.MediaFile.MediaType.*;

/**
 * Description: Merci de donner une description du service rendu par cette classe
 */
public class AlbumDaoTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/dao/mediaFileDaoTestCase/";

  private AlbumDao albumDao = null;

  /* Test Cases */

  @Override
  protected void setUp() throws Exception {
    super.setUp();

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

    albumDao = (AlbumDao)context.getBean("albumDao");
  }

  public void testGetAlbum () {

    Album album = albumDao.getAlbum("Sarah Walker/Nash Ensemble","Ravel - Chamber Music With Voice");
    System.out.print("End test.");
  }

}
 
