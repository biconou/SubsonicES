/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package com.github.biconou.subsonic.service;

import com.github.biconou.inotify.DefaultInotifywaitEventListener;
import com.github.biconou.inotify.DirectoryTreeWatcher;
import com.github.biconou.inotify.InotifywaitEvent;
import com.github.biconou.service.media.scan.QueueSender;
import net.sourceforge.subsonic.domain.Album;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.util.FileUtil;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 */
public class MediaScannerService extends net.sourceforge.subsonic.service.MediaScannerService {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MediaScannerService.class);
  private QueueSender queueSender = null;


    private static class MusicFolderChangedInotifywaitEventListener extends DefaultInotifywaitEventListener {

        private MediaScannerService mediaScannerService = null;
        private Map<String,Date> directoriesToBeScanned = new Hashtable<>();
        Thread tDirectoriesToBeScanned = null;
        private final static long timeToWait = 60000;

        public MusicFolderChangedInotifywaitEventListener(MediaScannerService mediaScannerService) {
            this.mediaScannerService = mediaScannerService;
        }

        @Override
        public void doModify(InotifywaitEvent event) {
            registerDirectoryToScan(event);
        }

        @Override
        public void doCreate(InotifywaitEvent event) {
            registerDirectoryToScan(event);
        }

        private void registerDirectoryToScan(InotifywaitEvent event) {
            synchronized (directoriesToBeScanned) {
                directoriesToBeScanned.put(event.getPath(),new Date());
                if (tDirectoriesToBeScanned == null) {
                    tDirectoriesToBeScanned = new Thread(() -> {
                        while (directoriesToBeScanned.keySet().size() > 0) {
                            try {
                                Thread.sleep(timeToWait);
                            } catch (InterruptedException e) {
                                // Nothing to do
                            }
                            directoriesToBeScanned.keySet().forEach(path -> {
                                Date now = new Date();
                                if (now.compareTo(new Date(directoriesToBeScanned.get(path).getTime()+timeToWait)) > 0) {
                                    mediaScannerService.scanDirectory(path);
                                    synchronized (directoriesToBeScanned) {
                                        directoriesToBeScanned.remove(path);
                                    }
                                }
                            });
                        }
                        tDirectoriesToBeScanned = null;
                    });
                    tDirectoriesToBeScanned.start();
                }
            }
        }
    }

  /**
   * Constructor needed for Spring 2.5 without annotations
   *
   * @return
   */
  public QueueSender getQueueSender() {
    return queueSender;
  }

  /**
   * Sets the queue sender
   *
   * @param queueSender
   */
  public void setQueueSender(QueueSender queueSender) {
    this.queueSender = queueSender;
  }


    @Override
    public void init() {

        // Watch each music folder directory
        settingsService.getAllMusicFolders().forEach(musicFolder -> {
            String folderPath = musicFolder.getPath().getAbsolutePath();

            DirectoryTreeWatcher directoryTreeWatcher = new DirectoryTreeWatcher(folderPath)
                    .addEventListener(new MusicFolderChangedInotifywaitEventListener(this));
            directoryTreeWatcher.startWatch();
        });
    }


    @Override
  protected void doScanLibrary() {
    logger.info("Starting to scan media library.");

    try {
      Date lastScanned = new Date();

      // Maps from artist name to album count.
      Map<String, Integer> albumCount = new HashMap<String, Integer>();

      scanCount = 0;
      statistics.reset();

      mediaFileService.setMemoryCacheEnabled(false);

      mediaFileService.clearMemoryCache();

      // Recurse through all files on disk.
      for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
        scanFileOrDirectory(musicFolder.getPath(), musicFolder, lastScanned);
      }

      // TODO traiter les podcasts
      // Scan podcast folder.
      /* File podcastFolder = new File(settingsService.getPodcastFolder());
      if (podcastFolder.exists()) {
        scanFile(mediaFileService.getMediaFile(podcastFolder), new MusicFolder(podcastFolder, null, true, null),
          lastScanned, albumCount, genres, true);
      } */

      logger.info("Scanned media library with " + scanCount + " entries.");

      // Update statistics
      statistics.incrementArtists(albumCount.size());
      for (Integer albums : albumCount.values()) {
        statistics.incrementAlbums(albums);
      }

      settingsService.setMediaLibraryStatistics(statistics);
      settingsService.setLastScanned(lastScanned);
      settingsService.save(false);
      logger.info("Completed media library scan.");

    } catch (Throwable x) {
      logger.error("Failed to scan media library.", x);
    } finally {
      mediaFileService.setMemoryCacheEnabled(true);
      scanning = false;
    }
  }


  /**
   *
   * @param dirPath
   */
  public void scanDirectory(String dirPath) {

    logger.debug("BEGIN : scanDirectory [" + dirPath + "]");

    MusicFolder owningFolder = settingsService.getAllMusicFolders().stream().filter(folder -> {
      String folderPath = folder.getPath().getAbsolutePath();
      if (folderPath.contains("\\")) {
        folderPath += "\\";
      }
      else {
        folderPath += "/";
      }
      return dirPath.startsWith(folderPath);
    }).iterator().next();

    if (owningFolder == null) {
      logger.warn("The file [" + dirPath + "] does not belong to any music folder");
    }
    else {
      logger.info("Will scan [" + dirPath + "] that belongs to music folder [" + owningFolder.getName() + "]");
    }

    File file = new File(dirPath);
    if (!file.exists()) {
      logger.warn("File [" + dirPath + "] does not exist.");
    }
    else {
      scanFileOrDirectory(file,owningFolder,new Date());
    }
  }

  /**
   * Override behavior of scanFile with pre dans post process.
   *
   * @param musicFolder
   * @param lastScanned
   */
  protected void scanFileOrDirectory(File file, MusicFolder musicFolder, Date lastScanned) {

    MediaFile mediaFile = mediaFileService.getMediaFile(file, false);

    logger.debug("BEGIN : scan directory [" + mediaFile.getPath() + "]");

    try {
      mediaFileDao.createOrUpdateMediaFile(mediaFile);

      if (mediaFile.isDirectory()) {
        List<File> children = mediaFileService.filterMediaFiles(FileUtil.listFiles(mediaFile.getFile()));

        // Recursively scan sub directories
        for (File child : children) {
          if (child.isDirectory()) {
            scanFileOrDirectory(child, musicFolder, lastScanned);
          }
        }

        // create media files and album
        Album album = null;
        boolean firstEncounter = true;
        for (File child : children) {
          if (!child.isDirectory()) {
            MediaFile childMediaFile = mediaFileService.createMediaFile(child);
            mediaFileDao.createOrUpdateMediaFile(childMediaFile);
            String artist = childMediaFile.getAlbumArtist() != null ? childMediaFile.getAlbumArtist() : childMediaFile.getArtist();
            if (album == null) {
              album = new Album();
              album.setPath(childMediaFile.getParentPath());
              album.setFolderId(musicFolder.getId());
            }
            if (album.getName() == null) {
              album.setName(childMediaFile.getAlbumName());
            }
            if (album.getArtist() == null) {
              album.setArtist(artist);
            }
            if (album.getCreated() == null) {
              album.setCreated(childMediaFile.getChanged());
            }

            if (album.getYear() == null) {
              album.setYear(childMediaFile.getYear());
            }
            if (album.getGenre() == null) {
              album.setGenre(childMediaFile.getGenre());
            }

            if (firstEncounter) {
              album.setDurationSeconds(0);
              album.setSongCount(0);
            }
            if (mediaFile.getDurationSeconds() != null) {
              album.setDurationSeconds(album.getDurationSeconds() + mediaFile.getDurationSeconds());
            }
            if (mediaFile.isAudio()) {
              album.setSongCount(album.getSongCount() + 1);
            }
            album.setLastScanned(lastScanned);
            album.setPresent(true);
            // TODO g�rer cover art
        /* MediaFile parent = mediaFileService.getParentOf(file);
        if (parent != null && parent.getCoverArtPath() != null) {
            album.setCoverArtPath(parent.getCoverArtPath());
        } */
            // TODO on fait pas �a. Qu'est-ce que �a implique ?
            // Update the file's album artist, if necessary.
                /* if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
                    file.setAlbumArtist(album.getArtist());
                    mediaFileDao.createOrUpdateMediaFile(file);
                } */

            firstEncounter = false;
          }
        }
        if (album != null && album.getName() != null && album.getArtist() != null) {
          albumDao.createOrUpdateAlbum(album);
        }
      }
    } catch (Exception e) {
      logger.error("Error while library scanning. ",e);
    }

    logger.debug("END : scan directory [" + mediaFile.getPath() + "]");

        /*
        if (file.getMediaType().equals(MediaFile.MediaType.VIDEO)) {
             getQueueSender().send(file);
        }
        */

  }


}
