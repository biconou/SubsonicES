package com.github.biconou.service.media.scan;

import net.sourceforge.subsonic.domain.MediaFile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.ObjectMessage;

public class QueueSender {

    private JmsTemplate jmsTemplate;

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void send(final MediaFile file) {
        MessageCreator creator = session -> {
            ObjectMessage message = session.createObjectMessage();
            message.setObject(file);
            return message;
        };
        jmsTemplate.send("Queue.Name", creator);
    }
}