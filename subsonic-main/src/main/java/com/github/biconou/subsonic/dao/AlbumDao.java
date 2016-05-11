package com.github.biconou.subsonic.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.biconou.dao.ElasticSearchClient;
import net.sourceforge.subsonic.domain.Album;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Created by remi on 11/05/2016.
 */
public class AlbumDao extends net.sourceforge.subsonic.dao.AlbumDao {

  private ElasticSearchClient elasticSearchClient = null;



  @Override
  public synchronized void createOrUpdateAlbum(Album album) {
    //SearchResponse searchResponse = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchClient(), file.getPath());
    //rechercher where artist = and name =
    // TODO
    SearchResponse searchResponse = null;

    if (searchResponse == null || searchResponse.getHits().totalHits() == 0) {
      try {
        String json = getElasticSearchClient().getMapper().writeValueAsString(album);
        IndexResponse indexResponse = getElasticSearchClient().getClient().prepareIndex(
                ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME,
                ElasticSearchClient.ALBUM_INDEX_TYPE)
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
        String json = getElasticSearchClient().getMapper().writeValueAsString(album);
        UpdateResponse response = getElasticSearchClient().getClient().prepareUpdate(
                ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME,
                ElasticSearchClient.ALBUM_INDEX_TYPE, id)
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
