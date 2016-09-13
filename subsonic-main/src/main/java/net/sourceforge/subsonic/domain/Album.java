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
package net.sourceforge.subsonic.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.biconou.subsonic.dao.SubsonicESDomainObject;

import java.util.Date;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class Album implements SubsonicESDomainObject {

    private String ESId = null;

    private int id;
    private String path;
    private String name;
    private String artist;
    private int songCount;
    private int durationSeconds;
    private String coverArtPath;
    private Integer year;
    private String genre;
    private int playCount;
    private Date lastPlayed;
    private String comment;
    private Date created;
    private Date lastScanned;
    private boolean present;
    private Integer folderId;
    private Date changed;
    private Date childrenLastUpdated;
    private String folder;
    private String parentPath;
    private int version;



    public Album() {
    }

    public Album(int id, String path, String name, String artist, int songCount, int durationSeconds, String coverArtPath,
            Integer year, String genre, int playCount, Date lastPlayed, String comment, Date created, Date lastScanned,
            boolean present, Integer folderId) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.artist = artist;
        this.songCount = songCount;
        this.durationSeconds = durationSeconds;
        this.coverArtPath = coverArtPath;
        this.year = year;
        this.genre = genre;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.lastScanned = lastScanned;
        this.folderId = folderId;
        this.present = present;
    }

    public int getId() {
        return getPath().hashCode();
    }

    @Deprecated
    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getAlbumName() {
        return getName();
    }


    public void setName(String name) {
        this.name = name;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date changed) {
        this.changed = changed;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getCoverArtPath() {
        return coverArtPath;
    }

    public void setCoverArtPath(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Integer getFolderId() {
        return folderId;
    }

    @JsonIgnore
    public String getESId() {
        return ESId;
    }

    public void setESId(String ESId) {
        this.ESId = ESId;
    }

    public Date getChildrenLastUpdated() {
        return childrenLastUpdated;
    }

    public void setChildrenLastUpdated(Date childrenLastUpdated) {
        this.childrenLastUpdated = childrenLastUpdated;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public MediaFile.MediaType getMediaType() {
        return MediaFile.MediaType.ALBUM;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
