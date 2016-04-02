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
import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.AlbumDao;
import net.sourceforge.subsonic.dao.ArtistDao;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlaylistService;
import net.sourceforge.subsonic.service.SearchService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.util.FileUtil;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.*;

/**
 * Provides services for scanning the music library.
 *
 * @author Sindre Mehus
 */
public class MediaScannerService extends net.sourceforge.subsonic.service.MediaScannerService {

    private static final int INDEX_VERSION = 15;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);
    private QueueSender queueSender = null;

    public QueueSender getQueueSender() {
        return queueSender;
    }

    public void setQueueSender(QueueSender queueSender) {
        this.queueSender = queueSender;
    }

    @Override
    protected void scanFile(MediaFile file, MusicFolder musicFolder, Date lastScanned, Map<String, Integer> albumCount, Genres genres, boolean isPodcast) {
        super.scanFile(file, musicFolder, lastScanned, albumCount, genres, isPodcast);

        if (file.getMediaType().equals(MediaFile.MediaType.VIDEO)) {
             getQueueSender().send("scanned : id="+file.getId()+" name="+file.getPath()+file.getName());
        }

    }
}
