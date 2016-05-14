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

  @Override
  public MediaFile getMediaFile(String path) {
    SearchResponse response = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchDaoHelper(), path);

    long nbFound = response.getHits().getTotalHits();
    if (nbFound > 1) {
      throw new RuntimeException("Index incoherence. more than one mediaFile found for patch" + path);
    }

    if (nbFound == 0) {
      return null;
    } else {
      return MediaFileDaoUtils.convertFromHit(getElasticSearchDaoHelper(), response.getHits().getHits()[0]);
    }
  }


  @Override
  public List<MediaFile> getChildrenOf(String path) {
    String jsonSearch = "{\"query\" : {\n" +
            "\t\"bool\" : {\n" +
            "\t\t\"filter\" : {\n" +
            "\t\t\t\"term\" : {\"parentPath\" : \"" + MediaFileDaoUtils.preparePathForSearch(path) + "\"}\n" +
            "\t\t },\n" +
            "\t\t\"filter\" : {\n" +
            "\t\t\t\"term\" : {\"present\" : \"true\"}\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}}";

    return MediaFileDaoUtils.extractMediaFiles(getElasticSearchDaoHelper(), jsonSearch, null, null);
  }

  @Override
  public List<MediaFile> getSongsForAlbum(String artist, String album) {
    Map<String,String> vars = new HashMap<>();
    vars.put("artist",artist);
    vars.put("album",album);
    String jsonQuery;
    try {
      jsonQuery = getElasticSearchDaoHelper().getQuery("getSongsForAlbum",vars);
    } catch (IOException |TemplateException e) {
      throw new RuntimeException(e);
    }

    return MediaFileDaoUtils.extractMediaFiles(getElasticSearchDaoHelper(),jsonQuery,null,null);
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

    // TODO reprendre le code de searchService
    String jsonSearch = "{\n" +
            "\t\"constant_score\" : {\n" +
            "\t\t\"filter\" : { \n" +
            "\t\t\t\"bool\" : {\n" +
            "\t\t\t\t\"must\" : {\n" +
            "\t\t\t\t\t\"term\" : { \"genre\" : \"" + genre + "\" }           \n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"should\" : [\n" +
            "\t\t\t\t\t{\"type\" : { \"value\" : \"MUSIC\" }},\n" +
            "\t\t\t\t\t{\"type\" : { \"value\" : \"PODCAST\" }},\n" +
            "\t\t\t\t\t{\"type\" : { \"value\" : \"AUDIOBOOK\" }}\n" +
            "\t\t\t\t]\n" +
            "\t\t\t\t\n" +
            "\t\t\t}\n" +
            "\t\t} \n" +
            "\t }\n" +
            "\t}";

    Integer size = null;
    if (count > 0) {
      size = count;
    }

    return MediaFileDaoUtils.extractMediaFiles(getElasticSearchDaoHelper(), jsonSearch, offset, size);
  }

  @Override
  public synchronized void createOrUpdateMediaFile(MediaFile file) {
    createOrUpdateMediaFile(file, true);
  }

  public synchronized void createOrUpdateMediaFile(MediaFile file, boolean synchrone) {

    SearchResponse searchResponse = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchDaoHelper(), file.getPath());

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
