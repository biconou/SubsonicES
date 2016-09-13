package com.github.biconou.subsonic.dao;

import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AlbumDao extends net.sourceforge.subsonic.dao.AlbumDao {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AlbumDao.class);

  private ElasticSearchDaoHelper elasticSearchDaoHelper = null;

  private MusicFolderDao musicFolderDao = null;

  public void setMusicFolderDao(MusicFolderDao musicFolderDao) {
    this.musicFolderDao = musicFolderDao;
  }


  @Override
  public Album getAlbum(String artistName, String albumName) {
    Map<String,String> vars = new HashMap<>();
    vars.put("artist",artistName);
    vars.put("name",albumName);

    return elasticSearchDaoHelper.extractUnique("searchAlbumByArtistAndName",vars,Album.class);
  }

  /**
   *
   * @param
   * @return
   */
  private String resolveIndexNameForAlbum(Album album) {
    for (MusicFolder musicFolder : musicFolderDao.getAllMusicFolders()) {
      if (musicFolder.getId().equals(album.getFolderId())) {
        return musicFolder.getName().toLowerCase();
      }
    }
    return null;
  }


  @Override
  public synchronized void createOrUpdateAlbum(Album album) {

    String indexName = resolveIndexNameForAlbum(album);
    logger.debug("CreateOrUpdateAlbum for artist=["+album.getArtist()+"] and name=["+album.getName()+"] into index ["+indexName+"]");

    Album allReadyExistsAlbum = getAlbum(album.getArtist(),album.getName());

    if (allReadyExistsAlbum == null) {
      elasticSearchDaoHelper.createObject(album, indexName, false);
    }  else {
      elasticSearchDaoHelper.updateObject(allReadyExistsAlbum, album, indexName, false);
    }
  }

  public ElasticSearchDaoHelper getElasticSearchDaoHelper() {
    return elasticSearchDaoHelper;
  }

  public void setElasticSearchDaoHelper(ElasticSearchDaoHelper elasticSearchDaoHelper) {
    this.elasticSearchDaoHelper = elasticSearchDaoHelper;
  }

}
