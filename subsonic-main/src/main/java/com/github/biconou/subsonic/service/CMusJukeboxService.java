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

import java.util.List;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.AudioScrobblerService;
import net.sourceforge.subsonic.service.IJukeboxService;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.StatusService;
import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.service.jukebox.AudioPlayer;
import net.sourceforge.subsonic.service.jukebox.AudioPlayer.State;
import net.sourceforge.subsonic.util.FileUtil;

import org.slf4j.LoggerFactory;

import com.github.biconou.cmus.CMusController;
import com.github.biconou.cmus.CMusController.CMusStatus;

/**
 * Plays music on the local audio device.
 *
 * @author Sindre Mehus
 */
public class CMusJukeboxService implements AudioPlayer.Listener, IJukeboxService {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CMusJukeboxService.class);

	private AudioScrobblerService audioScrobblerService;
	private StatusService statusService;
	private SettingsService settingsService;
	private SecurityService securityService;

	private Player player;
	private TransferStatus status;
	private MediaFile currentPlayingFile;
	private float gain = 0.5f;
	private int offset;
	private MediaFileService mediaFileService;

	private CMusController cmusControler = null;
	private String cmusPlayingFile = null; 

	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	private CMusController getCMusController() throws Exception  {
		if (cmusControler == null) {
			try {
				//cmusControler = new CMusController("localhost",4041,"subsonic");
				cmusControler = new CMusController("192.168.0.7",4041,"subsonic");
			} catch (Exception e) {
				LOG.error("Error trying to create the cmus controller",e);
				throw e;
			}         	
		}
		return cmusControler;
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

		//
		if (player.getPlayQueue().getStatus() == PlayQueue.Status.PLAYING) {
			LOG.debug("Play Queue status : {}",PlayQueue.Status.PLAYING.toString());
			this.player = player;
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

			boolean paused = getCMusController().isPaused();
			if (LOG.isDebugEnabled()) {
				LOG.debug("CMUS paused ? {} ",paused);
			}

			if (sameFile && paused && offset == 0) {
				getCMusController().play();
			} else {
				this.offset = offset;
				getCMusController().stop();
				
				cmusPlayingFile = null;

				if (currentPlayingFile != null) {
					onSongEnd(currentPlayingFile);
				}

				if (currentFileInPlayQueue != null) {

					getCMusController().initPlayQueue(currentFileInPlayQueue.getFile().getAbsolutePath());

					getCMusController().setGain(gain);
					getCMusController().play();


					onSongStart(currentFileInPlayQueue);
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
				getCMusController().addFile(fileName);
			}
			
		} else {
			try {
				getCMusController().pause();
			} catch (Exception e) {
				LOG.error("Error trying to pause",e);
				throw e;
			}
		}
	}

	
	/**
	 * 
	 */
	public synchronized void cmusStatusChanged() {

		LOG.debug("Begin cmusStatusChanged");
		
		CMusStatus status = null;
		try {
			status = getCMusController().status();
			if (status.isPlaying()) {
				String oldCmusPlayingFile = cmusPlayingFile;
				cmusPlayingFile = status.getFile();
				if (oldCmusPlayingFile != null && !oldCmusPlayingFile.equals(cmusPlayingFile)) {
					LOG.debug("Playing file changed in CMus");
					synchronized (player.getPlayQueue()) {
						onSongEnd(player.getPlayQueue().getCurrentFile());
						player.getPlayQueue().next();
						onSongStart(player.getPlayQueue().getCurrentFile());
					}
				}
			} else {
				if (!status.isPaused()) {
					if (cmusPlayingFile != null) {
						cmusPlayingFile = null;
						MediaFile currentPlayingInPlayQueue = player.getPlayQueue().getCurrentFile();
						if (currentPlayingInPlayQueue != null) {
							onSongEnd(currentPlayingInPlayQueue);
						}
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

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#getPlayer()
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * 
	 * @param file
	 */
	private void onSongStart(MediaFile file) {
		LOG.info(player.getUsername() + " starting jukebox for \"" + FileUtil.getShortPath(file.getFile()) + "\"");
		status = statusService.createStreamStatus(player);
		status.setFile(file.getFile());
		status.addBytesTransfered(file.getFileSize());
		mediaFileService.incrementPlayCount(file);
		scrobble(file, false);
	}

	/**
	 * 
	 * @param file
	 */
	private void onSongEnd(MediaFile file) {
		LOG.info(player.getUsername() + " stopping jukebox for \"" + FileUtil.getShortPath(file.getFile()) + "\"");
		if (status != null) {
			statusService.removeStreamStatus(status);
		}
		scrobble(file, true);
	}

	private void scrobble(MediaFile file, boolean submission) {
		if (player.getClientId() == null) {  // Don't scrobble REST players.
			audioScrobblerService.register(file, player.getUsername(), submission, null);
		}
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.subsonic.service.IJukeboxService#setGain(float)
	 */
	public synchronized void setGain(float gain) {
		this.gain = gain;

		try {
			getCMusController().setGain(gain);
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

	@Override
	public void stateChanged(AudioPlayer player, State state) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @throws Exception 
	 * 
	 */
	public void updateCMUSPlayQueue() throws Exception {
		
		cmusControler.clearPlayQueue();
		List<MediaFile> files = player.getPlayQueue().getFiles();
		int nextAfterCurrentInPlayQueue = player.getPlayQueue().getIndex() + 1;
		for (int i=nextAfterCurrentInPlayQueue; i<files.size(); i++) {
			String fileName = files.get(i).getFile().getAbsolutePath();
			// fileName can not be null as it comes from a file name in queue.
			getCMusController().addFile(fileName);
		}
	}
}
