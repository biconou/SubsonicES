package com.github.biconou.cmus;

import com.github.biconou.cmus.CMusController.CMusStatus;

public class TestCmus {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			//CMusController cmus = new CMusController("localhost",4041 , "subsonic");
			CMusController cmus = new CMusController("192.168.0.7",4041 , "subsonic");
			//cmus.initPlayQueue("/mnt/NAS/REMI/tmp/concertContemporain1905.wav");
			cmus.initPlayQueue("/mnt/NAS/REMI/Ma musique/16 Horsepower/16 horsepower ep/01 haw.mp3");
			cmus.play();
			cmus.addFile("/mnt/NAS/REMI/Ma musique/16 Horsepower/16 horsepower ep/02 south pennsylvania waltz.mp3");
			//Thread.sleep(15000);
			//cmus.pause();
			//System.out.println("paused ?"+cmus.isPaused());
			//Thread.sleep(2000);
			//cmus.play();
			//Thread.sleep(5000);
			//cmus.next();
			CMusStatus status = cmus.status();
			System.out.println(status.getFile());
			System.out.println(status.getStatus());
			
		} catch (Exception e) {			
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		

	}

}
