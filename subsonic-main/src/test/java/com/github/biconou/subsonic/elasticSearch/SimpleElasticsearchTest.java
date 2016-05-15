package com.github.biconou.subsonic.elasticSearch;

import junit.framework.TestCase;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Demonstrates how to use an embedded elasticsearch server in your tests.
 *
 * @author Felix Müller
 */
public class SimpleElasticsearchTest extends TestCase {

    private EmbeddedElasticsearchServer embeddedElasticsearchServer;

    @Override
    protected void setUp() throws Exception {
        embeddedElasticsearchServer = new EmbeddedElasticsearchServer();
    }

    @Override
    protected void tearDown() throws Exception {
        embeddedElasticsearchServer.shutdown();
    }

    /**
     * By using this method you can access the embedded server.
     */
    protected Client getClient() {
        return embeddedElasticsearchServer.getClient();
    }

    public void testIndexSimpleDocument() throws IOException {
        getClient().prepareIndex("myindex", "document", "1")
                .setSource(jsonBuilder().startObject().field("test", "123").endObject())
                .execute()
                .actionGet();

        GetResponse fields = getClient().prepareGet("myindex", "document", "1").execute().actionGet();
        Assert.assertEquals("123",fields.getSource().get("test"));
    }
}
