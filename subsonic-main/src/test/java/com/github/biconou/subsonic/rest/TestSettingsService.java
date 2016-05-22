package com.github.biconou.subsonic.rest;

import com.github.biconou.subsonic.service.MusicFolderDaoMock;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.service.SettingsService;

import java.util.List;

/**
 * Created by remi on 22/05/2016.
 */
public class TestSettingsService extends SettingsService {

  @Override
  public List<MusicFolder> getMusicFoldersForUser(String username) {
    MusicFolderDaoMock musicFolderDaoMock = new MusicFolderDaoMock();
    return musicFolderDaoMock.getAllMusicFolders();
  }
}
