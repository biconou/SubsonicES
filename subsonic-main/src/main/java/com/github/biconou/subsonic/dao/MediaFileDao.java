package com.github.biconou.subsonic.dao;


import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import freemarker.template.TemplateException;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.Genre;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;

/**
 * Created by remi on 26/04/2016.
 */
public class MediaFileDao extends net.sourceforge.subsonic.dao.MediaFileDao {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MediaFileDao.class);

  private MusicFolderDao musicFolderDao = null;

  private ElasticSearchDaoHelper elasticSearchDaoHelper = null;
  public ElasticSearchDaoHelper getElasticSearchDaoHelper() {
    return elasticSearchDaoHelper;
  }

  public void setElasticSearchDaoHelper(ElasticSearchDaoHelper elasticSearchDaoHelper) {
    this.elasticSearchDaoHelper = elasticSearchDaoHelper;
  }

  public void setMusicFolderDao(MusicFolderDao musicFolderDao) {
    this.musicFolderDao = musicFolderDao;
  }

  /**
   *
   * @param path
   * @return
   */
  protected SearchResponse searchMediaFileByPath(String path) {

    Map<String,String> vars = new HashMap<>();
    vars.put("path",path);
    String jsonQuery = null;
    try {
      jsonQuery = getElasticSearchDaoHelper().getQuery("searchMediaFileByPath",vars);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    return getElasticSearchDaoHelper().getClient().prepareSearch(musicFolderDao.getAllMusicFoldersLowerNames())
            .setQuery(jsonQuery).setVersion(true).execute().actionGet();
  }


  /**
   * Retrieve a MediaFile identified by a path.
   *
   * @param path The path.
   * @return
   */
  @Override
  public MediaFile getMediaFile(String path) {
    SearchResponse response = searchMediaFileByPath(path);

    long nbFound = response.getHits().getTotalHits();
    if (nbFound > 1) {
      throw new RuntimeException("Index incoherence. more than one mediaFile found for patch" + path);
    }

    if (nbFound == 0) {
      return null;
    } else {
      return getElasticSearchDaoHelper().convertFromHit(response.getHits().getHits()[0],MediaFile.class);
    }
  }


  @Override
  public List<MediaFile> getChildrenOf(String path) {
    Map<String,String> vars = new HashMap<>();
    vars.put("path",path);
    return getElasticSearchDaoHelper().extractMediaFiles("getChildrenOf",vars, null, null,MediaFile.class);
  }

  @Override
  public List<MediaFile> getSongsForAlbum(String artist, String album) {
    Map<String,String> vars = new HashMap<>();
    vars.put("artist",artist);
    vars.put("album",album);
    return getElasticSearchDaoHelper().extractMediaFiles("getSongsForAlbum",vars,null,null,MediaFile.class);
  }

  /**
   * @param genre
   * @param offset       first offset is 0
   * @param count        unlimited is 0
   * @param musicFolders
   * @return
   */
  @Override
  public List<MediaFile> getSongsByGenre(String genre, int offset, int count, List<MusicFolder> musicFolders) {

    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String,String> vars = new HashMap<String,String>();
    vars.put("genre",genre);

    Integer size = null;
    if (count > 0) {
      size = count;
    }

    return getElasticSearchDaoHelper().extractMediaFiles("getSongsByGenre",vars, offset, size,musicFolders,MediaFile.class);
  }


  @Override
  public synchronized void createOrUpdateMediaFile(MediaFile file) {
    createOrUpdateMediaFile(file, false);
    //createOrUpdateMediaFile(file, true);
  }

  /**
   *
   * @param mediaFile
   * @return
   */
  private String resolveIndexNameForMediaFile(MediaFile mediaFile) {
    for (MusicFolder musicFolder : musicFolderDao.getAllMusicFolders()) {
      if (musicFolder.getPath().getPath().equals(mediaFile.getFolder())) {
        return musicFolder.getName().toLowerCase();
      }
    }
    return null;
  }


  public synchronized void createOrUpdateMediaFile(MediaFile file, boolean synchrone) {

    logger.debug("CreateOrUpdate MediaFile : "+file.getPath());

    String indexName = resolveIndexNameForMediaFile(file);

    SearchResponse searchResponse = searchMediaFileByPath(file.getPath());

    if (searchResponse.getHits().totalHits() == 0) {
      try {
        String json = getElasticSearchDaoHelper().getMapper().writeValueAsString(file);
        IndexResponse indexResponse = getElasticSearchDaoHelper().getClient().prepareIndex(
                indexName,
                ElasticSearchDaoHelper.MEDIA_FILE_INDEX_TYPE)
                .setSource(json).setVersionType(VersionType.INTERNAL).get();
        if (synchrone) {
          long l = 0;
          while (l == 0) {
            l = getElasticSearchDaoHelper().getClient().prepareSearch(indexName)
                    .setQuery(QueryBuilders.idsQuery().addIds(indexResponse.getId())).execute().actionGet().getHits().totalHits();
          }
        }

      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error trying indexing mediaFile " + e);
      }
    } else {
      // update the media file.
      try {
        String id = searchResponse.getHits().getAt(0).id();
        long version = searchResponse.getHits().getAt(0).version();
        String json = getElasticSearchDaoHelper().getMapper().writeValueAsString(file);
        UpdateResponse response = getElasticSearchDaoHelper().getClient().prepareUpdate(
                indexName,
                ElasticSearchDaoHelper.MEDIA_FILE_INDEX_TYPE, id)
                .setDoc(json).setVersion(version).setVersionType(VersionType.INTERNAL)
                .get();
        if (synchrone) {
          long newVersion = version;
          while (newVersion == version) {
            newVersion = getElasticSearchDaoHelper().getClient().prepareSearch(indexName)
                    .setQuery(QueryBuilders.idsQuery().addIds(id)).setVersion(true).execute().actionGet().getHits().getAt(0).version();
          }
        }
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error trying indexing mediaFile " + e);
      }
    }
  }


  @Override
  public MediaFile getMediaFile(int id) {
    Map<String,String> vars = new HashMap<>();
    vars.put("id","" + id);
    List<MediaFile> list = getElasticSearchDaoHelper().extractMediaFiles("getMediaFile", vars, null, null, MediaFile.class);
    if (list != null && list.size() > 0) {
      return list.get(0);
    } else {
      return null;
    }
  }

  @Override
  public List<MediaFile> getFilesInPlaylist(int playlistId) {
    Map<String,String> vars = new HashMap<>();
    vars.put("playlistId","" + playlistId);
    return getElasticSearchDaoHelper().extractMediaFiles("getFilesInPlaylist", vars, null, null, MediaFile.class);
  }

  @Override
  public List<MediaFile> getVideos(int count, int offset, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    return getElasticSearchDaoHelper().extractMediaFiles("getVideos", null, count, offset,musicFolders, MediaFile.class);
  }

  @Override
  public MediaFile getArtistByName(String name, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return null;
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("name",name);
    List<MediaFile> list = getElasticSearchDaoHelper().extractMediaFiles("getArtistByName", null, null, null, musicFolders, MediaFile.class);
    if (list != null && list.size() > 0) {
      return list.get(0);
    } else {
      return null;
    }
  }

  @Override
  public void deleteMediaFile(String path) {
    // TODO
    // Appel de la classe parente
    super.deleteMediaFile(path);
  }

  @Override
  public List<Genre> getGenres(boolean sortByAlbum) {
    // TODO
    // Appel de la classe parente
    return super.getGenres(sortByAlbum);
  }

  @Override
  public void updateGenres(List<Genre> genres) {
    // TODO
    // Appel de la classe parente
    super.updateGenres(genres);
  }

  @Override
  public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    return getElasticSearchDaoHelper().extractMediaFiles("getMostFrequentlyPlayedAlbums", null, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    return getElasticSearchDaoHelper().extractMediaFiles("getMostRecentlyPlayedAlbums", null, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    return getElasticSearchDaoHelper().extractMediaFiles("getNewestAlbums", null, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("byArtist","" + byArtist);
    return getElasticSearchDaoHelper().extractMediaFiles("getAlphabeticalAlbums", vars, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("fromYear","" + fromYear);
    vars.put("toYear","" + toYear);
    return getElasticSearchDaoHelper().extractMediaFiles("getAlbumsByYear", vars, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("genre","" + genre);
    return getElasticSearchDaoHelper().extractMediaFiles("getAlbumsByGenre", vars, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getSongsByArtist(String artist, int offset, int count) {
    Map<String,String> vars = new HashMap<>();
    vars.put("artist","" + artist);
    return getElasticSearchDaoHelper().extractMediaFiles("getSongsByArtist", vars, count, offset, MediaFile.class);
  }

  @Override
  public MediaFile getSongByArtistAndTitle(String artist, String title, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return null;
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("artist",artist);
    vars.put("title",title);
    List<MediaFile> list = getElasticSearchDaoHelper().extractMediaFiles("getSongByArtistAndTitle", vars, null, null, musicFolders, MediaFile.class);
    if (list != null && list.size() > 0) {
      return list.get(0);
    } else {
      return null;
    }
  }

  @Override
  public List<MediaFile> getStarredAlbums(int offset, int count, String username, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("username",username);
    return getElasticSearchDaoHelper().extractMediaFiles("getStarredAlbums", vars, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getStarredDirectories(int offset, int count, String username, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("username",username);
    return getElasticSearchDaoHelper().extractMediaFiles("getStarredDirectories", vars, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getStarredFiles(int offset, int count, String username, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("username",username);
    return getElasticSearchDaoHelper().extractMediaFiles("getStarredFiles", vars, count, offset, musicFolders, MediaFile.class);
  }

  @Override
  public int getAlbumCount(List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return 0;
    }

    String jsonQuery = null;
    try {
      jsonQuery = getElasticSearchDaoHelper().getQuery("getAlbumCount",null);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    return (int) getElasticSearchDaoHelper().getClient().prepareSearch(musicFolderDao.getMusicFoldersLowerNames(musicFolders))
      .setQuery(jsonQuery).setVersion(true).execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return 0;
    }

    String jsonQuery = null;
    try {
      jsonQuery = getElasticSearchDaoHelper().getQuery("getPlayedAlbumCount",null);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    // TODO multifolders
    return (int) getElasticSearchDaoHelper().getClient().prepareSearch(musicFolderDao.getMusicFoldersLowerNames(musicFolders))
      .setQuery(jsonQuery).setVersion(true).execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public int getStarredAlbumCount(String username, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return 0;
    }

    Map<String,String> vars = new Hashtable<>();
    vars.put("username",username);
    String jsonQuery = null;
    try {
      jsonQuery = getElasticSearchDaoHelper().getQuery("getStarredAlbumCount",vars);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    // TODO multifolders
    return (int) getElasticSearchDaoHelper().getClient().prepareSearch(musicFolderDao.getMusicFoldersLowerNames(musicFolders))
      .setQuery(jsonQuery).setVersion(true).execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public void starMediaFile(int id, String username) {
    //TODO
    // Appel de la classe parente
    super.starMediaFile(id, username);
  }

  @Override
  public void unstarMediaFile(int id, String username) {
    //TODO
    // Appel de la classe parente
    super.unstarMediaFile(id, username);
  }

  @Override
  public Date getMediaFileStarredDate(int id, String username) {
    // TODO
    // Appel de la classe parente
    return super.getMediaFileStarredDate(id, username);
  }

  @Override
  public void markPresent(String path, Date lastScanned) {
    //TODO
    // Appel de la classe parente
    super.markPresent(path, lastScanned);
  }

  @Override
  public void markNonPresent(Date lastScanned) {
    //TODO
    // Appel de la classe parente
    super.markNonPresent(lastScanned);
  }

  @Override
  public void expunge() {
    //TODO
    // Appel de la classe parente
    super.expunge();
  }

  @Override
  public List<String> getArtistNames() {
    // TODO
    // Appel de la classe parente
    return super.getArtistNames();
  }
}
