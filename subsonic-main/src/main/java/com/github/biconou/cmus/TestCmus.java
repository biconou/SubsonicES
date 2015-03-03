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
                "/mnt/NAS/REMI/musique/PCDM/3 . Musique classique/3.1 Musique de chambre/Vienna Octet - Schubert Octet D803 - Mozart Divertimento K205/11 - Mozart, W.A. - Finale presto.flac",
                "/mnt/NAS/REMI/musique/PCDM/3 . Musique classique/3.9 classement par périodes/3.94 Epoque baroque/Dumont/Henry Dumont - Motets à la chapelle de Louis XIV - FNAC MUSIC 592054/09 - Les Pages de la Chapelle-Muasica Aeterna-Olivier Schneebeli - Magnificat.wav",
                "/mnt/NAS/REMI/musique/PCDM/3 . Musique classique/3.9 classement par périodes/3.94 Epoque baroque/Rameau/Rameau - Dardanus_hi-res/01-29-Dardanus_Acte_II_Scene_3_Entendez_ma_voi-SMR.flac",
                "/mnt/NAS/REMI/Ma musique/The Smiths/The Smiths/09 What Difference Does It Make-.mp3",
                "/mnt/NAS/REMI/Ma musique/The Smiths/The Smiths/02 You've Got Everything Now.mp3",
                "/mnt/NAS/REMI/Ma musique/The Smiths/The Smiths/03 Miserable Lie.mp3"
            };
            
            final CMusRemoteDriver cmus = new CMusRemoteDriver("192.168.0.7", 4041, "subsonic");
            
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
                            System.out.println(status.getFile());
                            System.out.println(status.getStatus());
                            System.out.println(status.getUnifiedVolume());
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

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

}
