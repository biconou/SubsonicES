package com.github.biconou.service.media.scan;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.github.biconou.subsonic.service.MediaScannerService;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QueueListener implements MessageListener
{

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);

    public void onMessage( final Message message )
    {
        if ( message instanceof TextMessage )
        {
            final TextMessage textMessage = (TextMessage) message;
            try
            {
                LOG.info( textMessage.getText() );
            }
            catch (final JMSException e)
            {
                e.printStackTrace();
            }
        }
    }
}