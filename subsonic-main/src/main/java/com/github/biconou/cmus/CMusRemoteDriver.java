package com.github.biconou.cmus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

public class CMusRemoteDriver {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CMusRemoteDriver.class);

	/**
	 * 
	 * <pre>
	 *  status playing
	 *  file /home/me/Music/Queen . Greatest Hits I, II, III The Platinum Collection/Queen/Queen - Greatest Hits III (1999)(The Platinum Collection)/11 - Let me Live.mp3
	 *  duration 285
	 *  position 186
	 * tag artist Queen
	 * tag album Greatest Hits, Vol. 3
	 * tag title Let Me Live
	 * tag date 2000
	 * tag genre Rock
	 * tag tracknumber 11
	 * tag albumartist Queen
	 * set aaa_mode all
	 * set continue true
	 * set play_library true
	 * set play_sorted false
	 * set replaygain disabled
	 * set replaygain_limit true
	 * set replaygain_preamp 6.000000
	 * set repeat true
	 * set repeat_current false
	 * set shuffle true
	 * set softvol false
	 * set vol_left 69
	 * set vol_right 69
	 * </pre>
	 * 
	 * @author bboudreau
	 * 
	 */
	public class CMusStatus {

		private String status;
		private String file;
		private String duration;
		private String position;
		private Map<String, String> tags;
		private Map<String, String> settings;

		public CMusStatus() {
			this.tags = new HashMap<String, String>();
			this.settings = new HashMap<String, String>();
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getFile() {
			return file;
		}

		public void setFile(String file) {
			this.file = file;
		}

		public String getDuration() {
			return duration;
		}

		public void setDuration(String duration) {
			this.duration = duration;
		}

		public String getPosition() {
			return position;
		}

		public String getPositionPercent() {
			if (position == null || duration == null) {
				return "Unknown";
			}
			try {
				DecimalFormat twoDForm = new DecimalFormat("#.##%");
				Float positionF = Float.parseFloat(position);
				Float durationF = Float.parseFloat(duration);
				return twoDForm.format(positionF / durationF);
			} catch (Exception e) {
				//Log.w(TAG, e);
				return "Unknown";
			}
		}

		public void setPosition(String position) {
			this.position = position;
		}

		public String getTag(String key) {
			String value = tags.get(key);
			return value != null ? value : "Unknown";
		}

		public void setTag(String key, String value) {
			if (this.tags == null) {
				this.tags = new HashMap<String, String>();
			}
			this.tags.put(key, value);
		}

		public String getSettings(String key) {
			String value = settings.get(key);
			return value != null ? value : "Unknown";
		}

		public void setSetting(String key, String value) {
			if (this.settings == null) {
				this.settings = new HashMap<String, String>();
			}
			this.settings.put(key, value);
		}

		public String getUnifiedVolume() {
			String volRight = settings.get("vol_right");
			String volLeft = settings.get("vol_left");
			if (volLeft == null && volRight != null) {
				return volRight + "%";
			} else if (volLeft != null && volRight == null) {
				return volLeft + "%";
			}
			try {
				Float volRightF = Float.parseFloat(volRight);
				Float volLeftF = Float.parseFloat(volLeft);

				DecimalFormat twoDForm = new DecimalFormat("#.##");
				return twoDForm.format((volRightF + volLeftF) / 2.0f) + "%";
			} catch (Exception e) {
				//Log.w(TAG, e);
				return "Unknown";
			}
		}

		public boolean isPlaying() {
		
			if ("playing".equals(getStatus())) {
				return true;
			}
			return false;
		}
		
		public boolean isPaused() {
			
			if ("paused".equals(getStatus())) {
				return true;
			}
			return false;
		}
		
		public String toSimpleString() {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("Artist: ").append(getTag("artist")).append("\n");
			strBuilder.append("Title: ").append(getTag("title")).append("\n");
			strBuilder.append("Position: ").append(getPositionPercent()).append("\n");
			strBuilder.append("Volume: ").append(getUnifiedVolume()).append("\n");
			return strBuilder.toString();
		}
	}

	/**
	 * 
	 * @author remi
	 *
	 */
	private interface CMusCommandHandler {

		public void handleCommandAnswer(String answer) throws Exception;
	}

	/**
	 * 
	 * @author remi
	 *
	 */
	private class LogOnlyCMusCommandHandler implements CMusCommandHandler {

		public void handleCommandAnswer(String answer) throws Exception {

			LOG.debug("Command answer is : {}",answer);
		}

	}

	LogOnlyCMusCommandHandler logOnlyCMusCommandHandler = new LogOnlyCMusCommandHandler();

	/**
	 * 
	 * @author remi
	 *
	 */
	private class NoAnswerExpectedCMusCommandHandler implements CMusCommandHandler {

		public void handleCommandAnswer(String answer) throws Exception {

			if (answer != null && answer.trim().length() != 0) {
				LOG.error("Command answer is : {}",answer);
				throw new Exception("Command answer is : "+answer);
			} else {
				LOG.debug("Command answer is : {}",answer);
			}

		}		
	}

	NoAnswerExpectedCMusCommandHandler noAnswerExpectedCMusCommandHandler = new NoAnswerExpectedCMusCommandHandler();


	String host = null;
	int port = 0;
	String passwd = null;
	Socket socket = null;
	BufferedReader in = null;
	Writer out = null;
	
	private float gain = 0.5f;


	/**
	 * Public constructor.
	 * @throws Exception 
	 */
	public CMusRemoteDriver(final String host, final int port,
			final String password) throws Exception {

		LOG.debug("Creation of a new CMusControler");

		this.host = host;
		this.port = port;
		this.passwd = password;

		LOG.debug("Set softvolume on in cmus");
		sendCommandToCMus("set softvol=true",noAnswerExpectedCMusCommandHandler);
		//LOG.debug("Set status display program in cmus");
		//sendCommandToCMus("set status_display_program=/biconou/cmus-status-display",noAnswerExpectedCMusCommandHandler);
	}


	/**
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private String readAnswer(BufferedReader in) throws IOException {
		StringBuilder answerBuilder = new StringBuilder();

		String line;
		while ((line = in.readLine()) != null && line.length() != 0) {
			answerBuilder.append(line).append("\n");
		}

		return answerBuilder.toString();
	}

	/**
	 * 
	 * @param in
	 * @throws Exception
	 */
	private void validAuth(BufferedReader in) throws Exception {
		String passAnswer = readAnswer(in);
		if (passAnswer != null && passAnswer.trim().length() != 0) {
			throw new Exception("Could not login: " + passAnswer);
		}
	}


	/**
	 * 
	 * @param command
	 */
	public  void sendCommandToCMus(final String command,final CMusCommandHandler handler) throws Exception {

		LOG.debug("sending command to cmus : {}",command);

		try {
			
			if (socket == null) {
				try {
					startCmusSession();
				} catch (Exception e) {
					LOG.error("Could not start a cmus session", e);
					throw e;
				}
			}

			out.write(command + "\n");
			out.flush();
			handler.handleCommandAnswer(readAnswer(in));
		} catch (final Exception e) {
			LOG.error("Could not send the command. Trying again with a new cmus session.");
			try {
				if (in != null) {
					try {
						in.close();
					} catch (Exception ignoreEx) {
					}
					in = null;
				}
				if (out != null) {
					try {
						out.close();
					} catch (Exception ignoreEx) {
					}
					out = null;
				}
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception ignoreEx) {
					}
					socket = null;
				}
				startCmusSession();
				out.write(command + "\n");
				out.flush();
				handler.handleCommandAnswer(readAnswer(in));				
			} catch (Exception eAgain) {
				LOG.error("Could not send command to cmus", eAgain);
				if (in != null) {
					try {
						in.close();
					} catch (Exception ignoreEx) {
					}
					in = null;
				}
				if (out != null) {
					try {
						out.close();
					} catch (Exception ignoreEx) {
					}
					out = null;
				}
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception ignoreEx) {
					}
					socket = null;
				}
				throw eAgain;
			}
		} 

	}


	/**
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws Exception
	 */
	private void startCmusSession() throws UnknownHostException, IOException,
			UnsupportedEncodingException, Exception {
		LOG.trace("begin startCmusSession()");
		socket = new Socket(host, port);
		LOG.debug("Connected to cmus host {}:{}",host,port);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()), Character.SIZE);
		out = new OutputStreamWriter(socket.getOutputStream(),"UTF-8");

		LOG.debug("Sinding password to cmus");
		out.write("passwd " + passwd + "\n");
		out.flush();

		validAuth(in);
	}


	/**
	 * Stop playing in cmus
	 * 
	 * @throws Exception 
	 * 
	 */
	public void stop() throws Exception {
		LOG.trace("begin stop()");
		sendCommandToCMus("player-stop",noAnswerExpectedCMusCommandHandler);
	}

	/**
	 * Pause playing in cmus
	 * 
	 * @throws Exception 
	 * 
	 */
	public void pause() throws Exception {
		LOG.trace("begin pause()");
		sendCommandToCMus("player-pause",noAnswerExpectedCMusCommandHandler);
	}

	/**
	 * @throws Exception 
	 * 
	 */
	public void clearPlayQueue() throws Exception {
		LOG.trace("begin clearPlayQueue()");
		sendCommandToCMus("clear -q",noAnswerExpectedCMusCommandHandler);
	}

	public void addFile(String file) throws Exception {		
		sendCommandToCMus("add -q "+file,noAnswerExpectedCMusCommandHandler);
	}

	public void next() throws Exception {
		sendCommandToCMus("player-next",noAnswerExpectedCMusCommandHandler);
	}

	public void play() throws Exception {
		sendCommandToCMus("player-play",noAnswerExpectedCMusCommandHandler);
	}

	/**
	 * Clear the cmus play queue and init a new play queue with the given file name ready to be played.
	 * 
	 * @param file The given file name  (must be an absolute file path and name).
	 * @throws Exception 
	 */
	public void initPlayQueue(String file) throws Exception {
		LOG.trace("begin initPlayQueue(String file[{}])", file);
//		stop();
//		clearPlayQueue();
//		addFile(file);		
//		Thread.sleep(1000); // This is a hack !!!! (don't remove).
//		CMusStatus status = status();
//		if (status.getFile() != null && !status.isPlaying()) {
//			next();
//		}
		//CMusStatus status = status();
		clearPlayQueue();
		addFile(file);		
//		if (file.equals(status.getFile())) {
//			next();
//		} else {
//			while (!file.equals(status.getFile())) {
//				next();
//				status = status();
//			}
//		}
		//next();
	}

	/**
	 * Close the cmus controler and release resources.
	 */
	public void close(){
		//TODO do something ?
	}

	/**
	 * 
	 * @param gain
	 * @throws Exception 
	 */
	public void setGain(float gain) throws Exception {
		LOG.trace("begin setGain(float gain[{}])", gain);
		int vol = (int)(gain*100);
		sendCommandToCMus("set softvol_state="+vol+" "+vol,noAnswerExpectedCMusCommandHandler);
		// Store current gain
		this.gain = gain;  
	}
	
	
	/**
	 * Returns the current gain of that player.
	 * 
	 * @return
	 */
	public float getGain() {
		return this.gain;
	}


	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public CMusStatus status() throws Exception {
		LOG.debug("Begin status()");

		final CMusStatus cmusStatus =  new CMusStatus();

		sendCommandToCMus("status", new CMusCommandHandler() {

			private void addTagOrSetting(CMusStatus cmusStatus, String line) {
				int firstSpace = line.indexOf(' ');
				int secondSpace = line.indexOf(' ', firstSpace + 1);
				String type = line.substring(0, firstSpace);
				String key = line.substring(firstSpace + 1, secondSpace);
				String value = line.substring(secondSpace + 1);
				if (type.equals("set")) {
					cmusStatus.setSetting(key, value);
				} else if (type.equals("tag")) {
					cmusStatus.setTag(key, value);
				} else {
					//Log.e(TAG, "Unknown type in status: " + line);
				}
			}

			
			public void handleCommandAnswer(String answer) throws Exception {

				String[] strs = answer.split("\n");

				for (String str : strs) {
					if (str.startsWith("set") || str.startsWith("tag")) {
						addTagOrSetting(cmusStatus, str);
					} else {
						int firstSpace = str.indexOf(' ');
						String type = str.substring(0, firstSpace);
						String value = str.substring(firstSpace + 1);
						if (type.equals("status")) {
							cmusStatus.setStatus(value);
						} else if (type.equals("file")) {
							cmusStatus.setFile(value);
						} else if (type.equals("duration")) {
							cmusStatus.setDuration(value);
						} else if (type.equals("position")) {
							cmusStatus.setPosition(value);
						}
					}
				}
			}
		});

		LOG.debug("End status()");
		return cmusStatus; 
	}

	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	public boolean isPaused() throws Exception {
		CMusStatus status = status();
		return status.isPaused();
	}
	
	public boolean isPlaying() throws Exception {
		CMusStatus status = status();
		return status.isPlaying();
	}
	

}
