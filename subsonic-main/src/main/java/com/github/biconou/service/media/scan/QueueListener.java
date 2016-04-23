package com.github.biconou.service.media.scan;

import com.github.biconou.subsonic.service.MediaScannerService;
import net.sourceforge.subsonic.domain.MediaFile;
import org.slf4j.LoggerFactory;
import org.springframework.jms.listener.SessionAwareMessageListener;

import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

public class QueueListener implements SessionAwareMessageListener {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);
    private VideoIndex videoIndex = null;



    /**
     * @param message
     */
    public void onMessage(final Message message, Session session) {
        try {
            LOG.info( "onMessage : "+message.getJMSMessageID() );
            ObjectMessage objectMessage = (ObjectMessage) message;
            Object obj = objectMessage.getObject();
            if (obj instanceof MediaFile) {
                MediaFile mediaFile = (MediaFile) obj;
                LOG.info("Scan media file : "+mediaFile.getPath()+mediaFile.getName());
                getVideoIndex().createOrReplace(mediaFile);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    public VideoIndex getVideoIndex() {
        return videoIndex;
    }

    public void setVideoIndex(VideoIndex videoIndex) {
        this.videoIndex = videoIndex;
    }
}