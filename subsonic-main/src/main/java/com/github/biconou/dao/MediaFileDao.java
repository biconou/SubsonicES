package com.github.biconou.dao;


import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.sourceforge.subsonic.domain.MediaFile;

/**
 * Created by remi on 26/04/2016.
 */
public class MediaFileDao extends net.sourceforge.subsonic.dao.MediaFileDao {


    private ElasticSearchClient elasticSearchClient = null;

    @Override
    public synchronized void createOrUpdateMediaFile(MediaFile file) {

        if (file.getMediaType().equals(MediaFile.MediaType.MUSIC) || file.getMediaType().equals(MediaFile.MediaType.ALBUM)) {

         /* SearchResponse alreadyIndexedMediaFileResponse = getElasticSearchClient().getClient().prepareSearch(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
            .setQuery(QueryBuilders.idsQuery().ids("" + file.getId()))
            .execute().actionGet();
*/

        /* SearchResponse alreadyIndexedMediaFileResponse = getElasticSearchClient().getClient().prepareSearch(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
            .setQuery(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("path", file.getPath())))
           .setPostFilter()
            .execute().actionGet();
            */


          String jsonSearch = "" +
            "{" +
            "    \"constant_score\" : {" +
            "        \"filter\" : {" +
            "            \"term\" : {" +
            "                \"path\" : \""+file.getPath()+"\"" +
            "            }" +
            "        }" +
            "    }" +
            "}";


          SearchResponse alreadyIndexedMediaFileResponse = getElasticSearchClient().getClient().prepareSearch(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
            .setQuery(jsonSearch).execute().actionGet();

          alreadyIndexedMediaFileResponse.getHits().getTotalHits();


          try {
              //TODO traiter la mise à jour
                String json = getElasticSearchClient().getMapper().writeValueAsString(file);
                IndexResponse response = getElasticSearchClient().getClient().prepareIndex(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME, file.getMediaType().toString(), "" + file.getId())
                        .setSource(json)
                        .get();

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error trying indexing mediaFile "+e);
            }

        } else {
            super.createOrUpdateMediaFile(file);
        }
    }

    public ElasticSearchClient getElasticSearchClient() {
        return elasticSearchClient;
    }

    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient) {
        this.elasticSearchClient = elasticSearchClient;
    }
}
