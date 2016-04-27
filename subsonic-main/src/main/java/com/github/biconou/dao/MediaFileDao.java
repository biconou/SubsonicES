package com.github.biconou.dao;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.subsonic.domain.MediaFile;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.bootstrap.Elasticsearch;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Created by remi on 26/04/2016.
 */
public class MediaFileDao extends net.sourceforge.subsonic.dao.MediaFileDao {




    private ElasticSearchClient elasticSearchClient = null;

    @Override
    public synchronized void createOrUpdateMediaFile(MediaFile file) {

        if (file.getMediaType().equals(MediaFile.MediaType.MUSIC) || file.getMediaType().equals(MediaFile.MediaType.ALBUM)) {

            try {
                String json = getElasticSearchClient().getMapper().writeValueAsString(file);
                IndexResponse response = getElasticSearchClient().getClient().prepareIndex(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME, file.getMediaType().toString())
                        .setSource(json)
                        .get();

            } catch (JsonProcessingException e) {
                e.printStackTrace();
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
