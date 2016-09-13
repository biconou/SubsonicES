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
    private List<DirectoryTreeWatcher> directoryTreeWatchers = new ArrayList<>();


    private static class MusicFolderChangedInotifywaitEventListener extends DefaultInotifywaitEventListener {

        private MediaScannerService mediaScannerService = null;
        private Map<String, Date> directoriesToBeScanned = new Hashtable<>(); // Note that HashTable is synchronized
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
            directoriesToBeScanned.put(event.getPath(), new Date());
            if (tDirectoriesToBeScanned == null) {
                tDirectoriesToBeScanned = new Thread(() -> {
                    while (directoriesToBeScanned.keySet().size() > 0) {
                        try {
                            Thread.sleep(timeToWait);
                        } catch (InterruptedException e) {
                            // Nothing to do
                        }
                        List<String> toScan = new ArrayList();
                        directoriesToBeScanned.keySet().forEach(path -> {
                            Date now = new Date();
                            if (now.compareTo(new Date(directoriesToBeScanned.get(path).getTime() + timeToWait)) > 0) {
                                toScan.add(path);
                            }
                        });
                        toScan.forEach(pathToScan -> {
                            mediaScannerService.scanDirectory(pathToScan);
                            directoriesToBeScanned.remove(pathToScan);
                        });
                    }
                    tDirectoriesToBeScanned = null;
                });
                tDirectoriesToBeScanned.start();
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
            directoryTreeWatchers.add(directoryTreeWatcher);
        });
    }


    @Override
    protected void doScanLibrary() {
        logger.info("Starting to scan media library.");

        try {

            scanning = true;
            directoryTreeWatchers.forEach(w -> w.stopWatch());

            Date lastScanned = new Date();

            // Maps from artist name to album count.
            Map<String, Integer> albumCount = new HashMap<String, Integer>();

            scanCount = 0;

            mediaFileService.setMemoryCacheEnabled(false);

            mediaFileService.clearMemoryCache();

            // Recurse through all files on disk.
            for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
                scanDirectory(musicFolder.getPath(), musicFolder, lastScanned, true);
            }

            // TODO traiter les podcasts
            // Scan podcast folder.
            /* File podcastFolder = new File(settingsService.getPodcastFolder());
            if (podcastFolder.exists()) {
              scanFile(mediaFileService.getMediaFile(podcastFolder), new MusicFolder(podcastFolder, null, true, null),
                lastScanned, albumCount, genres, true);
             } */

            logger.info("Scanned media library with " + scanCount + " entries.");


            settingsService.setLastScanned(lastScanned);
            settingsService.save(false);
            logger.info("Completed media library scan.");

        } catch (Throwable x) {
            logger.error("Failed to scan media library.", x);
        } finally {
            mediaFileService.setMemoryCacheEnabled(true);
            scanning = false;
            directoryTreeWatchers.forEach(w -> w.startWatch());
        }
    }


    /**
     * @param dirPath
     */
    public void scanDirectory(String dirPath) {

        logger.debug("BEGIN : scanDirectory [" + dirPath + "]");

        MusicFolder owningFolder = settingsService.getAllMusicFolders().stream().filter(folder -> {
            String folderPath = folder.getPath().getAbsolutePath();
            if (folderPath.contains("\\")) {
                folderPath += "\\";
            } else {
                folderPath += "/";
            }
            return dirPath.startsWith(folderPath);
        }).iterator().next();

        if (owningFolder == null) {
            logger.warn("The file [" + dirPath + "] does not belong to any music folder");
        } else {
            logger.info("Will scan [" + dirPath + "] that belongs to music folder [" + owningFolder.getName() + "]");
        }

        File file = new File(dirPath);
        if (!file.exists()) {
            logger.warn("File [" + dirPath + "] does not exist.");
        } else {
            scanDirectory(file, owningFolder, new Date(), false);
        }
    }

    /**
     * Override behavior of scanFile with pre dans post process.
     *
     * @param musicFolder
     * @param lastScanned
     */
    protected void scanDirectory(File dirToScan, MusicFolder musicFolder, Date lastScanned, boolean recursive) {

        // Finds the media file corresponding to file.
        // If it already exists in the the database, return it
        // otherwise create a new mediaFile object by reading the content of file.
        MediaFile dirMediaFile = mediaFileService.getMediaFile(dirToScan, false);

        logger.debug("BEGIN : scan directory [" + dirMediaFile.getPath() + "]");

        try {
            // Creates (or updates if it already exists) the media file in database.
            // TODO ici il ne faut pas créer le media file si c'est un album
            if (!dirMediaFile.isAlbum()) {
                mediaFileDao.createOrUpdateMediaFile(dirMediaFile);
            }

            // if mediafile is a directory ...
            if (dirMediaFile.isDirectory()) {

                // retrieve all the children of this directory
                List<File> children = mediaFileService.filterMediaFiles(FileUtil.listFiles(dirMediaFile.getFile()));

                // Recursively scan sub directories
                for (File child : children) {
                    if (child.isDirectory() && recursive) {
                        scanDirectory(child, musicFolder, lastScanned, recursive);
                    }
                }

                // take each file that is not a directory and create media file
                Album album = null; // The album to wich these child files belong to
                if (dirMediaFile.isAlbum()) {
                    album = new Album();
                    album.setPath(dirMediaFile.getPath());
                    album.setFolderId(musicFolder.getId());
                    album.setLastScanned(lastScanned);
                    album.setChanged(dirMediaFile.getChanged());
                    album.setCoverArtPath(dirMediaFile.getCoverArtPath());
                    album.setChildrenLastUpdated(dirMediaFile.getChildrenLastUpdated());
                    album.setFolder(dirMediaFile.getFolder());
                    album.setParentPath(dirMediaFile.getParentPath());
                    album.setDurationSeconds(0);
                    album.setSongCount(0);
                    album.setPresent(true);
                }


                for (File child : children) {
                    if (!child.isDirectory()) {
                        // create or update the mediafile
                        MediaFile childMediaFile = mediaFileService.createMediaFile(child);
                        mediaFileDao.createOrUpdateMediaFile(childMediaFile);

                        album = buildAlbum(album, childMediaFile, musicFolder, lastScanned);
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

                    } // if child is not a directory
                } // for all children
                if (album != null && album.getName() != null && album.getArtist() != null) {
                    // TODO modifier le create album pour que ça aille dans les media_file
                    albumDao.createOrUpdateAlbum(album);
                }
            } else {
                // if dirMediaFile is not a directory.
                throw new RuntimeException(dirMediaFile.getPath()+" must be a directory to be scanned");
            }
        } catch (Exception e) {
            logger.error("Error while library scanning. ", e);
        }

        logger.debug("END : scan directory [" + dirMediaFile.getPath() + "]");

        /*
        if (file.getMediaType().equals(MediaFile.MediaType.VIDEO)) {
             getQueueSender().send(file);
        }
        */

    }

    private Album buildAlbum(Album album, MediaFile childMediaFile, MusicFolder musicFolder, Date lastScanned) {
        if (album == null) {
            throw new RuntimeException("parameter album must not be null here");
        }
        if (album.getName() == null) {
            album.setName(childMediaFile.getAlbumName());
        }
        if (album.getArtist() == null) {
            album.setArtist(childMediaFile.getAlbumArtist() != null
                    ? childMediaFile.getAlbumArtist() : childMediaFile.getArtist());
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

        if (childMediaFile.getDurationSeconds() != null) {
            album.setDurationSeconds(album.getDurationSeconds() + childMediaFile.getDurationSeconds());
        }
        if (childMediaFile.isAudio()) {
            album.setSongCount(album.getSongCount() + 1);
        }
        return album;
    }


}
