package com.github.biconou.subsonic.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import freemarker.template.TemplateException;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by remi on 11/05/2016.
 */
public class AlbumDao extends net.sourceforge.subsonic.dao.AlbumDao {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AlbumDao.class);

  private ElasticSearchDaoHelper elasticSearchDaoHelper = null;

  private MusicFolderDao musicFolderDao = null;

  public void setMusicFolderDao(MusicFolderDao musicFolderDao) {
    this.musicFolderDao = musicFolderDao;
  }

  private SearchResponse searchAlbumByArtistAndName(String artist, String name) {

    Map<String,String> vars = new HashMap<>();
    vars.put("artist",artist);
    vars.put("name",name);
    String jsonQuery = null;
    try {
      jsonQuery = getElasticSearchDaoHelper().getQuery("searchAlbumByArtistAndName",vars);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    return getElasticSearchDaoHelper().getClient().prepareSearch(musicFolderDao.getAllMusicFoldersNames())
            .setQuery(jsonQuery).setVersion(true).execute().actionGet();
  }


  @Override
  public Album getAlbum(String artistName, String albumName) {
    SearchResponse response = searchAlbumByArtistAndName(artistName,albumName);

    long nbFound = response.getHits().getTotalHits();
    if (nbFound > 1) {
      throw new RuntimeException("Index incoherence. more than one album found for artist "+artistName+" and name "+albumName);
    }

    if (nbFound == 0) {
      return null;
    } else {
      return getElasticSearchDaoHelper().convertFromHit(response.getHits().getHits()[0],Album.class);
    }

  }

  /**
   *
   * @param
   * @return
   */
  private String resolveMusicFolderNameForAlbum(Album album) {
    for (MusicFolder musicFolder : musicFolderDao.getAllMusicFolders()) {
      if (musicFolder.getId().equals(album.getFolderId())) {
        return musicFolder.getName();
      }
    }
    return null;
  }

  @Override
  public synchronized void createOrUpdateAlbum(Album album) {
    createOrUpdateAlbum(album, false);
    //createOrUpdateMediaFile(file, true);
  }

  public synchronized void createOrUpdateAlbum(Album album, boolean synchrone) {

    logger.debug("CreateOrUpdateAlbum for artist=["+album.getArtist()+"] and name=["+album.getName()+"]");
    SearchResponse searchResponse = searchAlbumByArtistAndName(album.getArtist(),album.getName());

    String indexName = resolveMusicFolderNameForAlbum(album);


    if (searchResponse == null || searchResponse.getHits().totalHits() == 0) {
      logger.debug("Album does not exist");
      try {
        String json = getElasticSearchDaoHelper().getMapper().writeValueAsString(album);
        IndexResponse indexResponse = getElasticSearchDaoHelper().getClient().prepareIndex(
                indexName,
                ElasticSearchDaoHelper.ALBUM_INDEX_TYPE)
                .setSource(json).setVersionType(VersionType.INTERNAL).get();
        if (synchrone) {
          long l = 0;
          while (l == 0) {
            l = getElasticSearchDaoHelper().getClient().prepareSearch(indexName)
                    .setQuery(QueryBuilders.idsQuery().addIds(indexResponse.getId())).execute().actionGet().getHits().totalHits();
          }
        }

      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error trying indexing mediaFile "+e);
      }
    }  else {
      // update the media file.
      try {
        String id = searchResponse.getHits().getAt(0).id();
        long version  = searchResponse.getHits().getAt(0).version();
        logger.debug("Album exists with id=["+id+"] and version=["+version+"]. -> update with a new version.");
        String json = getElasticSearchDaoHelper().getMapper().writeValueAsString(album);
        UpdateResponse response = getElasticSearchDaoHelper().getClient().prepareUpdate(
                indexName,
                ElasticSearchDaoHelper.ALBUM_INDEX_TYPE, id)
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
        throw new RuntimeException("Error trying indexing mediaFile "+e);
      }
    }
  }

  public ElasticSearchDaoHelper getElasticSearchDaoHelper() {
    return elasticSearchDaoHelper;
  }

  public void setElasticSearchDaoHelper(ElasticSearchDaoHelper elasticSearchDaoHelper) {
    this.elasticSearchDaoHelper = elasticSearchDaoHelper;
  }

}
