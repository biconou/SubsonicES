package com.github.biconou.subsonic.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.sourceforge.subsonic.domain.MediaFile;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.istack.Nullable;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.slf4j.LoggerFactory;

/**
 * Created by remi on 26/04/2016.
 */
public class ElasticSearchDaoHelper {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ElasticSearchDaoHelper.class);

  public static final String MEDIA_FILE_INDEX_TYPE = "MEDIA_FILE";
  @Deprecated
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
    String[] musicFolders = indexNames();
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

          String[] indexNames = indexNames();
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
                              "name", "type=string,index=not_analyzed",
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

          // Create musicFolders index
          boolean indexExists = elasticSearchClient.admin().indices().prepareExists("musicfolders")
                  .execute().actionGet().isExists();
          if (!indexExists) {
            elasticSearchClient.admin().indices()
                    .prepareCreate("musicfolders")
                    .addMapping("MUSIC_FOLDER",
                            "path", "type=string,index=not_analyzed",
                            "name", "type=string,index=not_analyzed",
                            "enabled", "type=boolean",
                            "changed", "type=date")
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
   * @param folders
   * @return
   */
  public String[] indexNames(List<MusicFolder> folders) {

    List<MusicFolder> list;
    if (folders == null || folders.size() == 0) {
      list = musicFolderDao.getAllMusicFolders();
    } else {
      list = folders;
    }

    return list.stream().map(folder -> folder.getName().toLowerCase()).toArray(String[]::new);
  }

  public String[] indexNames() {
    return indexNames(null);
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

  /**
   *
   * @param hit
   * @return
   * @throws RuntimeException
   */
  private <T extends SubsonicESDomainObject> T convertFromHit(SearchHit hit, Class<T> type) throws RuntimeException {
    T object = null;

    if (hit != null) {
      String hitSource = hit.getSourceAsString();
      try {
        object = getMapper().readValue(hitSource,type);
        object.setESId(hit.id());
        object.setVersion((int)hit.getVersion());
      } catch (IOException e) {
        throw new RuntimeException("Error while reading MediaFile object from index. ", e);
      }
    }
    return object;
  }


  public void createObject(SubsonicESDomainObject obj, String indexName, boolean synchrone) {
    logger.debug("Object does not exist -> create");
    try {
      // Convert the object to a json string representation.
      String mediaFileAsJson = getMapper().writeValueAsString(obj);
      IndexResponse indexResponse = getClient().prepareIndex(
              indexName,
              ElasticSearchDaoHelper.MEDIA_FILE_INDEX_TYPE)
              .setSource(mediaFileAsJson).setVersionType(VersionType.INTERNAL).get();
      if (synchrone) {
        long l = 0;
        while (l == 0) {
          l = getClient().prepareSearch(indexName)
                  .setQuery(QueryBuilders.idsQuery().addIds(indexResponse.getId())).execute().actionGet().getHits().totalHits();
        }
      }

    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error trying indexing object " + e);
    }
  }

  public void updateObject(SubsonicESDomainObject allReadyExistsMediaFile, SubsonicESDomainObject obj, String indexName, boolean synchrone) {
    logger.debug("media files exists -> update");
    // update the media file.
    try {
      String esId = allReadyExistsMediaFile.getESId();
      int version = allReadyExistsMediaFile.getVersion();
      String json = getMapper().writeValueAsString(obj);
      UpdateResponse response = getClient().prepareUpdate(
              indexName,
              ElasticSearchDaoHelper.MEDIA_FILE_INDEX_TYPE, esId)
              .setDoc(json).setVersion(version).setVersionType(VersionType.INTERNAL)
              .get();
      if (synchrone) {
        long newVersion = version;
        while (newVersion == version) {
          newVersion = getClient().prepareSearch(indexName)
                  .setQuery(QueryBuilders.idsQuery().addIds(esId)).setVersion(true).execute().actionGet().getHits().getAt(0).version();
        }
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error trying indexing mediaFile " + e);
    }
  }




  public <T extends SubsonicESDomainObject> T extractUnique(String queryName, Map<String, String> vars, Class<T> type) {
    String jsonQuery;
    try {
      jsonQuery = getQuery(queryName,vars);
    } catch (IOException |TemplateException e) {
      throw new RuntimeException(e);
    }

    SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexNames())
            .setQuery(jsonQuery).setVersion(true);
    SearchResponse response = searchRequestBuilder.execute().actionGet();

    long totalHits = response == null ? 0 : response.getHits().totalHits();
    if (totalHits == 0) {
      return null;
    } else if (totalHits > 1) {
      throw new RuntimeException("Document is not unique "+type.getName()+" "+vars);
    } else {
      return convertFromHit(response.getHits().getHits()[0],type);
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
    SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(indexNames(musicFolders))
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


  public ElasticSearchDaoHelper getElasticSearchDaoHelper(MediaFileDao mediaFileDao) {
    return this;
  }
}
