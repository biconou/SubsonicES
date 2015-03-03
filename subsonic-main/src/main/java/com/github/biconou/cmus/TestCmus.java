package com.github.biconou.cmus;

import org.slf4j.LoggerFactory;

import com.github.biconou.cmus.CMusRemoteDriver.CMusStatus;

public class TestCmus {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			
			//CMusController cmus = new CMusController("localhost",4041 , "subsonic");
			CMusRemoteDriver cmus = new CMusRemoteDriver("192.168.0.7",4041 , "subsonic");
			cmus.initPlayQueue("/mnt/NAS/REMI/Ma musique/The Smiths/The Smiths/09 What Difference Does It Make-.mp3");
			if (!cmus.isPlaying()) {
				cmus.play();
			}
			cmus.addFile("/mnt/NAS/REMI/Ma musique/The Smiths/The Smiths/02 You've Got Everything Now.mp3");
			cmus.addFile("/mnt/NAS/REMI/Ma musique/The Smiths/The Smiths/03 Miserable Lie.mp3");
			CMusStatus status = cmus.status();
			System.out.println(status.getFile());
			System.out.println(status.getStatus());
			System.out.println(status.getUnifiedVolume());
			
		} catch (Exception e) {			
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		

	}

}
