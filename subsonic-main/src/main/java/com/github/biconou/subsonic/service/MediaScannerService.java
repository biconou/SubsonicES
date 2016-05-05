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
import net.sourceforge.subsonic.domain.Genres;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 */
public class MediaScannerService extends net.sourceforge.subsonic.service.MediaScannerService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);
    private QueueSender queueSender = null;


    /**
     * Constructor needed for Spring 2.5 without annotations
     * @return
     */
    public QueueSender getQueueSender() {
        return queueSender;
    }

    /**
     * Sets the queue sender
     * @param queueSender
     */
    public void setQueueSender(QueueSender queueSender) {
        this.queueSender = queueSender;
    }

    /**
     * Override behavior of scanFile with pre dans post process.
     * @param file
     * @param musicFolder
     * @param lastScanned
     * @param albumCount
     * @param genres
     * @param isPodcast
     */
    @Override
    protected void scanFile(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, Integer> albumCount, Genres genres, boolean isPodcast) {


        LOG.debug("BEGIN : scan file ["+file.getPath()+"]");

        super.scanFile(file, musicFolder, lastScanned, albumCount, genres, isPodcast);

        LOG.debug("END : scan file ["+file.getPath()+"]");

        /*
        if (file.getMediaType().equals(MediaFile.MediaType.VIDEO)) {
             getQueueSender().send(file);
        }
        */

    }
}
