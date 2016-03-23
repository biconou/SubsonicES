package com.github.biconou.cmus;

import org.slf4j.LoggerFactory;

import com.github.biconou.cmus.CMusRemoteDriver.CMusStatus;
import java.util.Random;

public class TestCmus {

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {

            String[] listSongs = {
                    "/mnt/NAS/REMI/musique/audiotest/FLAC/16bits-44_1khz.flac",
                    "/mnt/NAS/REMI/musique/audiotest/FLAC/naim-test-1-flac-16-44100.flac",
                    "/mnt/NAS/REMI/musique/audiotest/FLAC/naim-test-2-flac-24-96000.flac"
            };
            
            final CMusRemoteDriver cmus = new CMusRemoteDriver("192.168.0.13", 4041, "subsonic");
            
            Random rand = new Random();
            int first = rand.nextInt(listSongs.length);
            
            System.out.println("Add : "+listSongs[first]);
            cmus.initPlayQueue(listSongs[first]);
            cmus.setGain(0.6f);
            cmus.play();

            int i=first+1;
            while (i<listSongs.length) {
                System.out.println("Add : "+listSongs[i]);
                cmus.addFile(listSongs[i]);
                i++;
            }
            i = 0;
            while (i<first) {
                System.out.println("Add : "+listSongs[i]);
                cmus.addFile(listSongs[i]);
                i++;
            }
            
            
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (;;) {
                        try {
                            CMusStatus status = cmus.status();
                            System.out.println("+++");
                            System.out.println(status.getFile());
                            System.out.println(status.getStatus());
                            System.out.println(status.getUnifiedVolume());
                            System.out.println(status.getDuration());
                            System.out.println(status.getPosition());
                            System.out.println(status.getPositionPercent());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

            System.out.println("------Start status thread ----------------");
            t.start();

            Thread.sleep(10000);
            cmus.setPosition(3);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

}
