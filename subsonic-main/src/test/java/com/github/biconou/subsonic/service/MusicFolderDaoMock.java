package com.github.biconou.subsonic.service;

import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.MusicFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by remi on 06/05/2016.
 */
public class MusicFolderDaoMock extends MusicFolderDao {

  private static String baseResources = "/MEDIAS/";

  public static String resolveBaseMediaPath() {
    String baseDir = MusicFolderDaoMock.class.getResource(baseResources).toString().replace("file:/","");
    return baseDir;
  }

  public static String resolveMusicFolderPath() {
    return (MusicFolderDaoMock.resolveBaseMediaPath() + "Music").replace("/","\\");
    //return "Z:\\musique\\PCDM";
  }

  @Override
  public List<MusicFolder> getAllMusicFolders() {

    List<MusicFolder> liste = new ArrayList<>();

    File musicDir = new File(MusicFolderDaoMock.resolveMusicFolderPath());

    MusicFolder musicFolder = new MusicFolder(musicDir,"Music",true,new Date());

    liste.add(musicFolder);
    return liste;
  }
}
