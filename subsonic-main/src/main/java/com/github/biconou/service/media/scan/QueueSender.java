package com.github.biconou.service.media.scan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class QueueSender
{
    private final JmsTemplate jmsTemplate;

    @Autowired
    public QueueSender( final JmsTemplate jmsTemplate )
    {
        this.jmsTemplate = jmsTemplate;
    }

    public void send( final String message )
    {
        jmsTemplate.convertAndSend( "Queue.Name", message );
    }
}