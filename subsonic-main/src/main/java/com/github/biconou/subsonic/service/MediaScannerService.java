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

import com.github.biconou.service.media.scan.QueueSender;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.service.metadata.JaudiotaggerParser;
import net.sourceforge.subsonic.service.metadata.MetaData;
import net.sourceforge.subsonic.service.metadata.MetaDataParser;
import net.sourceforge.subsonic.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static net.sourceforge.subsonic.domain.MediaFile.MediaType.ALBUM;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.DIRECTORY;

/**
 */
public class MediaScannerService extends net.sourceforge.subsonic.service.MediaScannerService {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);
  private QueueSender queueSender = null;


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

  /**
   * Override behavior of scanFile with pre dans post process.
   *
   * @param file
   * @param musicFolder
   * @param lastScanned
   * @param albumCount
   * @param genres
   * @param isPodcast
   */
  @Override
  protected void scanFile(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, Integer> albumCount, Genres genres, boolean isPodcast) {


    LOG.debug("BEGIN : scan file [" + file.getPath() + "]");

    mediaFileDao.createOrUpdateMediaFile(file);

    if (file.isDirectory()) {
      List<File> children = mediaFileService.filterMediaFiles(FileUtil.listFiles(file.getFile()));

      // Recursively scan sub directories
      for (File child : children) {
        if (child.isDirectory()) {
          MediaFile childMediaFile = mediaFileService.createMediaFile(child);
          scanFile(childMediaFile, musicFolder, lastScanned, albumCount, genres, isPodcast);
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
            album.setCreated(childMediaFile.getChanged()) ;
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
          if (file.getDurationSeconds() != null) {
            album.setDurationSeconds(album.getDurationSeconds() + file.getDurationSeconds());
          }
          if (file.isAudio()) {
            album.setSongCount(album.getSongCount() + 1);
          }
          album.setLastScanned(lastScanned);
          album.setPresent(true);
          // TODO gérer cover art
        /* MediaFile parent = mediaFileService.getParentOf(file);
        if (parent != null && parent.getCoverArtPath() != null) {
            album.setCoverArtPath(parent.getCoverArtPath());
        } */
          // TODO on fait pas ça. Qu'est-ce que ça implique ?
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


    LOG.debug("END : scan file [" + file.getPath() + "]");

        /*
        if (file.getMediaType().equals(MediaFile.MediaType.VIDEO)) {
             getQueueSender().send(file);
        }
        */

  }
}
