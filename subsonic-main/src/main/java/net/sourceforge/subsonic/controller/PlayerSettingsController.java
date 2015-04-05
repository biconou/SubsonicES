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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.subsonic.command.PlayerSettingsCommand;
import net.sourceforge.subsonic.command.PlayerSettingsCommand.MusicFolderRef;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.PlayerTechnology;
import net.sourceforge.subsonic.domain.TranscodeScheme;
import net.sourceforge.subsonic.domain.Transcoding;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.TranscodingService;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * Controller for the player settings page.
 *
 * @author Sindre Mehus
 */
public class PlayerSettingsController extends SimpleFormController {

    private PlayerService playerService;
    private SecurityService securityService;
    private TranscodingService transcodingService;
    private SettingsService settingsService;


    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {

        handleRequestParameters(request);
        List<Player> players = getPlayers(request);

        User user = securityService.getCurrentUser(request);
        PlayerSettingsCommand command = new PlayerSettingsCommand();
        Player player = null;
        String playerId = request.getParameter("id");
        if (playerId != null) {
            player = playerService.getPlayerById(playerId);
        } else if (!players.isEmpty()) {
            player = players.get(0);
        }

        command.setMusicFolders(wrap(settingsService.getAllMusicFolders(true, true)));

        if (player != null) {
            command.setPlayerId(player.getId());
            command.setName(player.getName());
            command.setDescription(player.toString());
            command.setType(player.getType());
            command.setLastSeen(player.getLastSeen());
            command.setDynamicIp(player.isDynamicIp());
            command.setAutoControlEnabled(player.isAutoControlEnabled());
            command.setTranscodeSchemeName(player.getTranscodeScheme().name());
            command.setTechnologyName(player.getTechnology().name());
            command.setAllTranscodings(transcodingService.getAllTranscodings());
            List<Transcoding> activeTranscodings = transcodingService.getTranscodingsForPlayer(player);
            int[] activeTranscodingIds = new int[activeTranscodings.size()];
            for (int i = 0; i < activeTranscodings.size(); i++) {
                activeTranscodingIds[i] = activeTranscodings.get(i).getId();
            }
            command.setActiveTranscodingIds(activeTranscodingIds);
            
            if (player.getTechnology().equals(PlayerTechnology.CMUS)) {
                command.setCmusIP(player.getCmusHost());
                command.setCmusPort("" + player.getCmusPort());
                command.setCmusPassword(player.getCmusPassword());            
                
                if (player.getCmusMusicFoldersPath() != null) {
	                for (MusicFolderRef folderRef : command.getMusicFolders()) {
						String cmusPathForThisPlayer = player.getCmusMusicFolderPath(folderRef.getId());
	                	if (cmusPathForThisPlayer != null) {
	                		folderRef.setPathInCmus(cmusPathForThisPlayer);
	                	}
					}
                }
            }
        }

        command.setTranscodingSupported(transcodingService.isDownsamplingSupported(null));
        command.setTranscodeDirectory(transcodingService.getTranscodeDirectory().getPath());
        command.setTranscodeSchemes(TranscodeScheme.values());
        command.setTechnologies(PlayerTechnology.values());
        command.setPlayers(players.toArray(new Player[players.size()]));
        command.setAdmin(user.isAdminRole());
        
        ///

        return command;
    }
    
    
    /**
     * 
     * @param musicFolders
     * @return
     */
    private List<PlayerSettingsCommand.MusicFolderRef> wrap(List<MusicFolder> musicFolders) {
        ArrayList<PlayerSettingsCommand.MusicFolderRef> result = new ArrayList<PlayerSettingsCommand.MusicFolderRef>();
        for (MusicFolder musicFolder : musicFolders) {
            result.add(new PlayerSettingsCommand.MusicFolderRef(musicFolder));
        }
        return result;
    }
    

    @Override
    protected void doSubmitAction(Object comm) throws Exception {
        PlayerSettingsCommand command = (PlayerSettingsCommand) comm;
        Player player = playerService.getPlayerById(command.getPlayerId());

        player.setAutoControlEnabled(command.isAutoControlEnabled());
        player.setDynamicIp(command.isDynamicIp());
        player.setName(StringUtils.trimToNull(command.getName()));
        player.setTranscodeScheme(TranscodeScheme.valueOf(command.getTranscodeSchemeName()));
        player.setTechnology(PlayerTechnology.valueOf(command.getTechnologyName()));
        
        if (player.getTechnology().equals(PlayerTechnology.CMUS)) {
        	player.setCmusHost(command.getCmusIP());
        	// TODO conversion must not be done here
        	player.setCmusPort(Integer.valueOf(command.getCmusPort()));
        	player.setCmusPassword(command.getCmusPassword());
        	//
        	Map<Integer, String> folders = new Hashtable<Integer, String>();
        	for (MusicFolderRef folder : command.getMusicFolders()) {
				folders.put(folder.getId(), folder.getPathInCmus());
			}
        	player.setCmusMusicFoldersPath(folders);
                
                // No transcoding for a CMUS player.
                command.setActiveTranscodingIds(new int[0]);                
        }

        playerService.updatePlayer(player);
        transcodingService.setTranscodingsForPlayer(player, command.getActiveTranscodingIds());

        command.setReloadNeeded(true);
    }

    private List<Player> getPlayers(HttpServletRequest request) {
        User user = securityService.getCurrentUser(request);
        String username = user.getUsername();
        List<Player> players = playerService.getAllPlayers();
        List<Player> authorizedPlayers = new ArrayList<Player>();

        for (Player player : players) {
            // Only display authorized players.
            if (user.isAdminRole() || username.equals(player.getUsername())) {
                authorizedPlayers.add(player);
            }
        }
        return authorizedPlayers;
    }

    private void handleRequestParameters(HttpServletRequest request) {
        if (request.getParameter("delete") != null) {
            playerService.removePlayerById(request.getParameter("delete"));
        } else if (request.getParameter("clone") != null) {
            playerService.clonePlayer(request.getParameter("clone"));
        }
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }
    
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    
}
