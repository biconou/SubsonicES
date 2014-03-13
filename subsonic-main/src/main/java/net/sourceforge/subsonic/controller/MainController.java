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
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.domain.CoverArtScheme;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.AdService;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.RatingService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Controller for the main page.
 *
 * @author Sindre Mehus
 */
public class MainController extends ParameterizableViewController {

    private SecurityService securityService;
    private PlayerService playerService;
    private SettingsService settingsService;
    private RatingService ratingService;
    private MediaFileService mediaFileService;
    private AdService adService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        Player player = playerService.getPlayer(request, response);
        List<MediaFile> mediaFiles = getMediaFiles(request);

        if (mediaFiles.isEmpty()) {
            return new ModelAndView(new RedirectView("notFound.view"));
        }

        MediaFile dir = mediaFiles.get(0);
        if (dir.isFile()) {
            dir = mediaFileService.getParentOf(dir);
        }

        // Redirect if root directory.
        if (mediaFileService.isRoot(dir)) {
            return new ModelAndView(new RedirectView("home.view?"));
        }

        List<MediaFile> children = mediaFiles.size() == 1 ? mediaFileService.getChildrenOf(dir, true, true, true) : getMultiFolderChildren(mediaFiles);
        String username = securityService.getCurrentUsername(request);
        UserSettings userSettings = settingsService.getUserSettings(username);

        mediaFileService.populateStarredDate(dir, username);
        mediaFileService.populateStarredDate(children, username);

        map.put("dir", dir);
        map.put("ancestors", getAncestors(dir));
        map.put("children", children);
        map.put("artist", guessArtist(children));
        map.put("album", guessAlbum(children));
        map.put("player", player);
        map.put("sieblingCoverArtScheme", CoverArtScheme.SMALL);
        map.put("user", securityService.getCurrentUser(request));
        map.put("visibility", userSettings.getMainVisibility());
        map.put("showAlbumYear", settingsService.isSortAlbumsByYear());
        map.put("updateNowPlaying", request.getParameter("updateNowPlaying") != null);
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("brand", settingsService.getBrand());
        if (!settingsService.isLicenseValid()) {
            map.put("ad", adService.getAd());
        }

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            map.put("parent", parent);
            map.put("navigateUpAllowed", !mediaFileService.isRoot(parent));
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }

        Integer userRating = ratingService.getRatingForUser(username, dir);
        Double averageRating = ratingService.getAverageRating(dir);

        if (userRating == null) {
            userRating = 0;
        }

        if (averageRating == null) {
            averageRating = 0.0D;
        }

        map.put("userRating", 10 * userRating);
        map.put("averageRating", Math.round(10.0D * averageRating));
        map.put("starred", mediaFileService.getMediaFileStarredDate(dir.getId(), username) != null);

        CoverArtScheme scheme = player.getCoverArtScheme();
        if (scheme != CoverArtScheme.OFF) {
            List<MediaFile> coverArts = getCoverArts(dir, children);
            map.put("coverArts", coverArts);
            setSieblingAlbums(dir, map);
        }

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private List<MediaFile> getMediaFiles(HttpServletRequest request) {
        List<MediaFile> mediaFiles = new ArrayList<MediaFile>();
        for (String path : ServletRequestUtils.getStringParameters(request, "path")) {
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            }
        }
        for (int id : ServletRequestUtils.getIntParameters(request, "id")) {
            MediaFile mediaFile = mediaFileService.getMediaFile(id);
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            }
        }
        return mediaFiles;
    }

    private String guessArtist(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getArtist();
            }
        }
        return null;
    }

    private String guessAlbum(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getAlbumName();
            }
        }
        return null;
    }

    private List<MediaFile> getCoverArts(MediaFile dir, List<MediaFile> children) throws IOException {
        int limit = settingsService.getCoverArtLimit();
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }

        List<MediaFile> coverArts = new ArrayList<MediaFile>();
        if (dir.isAlbum()) {
            coverArts.add(dir);
        } else {
            for (MediaFile child : children) {
                if (child.isAlbum()) {
                    coverArts.add(child);
                    if (coverArts.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return coverArts;
    }

    private List<MediaFile> getMultiFolderChildren(List<MediaFile> mediaFiles) throws IOException {
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.isFile()) {
                mediaFile = mediaFileService.getParentOf(mediaFile);
            }
            result.addAll(mediaFileService.getChildrenOf(mediaFile, true, true, true));
        }
        return result;
    }

    private List<MediaFile> getAncestors(MediaFile dir) throws IOException {
        LinkedList<MediaFile> result = new LinkedList<MediaFile>();

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            while (parent != null && !mediaFileService.isRoot(parent)) {
                result.addFirst(parent);
                parent = mediaFileService.getParentOf(parent);
            }
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return result;
    }

    private void setSieblingAlbums(MediaFile dir, Map<String, Object> map) throws IOException {
        MediaFile parent = mediaFileService.getParentOf(dir);

        if (dir.isAlbum() && !mediaFileService.isRoot(parent)) {
            List<MediaFile> sieblings = mediaFileService.getChildrenOf(parent, false, true, true);
            sieblings.remove(dir);

            int limit = settingsService.getCoverArtLimit();
            if (limit == 0) {
                limit = Integer.MAX_VALUE;
            }
            if (sieblings.size() > limit) {
                sieblings = sieblings.subList(0, limit);
            }
            map.put("sieblingAlbums", sieblings);
        }
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setRatingService(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    public void setAdService(AdService adService) {
        this.adService = adService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
