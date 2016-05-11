/**
 * Paquet de définition
 **/
package com.github.biconou.subsonic.dao;

import com.github.biconou.dao.ElasticSearchClient;
import com.sun.istack.Nullable;
import net.sourceforge.subsonic.domain.MediaFile;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Description: Merci de donner une description du service rendu par cette classe
 */
public class AlbumDaoUtils {

  protected static SearchResponse searchAlbumByArtistAndName(ElasticSearchClient client,String artist, String name) {

    // TODO
    String jsonSearch = "{" +
      "    \"constant_score\" : {" +
      "        \"filter\" : {" +
      "            \"term\" : {" +
      "                \"path\" : \""+""+"\"" +
      "            }" +
      "        }" +
      "    }" +
      "}";

    return client.getClient().prepareSearch(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
      .setQuery(jsonSearch).setVersion(true).execute().actionGet();
  }

}
 
