/**
 * Paquet de définition
 **/
package com.github.biconou.subsonic.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import junit.framework.TestCase;
import net.sourceforge.subsonic.domain.MediaFile;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.ALBUM;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.AUDIOBOOK;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.DIRECTORY;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.MUSIC;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.PODCAST;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.VIDEO;
import net.sourceforge.subsonic.service.metadata.MetaData;
import net.sourceforge.subsonic.service.metadata.MetaDataParser;
import net.sourceforge.subsonic.service.metadata.MetaDataParserFactory;
import net.sourceforge.subsonic.util.FileUtil;

/**
 * Description: Merci de donner une description du service rendu par cette classe
 */
public class MediaFileDaoTestCase extends TestCase {


  /* Code partly copied from MediaFileService */

  private boolean isVideoFile(String suffix) {
    String[] suffixes = new String[]{"AVI","MPG","MPEG"};
    for (String s : suffixes) {
      if (suffix.equals(s.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  private boolean isAudioFile(String suffix) {
    String[] suffixes = new String[]{"FLAC","MP3"};
    for (String s : suffixes) {
      if (suffix.equals(s.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  private MediaFile.MediaType getMediaType(MediaFile mediaFile) {
    if (isVideoFile(mediaFile.getFormat())) {
      return VIDEO;
    }
    String path = mediaFile.getPath().toLowerCase();
    String genre = StringUtils.trimToEmpty(mediaFile.getGenre()).toLowerCase();
    if (path.contains("podcast") || genre.contains("podcast")) {
      return PODCAST;
    }
    if (path.contains("audiobook") || genre.contains("audiobook") || path.contains("audio book") || genre.contains("audio book")) {
      return AUDIOBOOK;
    }
    return MUSIC;
  }

  public boolean isRoot(MediaFile mediaFile, String root) {
      if (mediaFile.getPath().equals(root)) {
        return true;
      }
    return false;
  }

  private boolean isExcluded(File file) {

    // Exclude all hidden files starting with a single "." or "@eaDir" (thumbnail dir created on Synology devices).
    String name = file.getName();
    return (name.startsWith(".") && !name.startsWith("..")) || name.startsWith("@eaDir") || name.equals("Thumbs.db");
  }


  public List<File> filterMediaFiles(File[] candidates) {
    List<File> result = new ArrayList<File>();
    for (File candidate : candidates) {
      String suffix = FilenameUtils.getExtension(candidate.getName()).toLowerCase();
      if (!isExcluded(candidate) && (FileUtil.isDirectory(candidate) || isAudioFile(suffix) || isVideoFile(suffix))) {
        result.add(candidate);
      }
    }
    return result;
  }


  private MediaFile createMediaFile(File file, MediaFileDao mediaFileDao, MetaDataParserFactory metaDataParserFactory, String root) {

    MediaFile existingFile = mediaFileDao.getMediaFile(file.getPath());

    MediaFile mediaFile = new MediaFile();
    Date lastModified = new Date(FileUtil.lastModified(file));
    mediaFile.setPath(file.getPath());
    //mediaFile.setFolder(securityService.getRootFolderForFile(file));
    mediaFile.setFolder(null);
    mediaFile.setParentPath(file.getParent());
    mediaFile.setChanged(lastModified);
    mediaFile.setLastScanned(new Date());
    mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
    mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
    mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
    mediaFile.setChildrenLastUpdated(new Date(0));
    mediaFile.setCreated(lastModified);
    mediaFile.setMediaType(DIRECTORY);
    mediaFile.setPresent(true);

    if (file.isFile()) {

      MetaDataParser parser = metaDataParserFactory.getParser(file);
      if (parser != null) {
        MetaData metaData = parser.getMetaData(file);
        mediaFile.setArtist(metaData.getArtist());
        mediaFile.setAlbumArtist(metaData.getAlbumArtist());
        mediaFile.setAlbumName(metaData.getAlbumName());
        mediaFile.setTitle(metaData.getTitle());
        mediaFile.setDiscNumber(metaData.getDiscNumber());
        mediaFile.setTrackNumber(metaData.getTrackNumber());
        mediaFile.setGenre(metaData.getGenre());
        mediaFile.setYear(metaData.getYear());
        mediaFile.setDurationSeconds(metaData.getDurationSeconds());
        mediaFile.setBitRate(metaData.getBitRate());
        mediaFile.setVariableBitRate(metaData.getVariableBitRate());
        mediaFile.setHeight(metaData.getHeight());
        mediaFile.setWidth(metaData.getWidth());
      }
      String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(mediaFile.getPath())));
      mediaFile.setFormat(format);
      mediaFile.setFileSize(FileUtil.length(file));
      mediaFile.setMediaType(getMediaType(mediaFile));

    } else {

      // Is this an album?
      if (!isRoot(mediaFile,root)) {
        File[] children = FileUtil.listFiles(file);
        File firstChild = null;
        for (File child : filterMediaFiles(children)) {
          if (FileUtil.isFile(child)) {
            firstChild = child;
            break;
          }
        }

        if (firstChild != null) {
          mediaFile.setMediaType(ALBUM);

          // Guess artist/album name, year and genre.
          MetaDataParser parser = metaDataParserFactory.getParser(firstChild);
          if (parser != null) {
            MetaData metaData = parser.getMetaData(firstChild);
            mediaFile.setArtist(metaData.getAlbumArtist());
            mediaFile.setAlbumName(metaData.getAlbumName());
            mediaFile.setYear(metaData.getYear());
            mediaFile.setGenre(metaData.getGenre());
          }

          /*
          // TODO handle covert Art path

          // Look for cover art.
          try {
            File coverArt = findCoverArt(children);
            if (coverArt != null) {
              mediaFile.setCoverArtPath(coverArt.getPath());
            }
          } catch (IOException x) {
            System.out.println("Failed to find cover art.");
            x.printStackTrace();
          }
          */

        } else {
          mediaFile.setArtist(file.getName());
        }
      }
    }

    return mediaFile;
  }


  /* Test Cases */

  public void test () {

    String applicationContextService = "/com/github/biconou/subsonic/service/mediaFileDaoTestCase/applicationContext-service.xml";
    String applicationContextCache = "/com/github/biconou/subsonic/service/mediaScannerServiceTestCase/applicationContext-cache.xml";

    String subsoncicHome = this.getClass().getResource("/com/github/biconou/subsonic/service/mediaFileDaoTestCase/").toString().replace("file:/","");
    System.setProperty("subsonic.home",subsoncicHome);

    String[] configLocations = new String[]{
      this.getClass().getResource(applicationContextCache).toString(),
      this.getClass().getResource(applicationContextService).toString()
    };
    ApplicationContext context = new ClassPathXmlApplicationContext(configLocations);

    MetaDataParserFactory metaDataParserFactory = (MetaDataParserFactory)context.getBean("metaDataParserFactory");
    MediaFileDao mediaFileDao = (MediaFileDao)context.getBean("mediaFileDao");

    String root = "D:\\musique";
    File fichierMusique1 = new File(root + "\\Ravel\\Ravel - Complete Piano Works\\02-Gaspard de la Nuit - ii. Le Gibet.complete.mp3");

    MediaFile mediaFileMusique1 = createMediaFile(fichierMusique1,mediaFileDao,metaDataParserFactory,root);

    mediaFileDao.createOrUpdateMediaFile(mediaFileMusique1);

    MediaFile mediaFileMusique1RetrievedByPatch = mediaFileDao.getMediaFile(mediaFileMusique1.getPath());


    System.out.print("End test.");
  }


}
 
