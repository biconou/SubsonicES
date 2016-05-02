package com.github.biconou.subsonic.dao;


import java.util.Date;
import java.util.List;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.biconou.dao.ElasticSearchClient;
import com.github.biconou.dao.MediaFileDaoUtils;
import net.sourceforge.subsonic.domain.Genre;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;

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
  public MediaFile getMediaFile(int id) {
    // Appel de la classe parente
    return super.getMediaFile(id);
  }

  @Override
  public List<MediaFile> getChildrenOf(String path) {
    // Appel de la classe parente
    return super.getChildrenOf(path);
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

  @Override
  public List<MediaFile> getSongsByGenre(String genre, int offset, int count, List<MusicFolder> musicFolders) {
    // Appel de la classe parente
    return super.getSongsByGenre(genre, offset, count, musicFolders);
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

          SearchResponse alreadyIndexedMediaFileResponse = MediaFileDaoUtils.searchMediaFileByPath(getElasticSearchClient(), file.getPath());

          long nbFound = alreadyIndexedMediaFileResponse.getHits().getTotalHits();

          if (nbFound > 1) {
            throw new RuntimeException("Index incoherence. more than one mediaFile found for patch"+file.getPath());
          }

          if (nbFound == 0) {
            try {
              String json = getElasticSearchClient().getMapper().writeValueAsString(file);
              IndexResponse response = getElasticSearchClient().getClient().prepareIndex(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME, file.getMediaType().toString())
                .setSource(json)
                .get();

            } catch (JsonProcessingException e) {
              throw new RuntimeException("Error trying indexing mediaFile "+e);
            }
          }  else {
            // update the media file.
            try {
              String json = getElasticSearchClient().getMapper().writeValueAsString(file);
              UpdateResponse response = getElasticSearchClient().getClient().prepareUpdate(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME, file.getMediaType().toString(), "" + file.getId())
                .setUpsert(json)
                .get();

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
