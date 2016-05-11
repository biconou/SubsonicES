package com.github.biconou.dao;

import java.net.InetAddress;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.subsonic.domain.MediaFile;

/**
 * Created by remi on 26/04/2016.
 */
public class ElasticSearchClient {

  public static final String SUBSONIC_MEDIA_INDEX_NAME = "subsonic_media";
  public static final String MEDIA_FILE_INDEX_TYPE = "MEDIA_FILE";
  public static final String ALBUM_INDEX_TYPE = "ALBUM";


  private Client elasticSearchClient = null;
  private ObjectMapper mapper = new ObjectMapper();

  private Client obtainESClient() {
    return TransportClient.builder().build()
            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300));
  }

  public void deleteIndex() {
    Client client = obtainESClient();
    boolean indexExists = client.admin().indices().prepareExists(SUBSONIC_MEDIA_INDEX_NAME).execute().actionGet().isExists();
    if (indexExists) {
      client.admin().indices().prepareDelete(SUBSONIC_MEDIA_INDEX_NAME).execute().actionGet();
    }
    elasticSearchClient = null;
  }

  public Client getClient() {
    if (elasticSearchClient == null) {
      synchronized (this) {
        if (elasticSearchClient == null) {
          elasticSearchClient = obtainESClient();

          boolean indexExists = elasticSearchClient.admin().indices().prepareExists(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
                  .execute().actionGet().isExists();
          if (!indexExists) {
            elasticSearchClient.admin().indices()
                    .prepareCreate(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
                    .addMapping(MEDIA_FILE_INDEX_TYPE,
                            "path", "type=string,index=not_analyzed",
                            "parentPath", "type=string,index=not_analyzed",
                            "mediaType", "type=string,index=not_analyzed",
                            "folder", "type=string,index=not_analyzed",
                            "format", "type=string,index=not_analyzed",
                            "genre", "type=string,index=not_analyzed",
                            "coverArtPath", "type=string,index=not_analyzed",
                            "created", "type=date",
                            "changed", "type=date",
                            "childrenLastUpdated", "type=date",
                            "lastPlayed", "type=date",
                            "lastScanned", "type=date",
                            "starredDate", "type=date")
                    .addMapping(ALBUM_INDEX_TYPE,
                            "path", "type=string,index=not_analyzed",
                            "name", "type=string,index=not_analyzed",
                            "artist", "type=string,index=not_analyzed",
                            "coverArtPath", "type=string,index=not_analyzed",
                            "lastPlayed", "type=date",
                            "created", "type=date",
                            "lastScanned", "type=date")
                    .execute().actionGet();
          }

        }
      }
    }
    return elasticSearchClient;
  }

  public ObjectMapper getMapper() {
    return mapper;
  }
}
