package com.github.biconou.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.subsonic.domain.MediaFile;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.net.InetAddress;

/**
 * Created by remi on 26/04/2016.
 */
public class ElasticSearchClient {

    public static final String SUBSONIC_MEDIA_INDEX_NAME = "subsonic_media";

    public static final String ALBUM_TYPE = MediaFile.MediaType.ALBUM.toString();
    public static final String MUSIC_TYPE = MediaFile.MediaType.MUSIC.toString();

    private Client elasticSearchClient = null;
    private ObjectMapper mapper = new ObjectMapper();

    public Client getClient() {
        if (elasticSearchClient == null) {
            synchronized (this) {
                if (elasticSearchClient == null) {
                    elasticSearchClient = TransportClient.builder().build()
                            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300));
                }
            }
        }
        return elasticSearchClient;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
