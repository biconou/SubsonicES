package com.github.biconou.subsonic.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.istack.Nullable;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MusicFolder;

/**
 * Created by remi on 26/04/2016.
 */
public class ElasticSearchDaoHelper {

  public static final String MEDIA_FILE_INDEX_TYPE = "MEDIA_FILE";
  public static final String ALBUM_INDEX_TYPE = "ALBUM";

  private MusicFolderDao musicFolderDao = null;

  private Client elasticSearchClient = null;
  private ObjectMapper mapper = new ObjectMapper();
  private Map<String,Template> queryTemplates = new HashMap<>();
  private Configuration freeMarkerConfiguration = null;

  public ElasticSearchDaoHelper() {
    freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_23);
    freeMarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/com/github/biconou/subsonic/dao");
  }

  public void setMusicFolderDao(MusicFolderDao musicFolderDao) {
    this.musicFolderDao = musicFolderDao;
  }

  private Client obtainESClient() {

    return TransportClient.builder().build()
             .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300));
    /* Map<String,String> settings = new HashMap<>();
    settings.put("path.home","/software/elasticsearch-2.3.3");
    Node node = NodeBuilder.nodeBuilder().client(true).settings(Settings.builder().put(settings)).node();
    return node.client(); */
  }

  public void deleteIndexes() {
    Client client = obtainESClient();
    String[] musicFolders = indexNames(musicFolderDao.getAllMusicFolders());
    for (String folder : musicFolders) {
      String indexName = folder;
      boolean indexExists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
      if (indexExists) {
        client.admin().indices().prepareDelete(indexName).execute().actionGet();
      }
    }
    elasticSearchClient = null;
  }

  public Client getClient() {
    if (elasticSearchClient == null) {
      synchronized (this) {
        if (elasticSearchClient == null) {
          elasticSearchClient = obtainESClient();

          String[] indexNames = indexNames(musicFolderDao.getAllMusicFolders());
          for (String indexName : indexNames) {
            boolean indexExists = elasticSearchClient.admin().indices().prepareExists(indexName)
                    .execute().actionGet().isExists();
            if (!indexExists) {
              elasticSearchClient.admin().indices()
                      .prepareCreate(indexName)
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
    }
    return elasticSearchClient;
  }

  public ObjectMapper getMapper() {
    return mapper;
  }


  /**
   *
   * @param folders
   * @return
   */
  public String[] indexNames(List<MusicFolder> folders) {
    if (folders != null) {
      return folders.stream().map(folder -> folder.getName().toLowerCase()).toArray(String[]::new);
    } else {
      return null;
    }
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

    if (vars != null) {
      vars.forEach((k, v) -> vars.put(k, escapeForJson(v)));
    }
    template.process(vars,result);

    return result.toString();
  }

  /**
   *
   * @param source
   * @return
   */
  private String escapeForJson(String source) {
    if (source == null) {
      return "";
    } else {
      String target = source.replace("\\", "\\\\");
      target = target.replace("\"", "\\\"");
      return target;
    }
  }



  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(String queryName, Map<String, String> vars, Class<T> type) {
    return extractMediaFiles(queryName, vars, null,null,null,type);
  }

  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(String queryName,@Nullable Map<String,String> vars,
                                                                      @Nullable Integer from, @Nullable Integer size,
                                                                      @Nullable Map<String,SortOrder> sortClause, Class<T> type) {
    return extractMediaFiles(queryName,vars,from,size,sortClause,null,type);
  }

  /**
   *
   * @param from
   * @param size
   * @return
   */
  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(String queryName, @Nullable Map<String,String> vars,
                                                                      @Nullable Integer from, @Nullable Integer size,
                                                                      @Nullable  Map<String,SortOrder> sortClause,
                                                                      @Nullable List<MusicFolder> musicFolders, Class<T> type) {
    String jsonQuery;
    try {
      jsonQuery = getQuery(queryName,vars);
    } catch (IOException |TemplateException e) {
      throw new RuntimeException(e);
    }

    return extractMediaFiles(jsonQuery, from, size, sortClause, musicFolders,type);
  }




  /**
   *
   * @param jsonSearch
   * @param from
   * @param size
   * @return
   */
  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(String jsonSearch, @Nullable Integer from, @Nullable Integer size,
                                                                      @Nullable Map<String,SortOrder> sortClause,
                                                                      @Nullable List<MusicFolder> musicFolders,Class<T> type) {
    List<MusicFolder> list = null;
    if (musicFolders == null) {
      list = musicFolderDao.getAllMusicFolders();
    } else {
      list = musicFolders;
    }
    SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexNames(list))
            .setQuery(jsonSearch).setVersion(true);
    return extractMediaFiles(searchRequestBuilder,from,size,sortClause,type);
  }

  /**
   *
   * @param searchRequestBuilder
   * @param from
   * @param size
   * @return
   */
  public <T extends SubsonicESDomainObject> List<T> extractMediaFiles(SearchRequestBuilder searchRequestBuilder,
                                                                      @Nullable Integer from, @Nullable Integer size,
                                                                      @Nullable Map<String,SortOrder> sortClause, Class<T> type) {
    if (from != null) {
      searchRequestBuilder.setFrom(from);
    }
    if (size != null) {
      searchRequestBuilder.setSize(size);
    }
    if (sortClause != null) {
      sortClause.keySet().forEach(sortField -> searchRequestBuilder.addSort(sortField,sortClause.get(sortField)));
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
