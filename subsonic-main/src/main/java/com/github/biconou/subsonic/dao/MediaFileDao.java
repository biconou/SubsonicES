package com.github.biconou.subsonic.dao;


import java.io.IOException;
import java.util.ArrayList;
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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
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

  public void setElasticSearchDaoHelper(ElasticSearchDaoHelper elasticSearchDaoHelper) {
    this.elasticSearchDaoHelper = elasticSearchDaoHelper;
  }

  public void setMusicFolderDao(MusicFolderDao musicFolderDao) {
    this.musicFolderDao = musicFolderDao;
  }



  /**
   * Retrieve a MediaFile identified by a path.
   *
   * @param path The path.
   * @return
   */
  @Override
  public MediaFile getMediaFile(String path) {
    Map<String,String> vars = new HashMap<>();
    vars.put("path",path);

    return elasticSearchDaoHelper.extractUnique("searchMediaFileByPath",vars,MediaFile.class);
  }


  @Override
  public List<MediaFile> getChildrenOf(String path) {
    Map<String,String> vars = new HashMap<>();
    vars.put("path",path);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getChildrenOf",vars,MediaFile.class);
  }

  @Override
  public List<MediaFile> getSongsForAlbum(String artist, String album) {
    Map<String,String> vars = new HashMap<>();
    vars.put("artist",artist);
    vars.put("album",album);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getSongsForAlbum",vars,MediaFile.class);
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

    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getSongsByGenre",vars, offset, size,null,musicFolders,MediaFile.class);
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


  @Override
  public synchronized void createOrUpdateMediaFile(MediaFile mediaFile) {

    String indexName = resolveIndexNameForMediaFile(mediaFile);
    logger.debug("CreateOrUpdate MediaFile : "+mediaFile.getPath()+" into index "+indexName);

    MediaFile allReadyExistsMediaFile = getMediaFile(mediaFile.getPath());

    if (allReadyExistsMediaFile == null) {
      elasticSearchDaoHelper.createObject(mediaFile, indexName, false);
    } else {
      elasticSearchDaoHelper.updateObject(allReadyExistsMediaFile, mediaFile, indexName, false);
    }
  }



  @Override
  public MediaFile getMediaFile(int id) {
    Map<String,String> vars = new HashMap<>();
    vars.put("id","" + id);
    List<MediaFile> list = elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getMediaFile", vars, MediaFile.class);
    if (list != null && list.size() > 0) {
      if (list.size() > 1) {
        throw new RuntimeException("Multiple Ids");
      } else {
        return list.get(0);
      }
    } else {
      return null;
    }
  }

  @Override
  public List<MediaFile> getFilesInPlaylist(int playlistId) {
    throw new UnsupportedOperationException("com.github.biconou.subsonic.dao.MediaFileDao.getFilesInPlaylist");
  }

  @Override
  public List<MediaFile> getVideos(int count, int offset, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,SortOrder> sortClause = new Hashtable<>();
    sortClause.put("title",SortOrder.ASC);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getVideos", null, offset, count,sortClause,musicFolders, MediaFile.class);
  }

  @Override
  public MediaFile getArtistByName(String name, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return null;
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("artist",name);
    List<MediaFile> list = elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getArtistByName", vars,null, null, null, musicFolders, MediaFile.class);
    if (list != null && list.size() > 0) {
      return list.get(0);
    } else {
      return null;
    }
  }

  @Override
  public void deleteMediaFile(String path) {
    // TODO
    throw new UnsupportedOperationException("com.github.biconou.subsonic.dao.MediaFileDao.deleteMediaFile");
  }

  @Override
  public List<Genre> getGenres(boolean sortByAlbum) {

    Map<String,Genre> genresMap = new Hashtable();

    SearchResponse genresResponse = elasticSearchDaoHelper.getClient().prepareSearch()
            .setQuery(QueryBuilders.typeQuery("MEDIA_FILE"))
            .addAggregation(AggregationBuilders.terms("mediaType_agg").field("mediaType")
                    .subAggregation(AggregationBuilders.terms("genre_agg").field("genre"))).setSize(0).get();


    StringTerms mediaTypeAgg = genresResponse.getAggregations().get("mediaType_agg");
    for (Terms.Bucket entry : mediaTypeAgg.getBuckets()) {
      String mediaTypeKey = entry.getKeyAsString();
      StringTerms genreAgg = entry.getAggregations().get("genre_agg");
      for (Terms.Bucket genreEntry : genreAgg.getBuckets()) {
        Genre genre = genresMap.get(genreEntry.getKeyAsString());
        if (genre == null) {
          genre = new Genre(genreEntry.getKeyAsString());
          genresMap.put(genre.getName(),genre);
        }
        if ("ALBUM".equals(mediaTypeKey)) {
          genre.setAlbumCount((int)genreEntry.getDocCount());
        }
        if ("MUSIC".equals(mediaTypeKey)) {
          genre.setSongCount((int)genreEntry.getDocCount());
        }
      }
    }

    List<Genre> genres = new ArrayList<>();
    genresMap.keySet().forEach(genreKey -> genres.add(genresMap.get(genreKey)));
    if (sortByAlbum) {
      genres.sort((o1, o2) -> {
        if (o1.getAlbumCount() > o2.getAlbumCount()) {
          return -1;
        }
        if (o1.getAlbumCount() < o2.getAlbumCount()) {
          return 1;
        }
        return 0;
      });
    } else {
      genres.sort((o1, o2) -> {
        if (o1.getSongCount() > o2.getSongCount()) {
          return -1;
        }
        if (o1.getSongCount() < o2.getSongCount()) {
          return 1;
        }
        return 0;
      });
    }
    return genres;
  }

  @Override
  public void updateGenres(List<Genre> genres) {
    // TODO
    throw new UnsupportedOperationException("com.github.biconou.subsonic.dao.MediaFileDao.updateGenres");
  }

  @Override
  public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,SortOrder> sortClause = new Hashtable<>();
    sortClause.put("playCount",SortOrder.DESC);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getMostFrequentlyPlayedAlbums", null, offset, count,null, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,SortOrder> sortClause = new Hashtable<>();
    sortClause.put("lastPlayed",SortOrder.DESC);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getMostRecentlyPlayedAlbums", null, offset, count,null, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,SortOrder> sortClause = new HashMap<>();
    sortClause.put("created",SortOrder.DESC);

    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getNewestAlbums", null, offset, count, sortClause,musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,SortOrder> sortClause = new HashMap<>();
    if (byArtist) {
      sortClause.put("artist",SortOrder.ASC);
    }
    sortClause.put("albumName",SortOrder.ASC);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getAlphabeticalAlbums", null, offset, count, sortClause,musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    // TODO
    Map<String,String> vars = new HashMap<>();
    vars.put("fromYear","" + fromYear);
    vars.put("toYear","" + toYear);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getAlbumsByYear", vars, offset, count,null, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("genre",genre);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getAlbumsByGenre", vars, offset, count,null, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getSongsByArtist(String artist, int offset, int count) {
    Map<String,String> vars = new HashMap<>();
    vars.put("artist","" + artist);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getSongsByArtist", vars, offset,count,null, MediaFile.class);
  }

  @Override
  public MediaFile getSongByArtistAndTitle(String artist, String title, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return null;
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("artist",artist);
    vars.put("title",title);
    List<MediaFile> list = elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getSongByArtistAndTitle", vars, null, null, null,musicFolders, MediaFile.class);
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
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getStarredAlbums", vars, offset, count, null,musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getStarredDirectories(int offset, int count, String username, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("username",username);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getStarredDirectories", vars, offset, count,null, musicFolders, MediaFile.class);
  }

  @Override
  public List<MediaFile> getStarredFiles(int offset, int count, String username, List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }
    Map<String,String> vars = new HashMap<>();
    vars.put("username",username);
    return elasticSearchDaoHelper.getElasticSearchDaoHelper(this).extractMediaFiles("getStarredFiles", vars, offset, count,null, musicFolders, MediaFile.class);
  }

  @Override
  public int getAlbumCount(List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return 0;
    }

    String jsonQuery = null;
    try {
      jsonQuery = elasticSearchDaoHelper.getElasticSearchDaoHelper(this).getQuery("getAlbumCount",null);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    return (int) elasticSearchDaoHelper.getElasticSearchDaoHelper(this).getClient().prepareSearch(elasticSearchDaoHelper.getElasticSearchDaoHelper(this).indexNames(musicFolders))
      .setQuery(jsonQuery).setVersion(true).execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
    if (musicFolders.isEmpty()) {
      return 0;
    }

    String jsonQuery = null;
    try {
      jsonQuery = elasticSearchDaoHelper.getElasticSearchDaoHelper(this).getQuery("getPlayedAlbumCount",null);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    // TODO multifolders
    return (int) elasticSearchDaoHelper.getElasticSearchDaoHelper(this).getClient().prepareSearch(elasticSearchDaoHelper.getElasticSearchDaoHelper(this).indexNames(musicFolders))
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
      jsonQuery = elasticSearchDaoHelper.getElasticSearchDaoHelper(this).getQuery("getStarredAlbumCount",vars);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    // TODO multifolders
    return (int) elasticSearchDaoHelper.getElasticSearchDaoHelper(this).getClient().prepareSearch(elasticSearchDaoHelper.getElasticSearchDaoHelper(this).indexNames(musicFolders))
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
    throw new UnsupportedOperationException("com.github.biconou.subsonic.dao.MediaFileDao.markPresent");
  }

  @Override
  public void markNonPresent(Date lastScanned) {
    //TODO
    throw new UnsupportedOperationException("com.github.biconou.subsonic.dao.MediaFileDao.markNonPresent");
  }

  @Override
  public void expunge() {
    //TODO
    throw new UnsupportedOperationException("com.github.biconou.subsonic.dao.MediaFileDao.expunge");
  }

  @Override
  public List<String> getArtistNames() {
    // TODO
    throw new UnsupportedOperationException("com.github.biconou.subsonic.dao.MediaFileDao.getArtistNames");
  }

  public long countMediaFiles(List<MusicFolder> folders) {
    if (folders == null) {
      throw new NullPointerException();
    }
    SearchResponse countResponse = elasticSearchDaoHelper.getClient().prepareSearch(elasticSearchDaoHelper.indexNames())
            .setQuery(QueryBuilders.typeQuery("MEDIA_FILE")).get();
    return countResponse.getHits().totalHits();
  }

  public long countMediaFileMusic(List<MusicFolder> folders) {
    if (folders == null) {
      throw new NullPointerException();
    }
    SearchResponse countResponse = elasticSearchDaoHelper.getClient().prepareSearch(elasticSearchDaoHelper.indexNames())
            .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.typeQuery("MEDIA_FILE")).must(QueryBuilders.termQuery("mediaType","MUSIC"))).get();
    return countResponse.getHits().totalHits();
  }

  public long countMediaFileAlbum(List<MusicFolder> folders) {
    if (folders == null) {
      throw new NullPointerException();
    }
    SearchResponse countResponse = elasticSearchDaoHelper.getClient().prepareSearch(elasticSearchDaoHelper.indexNames())
            .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.typeQuery("MEDIA_FILE")).must(QueryBuilders.termQuery("mediaType","ALBUM"))).get();
    return countResponse.getHits().totalHits();
  }

  public long countMediaFileDirectory(List<MusicFolder> folders) {
    if (folders == null) {
      throw new NullPointerException();
    }
    SearchResponse countResponse = elasticSearchDaoHelper.getClient().prepareSearch(elasticSearchDaoHelper.indexNames())
            .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.typeQuery("MEDIA_FILE")).must(QueryBuilders.termQuery("mediaType","DIRECTORY"))).get();
    return countResponse.getHits().totalHits();
  }

}
