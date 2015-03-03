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

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.PlayQueue.Status;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.PlayerTechnology;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.AudioScrobblerService;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.StatusService;
import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.util.FileUtil;

import org.slf4j.LoggerFactory;

import com.github.biconou.cmus.CMusRemoteDriver;
import com.github.biconou.cmus.CMusRemoteDriver.CMusStatus;

/**
 * Plays music on on a remote cmus.
 *
 * @author Rémi Cocula
 */
public class CMusService  {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CMusService.class);

	// TODO what to do with that ?
	private AudioScrobblerService audioScrobblerService;
	private StatusService statusService;
	private SettingsService settingsService;
	private SecurityService securityService;

	// TODO not safe if multiple players
	// TODO currentPlayingFile is perhaps non necessary !!!! try to remove it.
	private MediaFile currentPlayingFile;
	private float gain = 0.5f;
	private int offset;
	private MediaFileService mediaFileService;
	
	private List<MusicFolder> allMusicFolders = null;

	private Map<Integer, CMusRemoteDriver> cmusDriversForPlayers = new Hashtable<Integer, CMusRemoteDriver>();
	private Map<Integer, String> cmusPlayingFileForPlayers = new Hashtable<Integer, String>();
	
	
	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	private CMusRemoteDriver getCMusRemoteDriver(Player player) throws Exception  {
		if (player == null) {
			throw new IllegalStateException("Parameter player most not be null");
		}

		CMusRemoteDriver driver = null;

		if (player.getTechnology().equals(PlayerTechnology.CMUS)) {

			synchronized (cmusDriversForPlayers) {

				driver = cmusDriversForPlayers.get(Integer.valueOf(player.getId()));
				if (driver == null) {
					try {
						driver = new CMusRemoteDriver(player.getCmusHost()
								,player.getCmusPort()
								,player.getCmusPassword());
						driver.setGain(gain);
					} catch (Exception e) {
						LOG.error("Error trying to create the cmus controller",e);
						throw e;
					}
					cmusDriversForPlayers.put(Integer.valueOf(player.getId()), driver);
				}
				
			}

		} else {
			throw new IllegalStateException("The player "+player.getId()+" is not a cmus player");
		}
		return driver;
	}
	
	
	/**
	 * 
	 * @param player
	 */
	private String getCmusPlayingFile(Player player) {
		return cmusPlayingFileForPlayers.get(Integer.valueOf(player.getId()));
	}
	
	/**
	 * 
	 * @param player
	 * @param file
	 */
	private void setCmusPlayingFile(Player player,String file) {
		cmusPlayingFileForPlayers.put(Integer.valueOf(player.getId()),file);
	}
	
	/**
	 * 
	 * @param player
	 */
	private void deleteCmusPlayingFile(Player player) {
		cmusPlayingFileForPlayers.remove(Integer.valueOf(player.getId()));
	}
	
	
	/**
	 * Returns the list of all music folders, get the list just one time from the settings service.
	 * @return
	 */
	private List<MusicFolder> getAllMusicFolders() {
		if (allMusicFolders == null) {
			synchronized (this) {
				allMusicFolders = settingsService.getAllMusicFolders();
			}
		}
		return allMusicFolders;
	}
	/**
	 * Compute the path to be used inside cmus for a given file.
	 * @param player
	 * @param fileName
	 */
	private String computeFilePathForCmus(final Player player, final String fileName) {
		
		String transformedFilePathAndName = fileName;
		
		List<MusicFolder> allFolders = getAllMusicFolders();
		for (MusicFolder musicFolder : allFolders) {
			String musicFolderPath = musicFolder.getPath().getAbsolutePath();
			if (fileName.contains(musicFolderPath)) {
				String translateFolderPath = player.getCmusMusicFoldersPath().get(musicFolder.getId());
				if (translateFolderPath != null) {
					transformedFilePathAndName = fileName.replace(musicFolderPath, translateFolderPath);
					if (translateFolderPath.contains("/")) {
						transformedFilePathAndName = transformedFilePathAndName.replaceAll("\\\\", "/");
					}					
				}
			}
		}
		
		return transformedFilePathAndName;
	}
	
	/**
	 * Returns the current gain value for a player. 
	 * 
	 * @param player
	 * @return
	 * @throws Exception If any problem occurs with CMUS remote control.
	 */
	public float getGain(Player player) throws Exception {
		// Find the correct cmus driver
		CMusRemoteDriver cmusDriver = getCMusRemoteDriver(player);
		return cmusDriver.getGain();
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#updateJukebox(net.sourceforge.subsonic.domain.Player, int)
	 */
	public synchronized void updateJukebox(Player player, int offset) throws Exception {
		
		// Control user authorizations
		User user = securityService.getUserByName(player.getUsername());
		if (!user.isJukeboxRole()) {
			LOG.warn(user.getUsername() + " is not authorized for jukebox playback.");
			return;
		}

		// Find the correct cmus driver
		CMusRemoteDriver cmusDriver = getCMusRemoteDriver(player);
		
		//
		if (player.getPlayQueue().getStatus() == PlayQueue.Status.PLAYING) {
			LOG.debug("Play Queue status : {}",PlayQueue.Status.PLAYING.toString());
			MediaFile currentFileInPlayQueue;
			synchronized (player.getPlayQueue()) {
				currentFileInPlayQueue = player.getPlayQueue().getCurrentFile();
			}
			LOG.debug("Play file {}",currentFileInPlayQueue.getName());
			
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Begin of play : file = {}",currentFileInPlayQueue != null?currentFileInPlayQueue.getName():"null");
			}
			
			// Resume if possible.
			boolean sameFile = currentFileInPlayQueue != null && currentFileInPlayQueue.equals(currentPlayingFile);
			if (LOG.isDebugEnabled()) {				
				LOG.debug("sameFile={} (file={} and currentPlayingFile={})",new Object[]{new Boolean(sameFile),currentFileInPlayQueue.getName(),
						currentPlayingFile == null?"null":currentPlayingFile.getName()});
			}

			boolean paused = cmusDriver.isPaused();
			if (LOG.isDebugEnabled()) {
				LOG.debug("CMUS paused ? {} ",paused);
			}

			if (sameFile && paused && offset == 0) {
				cmusDriver.play();
			} else {
				this.offset = offset;
				//cmusDriver.stop();
				
				deleteCmusPlayingFile(player);

				if (currentPlayingFile != null) {
					onSongEnd(player,currentPlayingFile);
				}

				if (currentFileInPlayQueue != null) {

					
					cmusDriver.initPlayQueue(computeFilePathForCmus(player, currentFileInPlayQueue.getFile().getAbsolutePath()));
					
					String fileBeforeNext = cmusDriver.status().getFile();
					cmusDriver.next();
					String fileAfterNext = cmusDriver.status().getFile();
					// Hack to force the next if it didn't work at first time.
					int countLoop = 0;
					while (countLoop <= 20 && (fileAfterNext == null || "".equals(fileAfterNext) || fileAfterNext.equals(fileBeforeNext))) {
						Thread.sleep(500);
						cmusDriver.next();
						fileAfterNext = cmusDriver.status().getFile();
						countLoop++;
					}
					if (!cmusDriver.isPlaying()) {
						cmusDriver.play();
					}


					onSongStart(player,currentFileInPlayQueue);
				}
			}

			currentPlayingFile = currentFileInPlayQueue;



			// load the other songs in cmus play queue starting with the second file in queue
			LOG.debug("load the other songs in cmus play queue starting with the next file in queue");
			List<MediaFile> files = player.getPlayQueue().getFiles();
			int nextAfterCurrentInPlayQueue = player.getPlayQueue().getIndex() + 1;
			for (int i=nextAfterCurrentInPlayQueue; i<files.size(); i++) {
				String fileName = files.get(i).getFile().getAbsolutePath();
				// fileName can not be null as it comes from a file name in queue.
				cmusDriver.addFile(computeFilePathForCmus(player, fileName));
			}
			
		} else {
			try {
				cmusDriver.pause();
			} catch (Exception e) {
				LOG.error("Error trying to pause",e);
				throw e;
			}
		}
	}

	
	/**
	 * 
	 * @param player
	 * @return
	 */
        @Deprecated
	public List<TransferStatus> checkForNowPlayingService(Player player) {
		
		//List<TransferStatus> statuses = null;
		checkCmusStatus(player);
	//	if (player.getPlayQueue().getStatus().equals(Status.PLAYING)) {			
//		MediaFile currentPlayingInPlayQueue = player.getPlayQueue().getCurrentFile();
//			if (currentPlayingInPlayQueue != null) {
//				TransferStatus status = statusService.createStreamStatus(player);
//				status.setFile(currentPlayingInPlayQueue.getFile());
//				status.addBytesTransfered(currentPlayingInPlayQueue.getFileSize());
//				statuses = new ArrayList<TransferStatus>();
//				statuses.add(status);
//			} 
//		}
		return statusService.getStreamStatusesForPlayer(player);
	}
	/**
	 * 
	 */
	
	public void checkCmusStatus(Player player) {

		LOG.debug("Begin CheckCmusStatus");
		
		CMusStatus cmusStatus = null;
		try {
			cmusStatus = getCMusRemoteDriver(player).status();
			if (cmusStatus.isPlaying()) {
				String oldCmusPlayingFile = getCmusPlayingFile(player);
				setCmusPlayingFile(player,cmusStatus.getFile());
				if (oldCmusPlayingFile != null && !oldCmusPlayingFile.equals(getCmusPlayingFile(player))) {
					LOG.debug("Playing file changed in CMus");
					synchronized (player.getPlayQueue()) {
						onSongEnd(player,player.getPlayQueue().getCurrentFile());
						player.getPlayQueue().next();
						onSongStart(player,player.getPlayQueue().getCurrentFile());
					}
				}
			} else {
				if (!cmusStatus.isPaused()) {
					if (getCmusPlayingFile(player) != null) {
						deleteCmusPlayingFile(player);
						MediaFile currentPlayingInPlayQueue = player.getPlayQueue().getCurrentFile();
						if (currentPlayingInPlayQueue != null) {
							onSongEnd(player,currentPlayingInPlayQueue);
						}
						player.getPlayQueue().setStatus(Status.STOPPED);
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Error getting cmus status",e);
		}
	}
	

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#getGain()
	 */
	public synchronized float getGain() {
		return gain;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#getPosition()
	 */
	public synchronized int getPosition() {
		// TODO do something ? 
		return 0;
		// return audioPlayer == null ? 0 : offset + audioPlayer.getPosition();
	}

	/**
	 * 
	 * @param file
	 * @throws Exception 
	 */
	private void onSongStart(Player player,MediaFile file) throws Exception {
		LOG.info("onSongStart : " + player.getUsername() + " start playing in CMUS file : \"" + FileUtil.getShortPath(file.getFile()) + "\"");
		
		CMusStatus cmusStatus = getCMusRemoteDriver(player).status();
		setCmusPlayingFile(player,cmusStatus.getFile());
		
		
		TransferStatus status = statusService.createStreamStatus(player);
		status.setFile(file.getFile());
		status.addBytesTransfered(file.getFileSize());
		//streamStatusesForPlayers.put(Integer.valueOf(player.getId()), status);
		//LOG.trace("streamStatusesForPlayers has {} element(s)",streamStatusesForPlayers.size());
		//statusService.getStreamStatusesForPlayer(player);
		
		mediaFileService.incrementPlayCount(file);
		scrobble(player,file, false);
	}

	/**
	 * 
	 * @param file
	 */
	private void onSongEnd(Player player,MediaFile file) {
		LOG.info(player.getUsername() + " stopping cmus for \"" + FileUtil.getShortPath(file.getFile()) + "\"");
		//TransferStatus status = streamStatusesForPlayers.get(Integer.valueOf(player.getId()));
		TransferStatus status = statusService.getStreamStatusesForPlayer(player).get(0);
		if (status != null) {
			statusService.removeStreamStatus(status);
		}
		scrobble(player,file, true);
	}

	private void scrobble(Player player,MediaFile file, boolean submission) {
		if (player.getClientId() == null) {  // Don't scrobble REST players.
			audioScrobblerService.register(file, player.getUsername(), submission, null);
		}
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setGain(float)
	 */
	public synchronized void setGain(Player player,float gain) {
		
		this.gain = gain;

		try {
			getCMusRemoteDriver(player).setGain(gain);
		} catch (Exception e) {
			LOG.error("Error trying to set volume",e);
		}

	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setTranscodingService(net.sourceforge.subsonic.service.TranscodingService)
	 */
	public void setTranscodingService(TranscodingService transcodingService) {
		//this.transcodingService = transcodingService;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setAudioScrobblerService(net.sourceforge.subsonic.service.AudioScrobblerService)
	 */
	public void setAudioScrobblerService(AudioScrobblerService audioScrobblerService) {
		this.audioScrobblerService = audioScrobblerService;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setStatusService(net.sourceforge.subsonic.service.StatusService)
	 */
	public void setStatusService(StatusService statusService) {
		this.statusService = statusService;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setSettingsService(net.sourceforge.subsonic.service.SettingsService)
	 */
	public void setSettingsService(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setSecurityService(net.sourceforge.subsonic.service.SecurityService)
	 */
	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setMediaFileService(net.sourceforge.subsonic.service.MediaFileService)
	 */
	public void setMediaFileService(MediaFileService mediaFileService) {
		this.mediaFileService = mediaFileService;
	}

	/**
	 * @throws Exception 
	 * 
	 */
	public void updateCMUSPlayQueue(Player player) throws Exception {
		
		CMusRemoteDriver cmusDriver = getCMusRemoteDriver(player);
		
		cmusDriver.clearPlayQueue();
		List<MediaFile> files = player.getPlayQueue().getFiles();
		int nextAfterCurrentInPlayQueue = player.getPlayQueue().getIndex() + 1;
		for (int i=nextAfterCurrentInPlayQueue; i<files.size(); i++) {
			String fileName = files.get(i).getFile().getAbsolutePath();
			// fileName can not be null as it comes from a file name in queue.
			cmusDriver.addFile(computeFilePathForCmus(player, fileName));
		}
	}
}
