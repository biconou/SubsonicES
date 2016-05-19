package com.github.biconou.subsonic.dao;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Created by remi on 26/04/2016.
 */
public class MediaFileDao extends net.sourceforge.subsonic.dao.MediaFileDao {


  private ElasticSearchDaoHelper elasticSearchDaoHelper = null;

  /**
   *
   * @param path
   * @return
   */
  protected SearchResponse searchMediaFileByPath(String path) {

    Map<String,String> vars = new HashMap<>();
    vars.put("path",preparePathForSearch(path));
    String jsonQuery = null;
    try {
      jsonQuery = getElasticSearchDaoHelper().getQuery("searchMediaFileByPath",vars);
    } catch (IOException|TemplateException e) {
      throw new RuntimeException(e);
    }

    return getElasticSearchDaoHelper().getClient().prepareSearch(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
            .setQuery(jsonQuery).setVersion(true).execute().actionGet();
  }


  /**
   *
   * @param path
   * @return
   */
  public static String preparePathForSearch(String path) {
    return path.replace("\\","\\\\");
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
    vars.put("path",preparePathForSearch(path));
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

    // TODO traiter le multi folder
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String,String> vars = new HashMap<String,String>();
    vars.put("genre",genre);

    Integer size = null;
    if (count > 0) {
      size = count;
    }

    return getElasticSearchDaoHelper().extractMediaFiles("getSongsByGenre",vars, offset, size,MediaFile.class);
  }

  @Override
  public synchronized void createOrUpdateMediaFile(MediaFile file) {
    createOrUpdateMediaFile(file, false);
    //createOrUpdateMediaFile(file, true);
  }

  public synchronized void createOrUpdateMediaFile(MediaFile file, boolean synchrone) {

    SearchResponse searchResponse = searchMediaFileByPath(file.getPath());

    if (searchResponse.getHits().totalHits() == 0) {
      try {
        String json = getElasticSearchDaoHelper().getMapper().writeValueAsString(file);
        IndexResponse indexResponse = getElasticSearchDaoHelper().getClient().prepareIndex(
                ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME,
                ElasticSearchDaoHelper.MEDIA_FILE_INDEX_TYPE)
                .setSource(json).setVersionType(VersionType.INTERNAL).get();
        if (synchrone) {
          long l = 0;
          while (l == 0) {
            l = getElasticSearchDaoHelper().getClient().prepareSearch(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
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
                ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME,
                ElasticSearchDaoHelper.MEDIA_FILE_INDEX_TYPE, id)
                .setDoc(json).setVersion(version).setVersionType(VersionType.INTERNAL)
                .get();
        if (synchrone) {
          long newVersion = version;
          while (newVersion == version) {
            newVersion = getElasticSearchDaoHelper().getClient().prepareSearch(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
                    .setQuery(QueryBuilders.idsQuery().addIds(id)).setVersion(true).execute().actionGet().getHits().getAt(0).version();
          }
        }
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error trying indexing mediaFile " + e);
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
