package com.github.biconou.subsonic.dao;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.biconou.dao.ElasticSearchClient;
import net.sourceforge.subsonic.domain.Genre;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Created by remi on 26/04/2016.
 */
public class MediaFileDao extends net.sourceforge.subsonic.dao.MediaFileDao {


  private ElasticSearchClient elasticSearchClient = null;

  @Override
  public MediaFile getMediaFile(String path) {
    SearchResponse response = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchClient(), path);

    long nbFound = response.getHits().getTotalHits();
    if (nbFound > 1) {
      throw new RuntimeException("Index incoherence. more than one mediaFile found for patch"+path);
    }

    if (nbFound == 0) {
      return null;
    } else {
      return MediaFileDaoUtils.convertFromHit(getElasticSearchClient(),response.getHits().getHits()[0]);
    }
  }



  @Override
  public List<MediaFile> getChildrenOf(String path) {
   String jsonSearch = "{\"query\" : {\n" +
           "\t\"bool\" : {\n" +
           "\t\t\"filter\" : {\n" +
           "\t\t\t\"term\" : {\"parentPath\" : \""+MediaFileDaoUtils.preparePathForSearch(path)+"\"}\n" +
           "\t\t },\n" +
           "\t\t\"filter\" : {\n" +
           "\t\t\t\"term\" : {\"present\" : \"true\"}\n" +
           "\t\t}\n" +
           "\t}\n" +
           "}}";

    return MediaFileDaoUtils.extractMediaFiles(getElasticSearchClient(),jsonSearch,null,null);
  }








  /**
   *
   * @param genre
   * @param offset first offset is 0
   * @param count unlimited is 0
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
            "\t\t\t\t\t\"term\" : { \"genre\" : \""+genre+"\" }           \n" +
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

    return MediaFileDaoUtils.extractMediaFiles(getElasticSearchClient(),jsonSearch,offset,size);
  }


  @Override
    public synchronized void createOrUpdateMediaFile(MediaFile file) {

          SearchResponse searchResponse = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchClient(), file.getPath());

          if (searchResponse.getHits().totalHits() == 0) {
            try {
              String json = getElasticSearchClient().getMapper().writeValueAsString(file);
              IndexResponse indexResponse = getElasticSearchClient().getClient().prepareIndex(
                      ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME,
                      ElasticSearchClient.MEDIA_FILE_INDEX_TYPE)
                      .setSource(json).setVersionType(VersionType.INTERNAL).get();
              long l = 0;
              while (l==0) {
                l = getElasticSearchClient().getClient().prepareSearch(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
                        .setQuery(QueryBuilders.idsQuery().addIds(indexResponse.getId())).execute().actionGet().getHits().totalHits();
              }

            } catch (JsonProcessingException e) {
              throw new RuntimeException("Error trying indexing mediaFile "+e);
            }
          }  else {
            // update the media file.
            try {
              String id = searchResponse.getHits().getAt(0).id();
              long version  = searchResponse.getHits().getAt(0).version();
              String json = getElasticSearchClient().getMapper().writeValueAsString(file);
              UpdateResponse response = getElasticSearchClient().getClient().prepareUpdate(
                      ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME,
                      ElasticSearchClient.MEDIA_FILE_INDEX_TYPE, id)
                .setDoc(json).setVersion(version).setVersionType(VersionType.INTERNAL)
                .get();
              long newVersion = version;
              while (newVersion==version) {
                newVersion = getElasticSearchClient().getClient().prepareSearch(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
                        .setQuery(QueryBuilders.idsQuery().addIds(id)).setVersion(true).execute().actionGet().getHits().getAt(0).version();
              }
            } catch (JsonProcessingException e) {
              throw new RuntimeException("Error trying indexing mediaFile "+e);
            }
          }
    }

  public ElasticSearchClient getElasticSearchClient() {
    return elasticSearchClient;
  }

  public void setElasticSearchClient(ElasticSearchClient elasticSearchClient) {
    this.elasticSearchClient = elasticSearchClient;
  }


}
