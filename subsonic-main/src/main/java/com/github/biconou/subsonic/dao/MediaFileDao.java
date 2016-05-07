package com.github.biconou.subsonic.dao;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.biconou.dao.ElasticSearchClient;
import net.sourceforge.subsonic.domain.Genre;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Created by remi on 26/04/2016.
 */
public class MediaFileDao extends net.sourceforge.subsonic.dao.MediaFileDao {


  private ElasticSearchClient elasticSearchClient = null;

  @Override
  public MediaFile getMediaFile(String path) {
    SearchResponse response = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchClient(), path);

    long nbFound = response.getHits().getTotalHits();
    if (nbFound > 1) {
      throw new RuntimeException("Index incoherence. more than one mediaFile found for patch"+path);
    }

    if (nbFound == 0) {
      return null;
    } else {
      return MediaFileDaoUtils.convertFromHit(getElasticSearchClient(),response.getHits().getHits()[0]);
    }
  }


  @Override
  // TODO
  public MediaFile getMediaFile(int id) {
    // Appel de la classe parente
    return super.getMediaFile(id);
  }

  @Override
  public List<MediaFile> getChildrenOf(String path) {
   String jsonSearch = "{\"query\" : {\n" +
           "\t\"bool\" : {\n" +
           "\t\t\"filter\" : {\n" +
           "\t\t\t\"term\" : {\"parentPath\" : \""+MediaFileDaoUtils.preparePathForSearch(path)+"\"}\n" +
           "\t\t },\n" +
           "\t\t\"filter\" : {\n" +
           "\t\t\t\"term\" : {\"present\" : \"true\"}\n" +
           "\t\t}\n" +
           "\t}\n" +
           "}}";

    return MediaFileDaoUtils.extractMediaFiles(getElasticSearchClient(),jsonSearch,null,null);
  }

  @Override
  public List<MediaFile> getFilesInPlaylist(int playlistId) {
    // Appel de la classe parente
    return super.getFilesInPlaylist(playlistId);
  }

  @Override
  public List<MediaFile> getSongsForAlbum(String artist, String album) {
    // Appel de la classe parente
    return super.getSongsForAlbum(artist, album);
  }

  @Override
  public List<MediaFile> getVideos(int count, int offset, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getVideos(count, offset, musicFolders);
  }

  @Override
  public MediaFile getArtistByName(String name, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getArtistByName(name, musicFolders);
  }

  @Override
  public void deleteMediaFile(String path) {
    // Appel de la classe parente
    super.deleteMediaFile(path);
  }

  @Override
  public List<Genre> getGenres(boolean sortByAlbum) {
    // Appel de la classe parente
    return super.getGenres(sortByAlbum);
  }

  @Override
  public void updateGenres(List<Genre> genres) {
    // Appel de la classe parente
    super.updateGenres(genres);
  }

  @Override
  public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
  }

  @Override
  public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
  }

  @Override
  public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getNewestAlbums(offset, count, musicFolders);
  }

  @Override
  public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getAlphabeticalAlbums(offset, count, byArtist, musicFolders);
  }

  @Override
  public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
  }

  @Override
  public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getAlbumsByGenre(offset, count, genre, musicFolders);
  }


  /**
   *
   * @param genre
   * @param offset first offset is 0
   * @param count unlimited is 0
   * @param musicFolders
   * @return
   */
  @Override
  public List<MediaFile> getSongsByGenre(String genre, int offset, int count, List<MusicFolder> musicFolders) {

    // TODO traiter le multi folder
    if (musicFolders.isEmpty()) {
      return Collections.emptyList();
    }

    // TODO reprendre le code de searchService
    String jsonSearch = "{\n" +
            "\t\"constant_score\" : {\n" +
            "\t\t\"filter\" : { \n" +
            "\t\t\t\"bool\" : {\n" +
            "\t\t\t\t\"must\" : {\n" +
            "\t\t\t\t\t\"term\" : { \"genre\" : \""+genre+"\" }           \n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"should\" : [\n" +
            "\t\t\t\t\t{\"type\" : { \"value\" : \"MUSIC\" }},\n" +
            "\t\t\t\t\t{\"type\" : { \"value\" : \"PODCAST\" }},\n" +
            "\t\t\t\t\t{\"type\" : { \"value\" : \"AUDIOBOOK\" }}\n" +
            "\t\t\t\t]\n" +
            "\t\t\t\t\n" +
            "\t\t\t}\n" +
            "\t\t} \n" +
            "\t }\n" +
            "\t}";

    Integer size = null;
    if (count > 0) {
      size = count;
    }

    return MediaFileDaoUtils.extractMediaFiles(getElasticSearchClient(),jsonSearch,offset,size);
  }

  @Override
  public List<MediaFile> getSongsByArtist(String artist, int offset, int count) {
    // Appel de la classe parente
    return super.getSongsByArtist(artist, offset, count);
  }

  @Override
  public MediaFile getSongByArtistAndTitle(String artist, String title, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getSongByArtistAndTitle(artist, title, musicFolders);
  }

  @Override
  public List<MediaFile> getStarredAlbums(int offset, int count, String username, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getStarredAlbums(offset, count, username, musicFolders);
  }

  @Override
  public List<MediaFile> getStarredDirectories(int offset, int count, String username, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getStarredDirectories(offset, count, username, musicFolders);
  }

  @Override
  public List<MediaFile> getStarredFiles(int offset, int count, String username, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getStarredFiles(offset, count, username, musicFolders);
  }

  @Override
  public int getAlbumCount(List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getAlbumCount(musicFolders);
  }

  @Override
  public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getPlayedAlbumCount(musicFolders);
  }

  @Override
  public int getStarredAlbumCount(String username, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getStarredAlbumCount(username, musicFolders);
  }

  @Override
  public void starMediaFile(int id, String username) {
    // Appel de la classe parente
    super.starMediaFile(id, username);
  }

  @Override
  public void unstarMediaFile(int id, String username) {
    // Appel de la classe parente
    super.unstarMediaFile(id, username);
  }

  @Override
  public Date getMediaFileStarredDate(int id, String username) {
    // Appel de la classe parente
    return super.getMediaFileStarredDate(id, username);
  }

  @Override
  public void markPresent(String path, Date lastScanned) {
    // Appel de la classe parente
    super.markPresent(path, lastScanned);
  }

  @Override
  public void markNonPresent(Date lastScanned) {
    // Appel de la classe parente
    super.markNonPresent(lastScanned);
  }

  @Override
  public void expunge() {
    // Appel de la classe parente
    super.expunge();
  }

  @Override
  public List<String> getArtistNames() {
    // Appel de la classe parente
    return super.getArtistNames();
  }

  @Override
    public synchronized void createOrUpdateMediaFile(MediaFile file) {

          //MediaFile existingMediaFile = getMediaFile(file.getPath());
          SearchResponse searchResponse = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchClient(), file.getPath());

          if (searchResponse.getHits().totalHits() == 0) {
            try {
              String json = getElasticSearchClient().getMapper().writeValueAsString(file);
              IndexResponse indexResponse = getElasticSearchClient().getClient().prepareIndex(
                      ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME,
                      file.getMediaType().toString())
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
              String json = getElasticSearchClient().getMapper().writeValueAsString(file);
              UpdateResponse response = getElasticSearchClient().getClient().prepareUpdate(
                      ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME,
                      file.getMediaType().toString(), id)
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
