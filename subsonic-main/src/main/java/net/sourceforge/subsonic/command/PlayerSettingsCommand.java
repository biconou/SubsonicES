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
package net.sourceforge.subsonic.command;

import java.io.File;
import java.util.Date;
import java.util.List;


import net.sourceforge.subsonic.controller.PlayerSettingsController;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.PlayerTechnology;
import net.sourceforge.subsonic.domain.TranscodeScheme;
import net.sourceforge.subsonic.domain.Transcoding;

/**
 * Command used in {@link PlayerSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PlayerSettingsCommand {
    private String playerId;
    private String name;
    private String description;
    private String type;
    private Date lastSeen;
    private boolean isDynamicIp;
    private boolean isAutoControlEnabled;
    private String technologyName;
    private String transcodeSchemeName;
    private boolean transcodingSupported;
    private String transcodeDirectory;
    private List<Transcoding> allTranscodings;
    private int[] activeTranscodingIds;
    private EnumHolder[] technologyHolders;
    private EnumHolder[] transcodeSchemeHolders;
    private Player[] players;
    private boolean isAdmin;
    private boolean isReloadNeeded;
    
    private String cmusIP;
    private String cmusPort;
    private String cmusPassword;
    
    private List<MusicFolderRef> musicFolders;
    
    /**
     * 
     * @author remi
     *
     */
    public static class MusicFolderRef {

        private Integer id;
        private String path;
        private String name;
        private String pathInCmus;

        public MusicFolderRef(MusicFolder musicFolder) {
            id = musicFolder.getId();
            path = musicFolder.getPath().getPath();
            name = musicFolder.getName();
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
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

        public void setName(String name) {
            this.name = name;
        }

		public String getPathInCmus() {
			return pathInCmus;
		}

		public void setPathInCmus(String pathInCmus) {
			this.pathInCmus = pathInCmus;
		}
    } ////



    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isDynamicIp() {
        return isDynamicIp;
    }

    public void setDynamicIp(boolean dynamicIp) {
        isDynamicIp = dynamicIp;
    }

    public boolean isAutoControlEnabled() {
        return isAutoControlEnabled;
    }

    public void setAutoControlEnabled(boolean autoControlEnabled) {
        isAutoControlEnabled = autoControlEnabled;
    }

    public String getTranscodeSchemeName() {
        return transcodeSchemeName;
    }

    public void setTranscodeSchemeName(String transcodeSchemeName) {
        this.transcodeSchemeName = transcodeSchemeName;
    }

    public boolean isTranscodingSupported() {
        return transcodingSupported;
    }

    public void setTranscodingSupported(boolean transcodingSupported) {
        this.transcodingSupported = transcodingSupported;
    }

    public String getTranscodeDirectory() {
        return transcodeDirectory;
    }

    public void setTranscodeDirectory(String transcodeDirectory) {
        this.transcodeDirectory = transcodeDirectory;
    }

    public List<Transcoding> getAllTranscodings() {
        return allTranscodings;
    }

    public void setAllTranscodings(List<Transcoding> allTranscodings) {
        this.allTranscodings = allTranscodings;
    }

    public int[] getActiveTranscodingIds() {
        return activeTranscodingIds;
    }

    public void setActiveTranscodingIds(int[] activeTranscodingIds) {
        this.activeTranscodingIds = activeTranscodingIds;
    }

    public EnumHolder[] getTechnologyHolders() {
        return technologyHolders;
    }

    public void setTechnologies(PlayerTechnology[] technologies) {
        technologyHolders = new EnumHolder[technologies.length];
        for (int i = 0; i < technologies.length; i++) {
            PlayerTechnology technology = technologies[i];
            technologyHolders[i] = new EnumHolder(technology.name(), technology.toString());
        }
    }

    public EnumHolder[] getTranscodeSchemeHolders() {
        return transcodeSchemeHolders;
    }

    public void setTranscodeSchemes(TranscodeScheme[] transcodeSchemes) {
        transcodeSchemeHolders = new EnumHolder[transcodeSchemes.length];
        for (int i = 0; i < transcodeSchemes.length; i++) {
            TranscodeScheme scheme = transcodeSchemes[i];
            transcodeSchemeHolders[i] = new EnumHolder(scheme.name(), scheme.toString());
        }
    }

    public String getTechnologyName() {
        return technologyName;
    }

    public void setTechnologyName(String technologyName) {
        this.technologyName = technologyName;
    }

    public Player[] getPlayers() {
        return players;
    }

    public void setPlayers(Player[] players) {
        this.players = players;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isReloadNeeded() {
        return isReloadNeeded;
    }

    public void setReloadNeeded(boolean reloadNeeded) {
        isReloadNeeded = reloadNeeded;
    }

    
    public String getCmusIP() {
		return cmusIP;
	}

	public void setCmusIP(String cmusIP) {
		this.cmusIP = cmusIP;
	}


	
	public String getCmusPort() {
		return cmusPort;
	}

	public void setCmusPort(String cmusPort) {
		this.cmusPort = cmusPort;
	}



	public String getCmusPassword() {
		return cmusPassword;
	}

	public void setCmusPassword(String cmusPassword) {
		this.cmusPassword = cmusPassword;
	}

    public List<MusicFolderRef> getMusicFolders() {
        return musicFolders;
    }

    public void setMusicFolders(List<MusicFolderRef> musicFolders) {
        this.musicFolders = musicFolders;
    }



	/**
     * Holds the transcoding and whether it is active for the given player.
     */
    public static class TranscodingHolder {
        private Transcoding transcoding;
        private boolean isActive;

        public TranscodingHolder(Transcoding transcoding, boolean isActive) {
            this.transcoding = transcoding;
            this.isActive = isActive;
        }

        public Transcoding getTranscoding() {
            return transcoding;
        }

        public boolean isActive() {
            return isActive;
        }
    }
}