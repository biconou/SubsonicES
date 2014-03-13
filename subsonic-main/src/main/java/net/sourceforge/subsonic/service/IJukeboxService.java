package net.sourceforge.subsonic.service;

import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.service.jukebox.AudioPlayer;

public interface IJukeboxService {

	/**
	 * Updates the jukebox by starting or pausing playback on the local audio device.
	 *
	 * @param player The player in question.
	 * @param offset Start playing after this many seconds into the track.
	 */
	public abstract void updateJukebox(Player player, int offset)
			throws Exception;

	public abstract void stateChanged(AudioPlayer audioPlayer,
			AudioPlayer.State state);

	public abstract float getGain();

	public abstract int getPosition();

	/**
	 * Returns the player which currently uses the jukebox.
	 *
	 * @return The player, may be {@code null}.
	 */
	public abstract Player getPlayer();

	public abstract void setGain(float gain);

	public abstract void setTranscodingService(
			TranscodingService transcodingService);

	public abstract void setAudioScrobblerService(
			AudioScrobblerService audioScrobblerService);

	public abstract void setStatusService(StatusService statusService);

	public abstract void setSettingsService(SettingsService settingsService);

	public abstract void setSecurityService(SecurityService securityService);

	public abstract void setMediaFileService(MediaFileService mediaFileService);

}