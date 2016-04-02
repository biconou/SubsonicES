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
package net.sourceforge.subsonic.ajax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContextFactory;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.AlbumNotes;
import net.sourceforge.subsonic.domain.ArtistBio;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.domain.VideoConversion;
import net.sourceforge.subsonic.service.LastFmService;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.NetworkService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.VideoConversionService;
import net.sourceforge.subsonic.util.StringUtil;

/**
 * Provides miscellaneous AJAX-enabled services.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class MultiService {

    private static final Logger LOG = Logger.getLogger(MultiService.class);

    private NetworkService networkService;
    private MediaFileService mediaFileService;
    private LastFmService lastFmService;
    private SecurityService securityService;
    private SettingsService settingsService;
    private VideoConversionService videoConversionService;

    /**
     * Returns status for port forwarding and URL redirection.
     */
    public NetworkStatus getNetworkStatus() {
        NetworkService.Status portForwardingStatus = networkService.getPortForwardingStatus();
        NetworkService.Status urlRedirectionStatus = networkService.getURLRedirecionStatus();
        return new NetworkStatus(portForwardingStatus.getText(),
                                 portForwardingStatus.getDate(),
                                 urlRedirectionStatus.getText(),
                                 urlRedirectionStatus.getDate());
    }

    public ArtistInfo getArtistInfo(int mediaFileId, int maxSimilarArtists, int maxTopSongs) {
        MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
        List<SimilarArtist> similarArtists = getSimilarArtists(mediaFileId, maxSimilarArtists);
        ArtistBio artistBio = lastFmService.getArtistBio(mediaFile);
        List<TopSong> topSongs = getTopSongs(mediaFile, maxTopSongs);

        return new ArtistInfo(similarArtists, artistBio, topSongs);
    }

    public AlbumInfo getAlbumInfo(int mediaFileId, int maxSimilarArtists) {
        MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
        AlbumNotes albumNotes = lastFmService.getAlbumNotes(mediaFile);
        ArtistInfo artistInfo = getArtistInfo(mediaFileId, maxSimilarArtists, 0);
        return new AlbumInfo(artistInfo, albumNotes == null ? null : albumNotes.getNotes());
    }

    private List<TopSong> getTopSongs(MediaFile mediaFile, int limit) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        List<TopSong> result = new ArrayList<TopSong>();
        List<MediaFile> files = lastFmService.getTopSongs(mediaFile, limit, musicFolders);
        mediaFileService.populateStarredDate(files, username);
        for (MediaFile file : files) {
            result.add(new TopSong(file.getId(), file.getTitle(), file.getArtist(), file.getAlbumName(),
                                   file.getDurationString(), file.getStarredDate() != null));
        }
        return result;
    }

    private List<SimilarArtist> getSimilarArtists(int mediaFileId, int limit) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(username);

        MediaFile artist = mediaFileService.getMediaFile(mediaFileId);
        List<MediaFile> similarArtists = lastFmService.getSimilarArtists(artist, limit, false, musicFolders);
        SimilarArtist[] result = new SimilarArtist[similarArtists.size()];
        for (int i = 0; i < result.length; i++) {
            MediaFile similarArtist = similarArtists.get(i);
            result[i] = new SimilarArtist(similarArtist.getId(), similarArtist.getName());
        }
        return Arrays.asList(result);
    }

    public void setShowSideBar(boolean show) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(username);
        userSettings.setShowSideBar(show);
        userSettings.setChanged(new Date());
        settingsService.updateUserSettings(userSettings);
    }

    public void setSelectedMusicFolder(int musicFolderId) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));

        // Note: UserSettings.setChanged() is intentionally not called. This would break browser caching
        // of the index frame.
        settings.setSelectedMusicFolderId(musicFolderId);
        settingsService.updateUserSettings(settings);
    }

    public VideoConversionStatus getVideoConversionStatus(int mediaFileId) {
        VideoConversion conversion = videoConversionService.getVideoConversionForFile(mediaFileId);
        if (conversion == null) {
            return null;
        }
        VideoConversionStatus result = new VideoConversionStatus();
        result.setProgressSeconds(conversion.getProgressSeconds());
        result.setProgressString(StringUtil.formatDuration(conversion.getProgressSeconds()));

        switch (conversion.getStatus()) {
            case NEW:
                result.setStatusNew(true);
                break;
            case IN_PROGRESS:
                result.setStatusInProgress(true);
                break;
            case COMPLETED:
                result.setStatusCompleted(true);
                break;
            case ERROR:
                result.setStatusError(true);
                break;
            default:
                break;
        }
        return result;
    }

    public VideoConversionStatus startVideoConversion(int mediaFileId, Integer audioTrackId) {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        String username = securityService.getCurrentUsername(request);
        authorizeVideoConversion();

        VideoConversion conversion = new VideoConversion(null, mediaFileId, audioTrackId, username, VideoConversion.Status.NEW,
                                                         null, new Date(), new Date(), null);
        videoConversionService.createVideoConversion(conversion);

        return getVideoConversionStatus(mediaFileId);
    }

    public VideoConversionStatus cancelVideoConversion(int mediaFileId) {
        authorizeVideoConversion();
        VideoConversion conversion = videoConversionService.getVideoConversionForFile(mediaFileId);
        if (conversion != null) {
            videoConversionService.cancelVideoConversion(conversion);
        }

        return getVideoConversionStatus(mediaFileId);
    }

    private void authorizeVideoConversion() {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        User user = securityService.getCurrentUser(request);
        if (!user.isVideoConversionRole()) {
            LOG.warn("User " + user.getUsername() + " is not allowed to convert videos.");
            throw new RuntimeException("User " + user.getUsername() + " is not allowed to convert videos.");
        }
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setLastFmService(LastFmService lastFmService) {
        this.lastFmService = lastFmService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setVideoConversionService(VideoConversionService videoConversionService) {
        this.videoConversionService = videoConversionService;
    }
}