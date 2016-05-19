package com.github.biconou.subsonic.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import freemarker.template.TemplateException;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.MediaFile;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by remi on 11/05/2016.
 */
public class AlbumDao extends net.sourceforge.subsonic.dao.AlbumDao {

  private ElasticSearchDaoHelper elasticSearchDaoHelper = null;

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

    return getElasticSearchDaoHelper().getClient().prepareSearch(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
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

  @Override
  public synchronized void createOrUpdateAlbum(Album album) {
    createOrUpdateAlbum(album, false);
    //createOrUpdateMediaFile(file, true);
  }

  public synchronized void createOrUpdateAlbum(Album album, boolean synchrone) {
    SearchResponse searchResponse = searchAlbumByArtistAndName(album.getArtist(),album.getName());


    if (searchResponse == null || searchResponse.getHits().totalHits() == 0) {
      try {
        String json = getElasticSearchDaoHelper().getMapper().writeValueAsString(album);
        IndexResponse indexResponse = getElasticSearchDaoHelper().getClient().prepareIndex(
                ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME,
                ElasticSearchDaoHelper.ALBUM_INDEX_TYPE)
                .setSource(json).setVersionType(VersionType.INTERNAL).get();
        if (synchrone) {
          long l = 0;
          while (l == 0) {
            l = getElasticSearchDaoHelper().getClient().prepareSearch(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
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
        String json = getElasticSearchDaoHelper().getMapper().writeValueAsString(album);
        UpdateResponse response = getElasticSearchDaoHelper().getClient().prepareUpdate(
                ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME,
                ElasticSearchDaoHelper.ALBUM_INDEX_TYPE, id)
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
