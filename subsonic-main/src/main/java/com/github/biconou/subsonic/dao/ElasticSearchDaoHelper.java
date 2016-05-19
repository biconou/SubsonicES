package com.github.biconou.subsonic.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ser.impl.StringArraySerializer;
import com.sun.istack.Nullable;
import com.sun.media.jfxmedia.Media;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sourceforge.subsonic.domain.MediaFile;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.search.SearchHit;

/**
 * Created by remi on 26/04/2016.
 */
public class ElasticSearchDaoHelper {

  public static final String SUBSONIC_MEDIA_INDEX_NAME = "subsonic_media";
  public static final String MEDIA_FILE_INDEX_TYPE = "MEDIA_FILE";
  public static final String ALBUM_INDEX_TYPE = "ALBUM";


  private Client elasticSearchClient = null;
  private ObjectMapper mapper = new ObjectMapper();
  private Map<String,Template> queryTemplates = new HashMap<>();
  private Configuration freeMarkerConfiguration = null;

  public ElasticSearchDaoHelper() {
    freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_23);
    freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/com/github/biconou/subsonic/dao");
  }

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

          boolean indexExists = elasticSearchClient.admin().indices().prepareExists(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
                  .execute().actionGet().isExists();
          if (!indexExists) {
            elasticSearchClient.admin().indices()
                    .prepareCreate(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
                    .addMapping(MEDIA_FILE_INDEX_TYPE,
                            "path", "type=string,index=not_analyzed",
                            "parentPath", "type=string,index=not_analyzed",
                            "mediaType", "type=string,index=not_analyzed",
                            "folder", "type=string,index=not_analyzed",
                            "format", "type=string,index=not_analyzed",
                            "genre", "type=string,index=not_analyzed",
                            "albumArtist", "type=string,index=not_analyzed",
                            "albumName", "type=string,index=not_analyzed",
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

  /**
   *
   * @param queryName
   * @param vars
   * @return
   * @throws IOException
   * @throws TemplateException
   */
  public String getQuery(String queryName, Map<String,String> vars) throws IOException, TemplateException {

    Template template = queryTemplates.get(queryName);
    if (template == null) {
      synchronized (queryTemplates) {
        template = queryTemplates.get(queryName);
        if (template == null) {
          template = freeMarkerConfiguration.getTemplate(queryName + ".ftl");
          queryTemplates.put(queryName,template);
        }
      }
    }
    StringWriter result = new StringWriter();
    template.process(vars,result);

    return result.toString();
  }


  /**
   *
   * @param from
   * @param size
   * @return
   */
  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(String queryName,@Nullable Map<String,String> vars,
                                                                      @Nullable Integer from, @Nullable Integer size,Class<T> type) {
    String jsonQuery;
    try {
      jsonQuery = getQuery(queryName,vars);
    } catch (IOException |TemplateException e) {
      throw new RuntimeException(e);
    }

    return extractMediaFiles(jsonQuery, null, null,type);
  }


  /**
   *
   * @param jsonSearch
   * @param from
   * @param size
   * @return
   */
  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(String jsonSearch, @Nullable Integer from, @Nullable Integer size,Class<T> type) {
    SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(ElasticSearchDaoHelper.SUBSONIC_MEDIA_INDEX_NAME)
            .setQuery(jsonSearch).setVersion(true);
    return extractMediaFiles(searchRequestBuilder,from,size,type);
  }

  /**
   *
   * @param searchRequestBuilder
   * @param from
   * @param size
   * @return
   */
  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(SearchRequestBuilder searchRequestBuilder,
                                                                      @Nullable Integer from, @Nullable Integer size,Class<T> type) {
    if (from != null) {
      searchRequestBuilder.setFrom(from);
    }
    if (size != null) {
      searchRequestBuilder.setSize(size);
    }
    SearchResponse response = searchRequestBuilder.execute().actionGet();
    List<T> returnedSongs = Arrays.stream(response.getHits().getHits()).map(hit -> convertFromHit(hit,type)).collect(Collectors.toList());
    return returnedSongs;
  }

  /**
   *
   * @param hit
   * @return
   * @throws RuntimeException
   */
  public <T extends SubsonicESDomainObject> T convertFromHit(SearchHit hit, Class<T> type) throws RuntimeException {
    T object = null;

    if (hit != null) {
      String hitSource = hit.getSourceAsString();
      try {
        object = getMapper().readValue(hitSource,type);
        object.setESId(hit.id());
      } catch (IOException e) {
        throw new RuntimeException("Error while reading MediaFile object from index. ", e);
      }
    }
    return object;
  }

}
