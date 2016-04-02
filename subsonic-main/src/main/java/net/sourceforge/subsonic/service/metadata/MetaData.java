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
package net.sourceforge.subsonic.service.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * Contains meta-data (song title, artist, album etc) for a music file.
 * @author Sindre Mehus
 */
public class MetaData {

    private Integer discNumber;
    private Integer trackNumber;
    private String title;
    private String artist;
    private String albumArtist;
    private String albumName;
    private String genre;
    private Integer year;
    private Integer bitRate;
    private boolean variableBitRate;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private final List<Track> tracks = new ArrayList<Track>(); // Only populated by FFmpegParser.

    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public void setBitRate(Integer bitRate) {
        this.bitRate = bitRate;
    }

    public boolean getVariableBitRate() {
        return variableBitRate;
    }

    public void setVariableBitRate(boolean variableBitRate) {
        this.variableBitRate = variableBitRate;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }

    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    public List<Track> getAudioTracks() {
        return FluentIterable.from(getTracks())
                             .filter(new Predicate<Track>() {
                                 @Override
                                 public boolean apply(Track input) {
                                     return input.isAudio();
                                 }
                             })
                             .toList();
    }

    public List<Track> getVideoTracks() {
        return FluentIterable.from(getTracks())
                             .filter(new Predicate<Track>() {
                                 @Override
                                 public boolean apply(Track input) {
                                     return input.isVideo();
                                 }
                             })
                             .toList();
    }
}
