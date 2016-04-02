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

import net.sourceforge.subsonic.command.UserSettingsCommand;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.TranscodeScheme;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.util.Util;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.mvc.SimpleFormController;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Controller for the page used to administrate users.
 *
 * @author Sindre Mehus
 */
public class UserSettingsController extends SimpleFormController {

    private SecurityService securityService;
    private SettingsService settingsService;
    private TranscodingService transcodingService;

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        UserSettingsCommand command = new UserSettingsCommand();

        User user = getUser(request);
        if (user != null) {
            command.setUser(user);
            command.setEmail(user.getEmail());
            command.setAdmin(User.USERNAME_ADMIN.equals(user.getUsername()));
            UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
            command.setTranscodeSchemeName(userSettings.getTranscodeScheme().name());

        } else {
            command.setNewUser(true);
            command.setStreamRole(true);
            command.setSettingsRole(true);
        }

        command.setUsers(securityService.getAllUsers());
        command.setTranscodingSupported(transcodingService.isDownsamplingSupported(null));
        command.setTranscodeDirectory(transcodingService.getTranscodeDirectory().getPath());
        command.setTranscodeSchemes(TranscodeScheme.values());
        command.setLdapEnabled(settingsService.isLdapEnabled());
        command.setAllMusicFolders(settingsService.getAllMusicFolders());
        command.setAllowedMusicFolderIds(Util.toIntArray(getAllowedMusicFolderIds(user)));

        return command;
    }

    private User getUser(HttpServletRequest request) throws ServletRequestBindingException {
        Integer userIndex = ServletRequestUtils.getIntParameter(request, "userIndex");
        if (userIndex != null) {
            List<User> allUsers = securityService.getAllUsers();
            if (userIndex >= 0 && userIndex < allUsers.size()) {
                return allUsers.get(userIndex);
            }
        }
        return null;
    }

    private List<Integer> getAllowedMusicFolderIds(User user) {
        List<Integer> result = new ArrayList<Integer>();
        List<MusicFolder> allowedMusicFolders = user == null
                                                ? settingsService.getAllMusicFolders()
                                                : settingsService.getMusicFoldersForUser(user.getUsername());

        for (MusicFolder musicFolder : allowedMusicFolders) {
            result.add(musicFolder.getId());
        }
        return result;
    }

    @Override
    protected void doSubmitAction(Object comm) throws Exception {
        UserSettingsCommand command = (UserSettingsCommand) comm;

        if (command.isDeleteUser()) {
            deleteUser(command);
        } else if (command.isNewUser()) {
            createUser(command);
        } else {
            updateUser(command);
        }
        resetCommand(command);
    }

    private void deleteUser(UserSettingsCommand command) {
        securityService.deleteUser(command.getUsername());
    }

    public void createUser(UserSettingsCommand command) {
        User user = new User(command.getUsername(), command.getPassword(), StringUtils.trimToNull(command.getEmail()));
        user.setLdapAuthenticated(command.isLdapAuthenticated());
        securityService.createUser(user);
        updateUser(command);
    }

    public void updateUser(UserSettingsCommand command) {
        User user = securityService.getUserByName(command.getUsername());
        user.setEmail(StringUtils.trimToNull(command.getEmail()));
        user.setLdapAuthenticated(command.isLdapAuthenticated());
        user.setAdminRole(command.isAdminRole());
        user.setDownloadRole(command.isDownloadRole());
        user.setUploadRole(command.isUploadRole());
        user.setCoverArtRole(command.isCoverArtRole());
        user.setCommentRole(command.isCommentRole());
        user.setPodcastRole(command.isPodcastRole());
        user.setStreamRole(command.isStreamRole());
        user.setJukeboxRole(command.isJukeboxRole());
        user.setSettingsRole(command.isSettingsRole());
        user.setShareRole(command.isShareRole());
        user.setVideoConversionRole(command.isVideoConversionRole());

        if (command.isPasswordChange()) {
            user.setPassword(command.getPassword());
        }

        securityService.updateUser(user);

        UserSettings userSettings = settingsService.getUserSettings(command.getUsername());
        userSettings.setTranscodeScheme(TranscodeScheme.valueOf(command.getTranscodeSchemeName()));
        userSettings.setChanged(new Date());
        settingsService.updateUserSettings(userSettings);

        List<Integer> allowedMusicFolderIds = Util.toIntegerList(command.getAllowedMusicFolderIds());
        settingsService.setMusicFoldersForUser(command.getUsername(), allowedMusicFolderIds);
    }

    private void resetCommand(UserSettingsCommand command) {
        command.setUser(null);
        command.setUsers(securityService.getAllUsers());
        command.setDeleteUser(false);
        command.setPasswordChange(false);
        command.setNewUser(true);
        command.setStreamRole(true);
        command.setSettingsRole(true);
        command.setPassword(null);
        command.setConfirmPassword(null);
        command.setEmail(null);
        command.setTranscodeSchemeName(null);
        command.setAllMusicFolders(settingsService.getAllMusicFolders());
        command.setAllowedMusicFolderIds(Util.toIntArray(getAllowedMusicFolderIds(null)));
        command.setToast(true);
        command.setReload(true);
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }
}
