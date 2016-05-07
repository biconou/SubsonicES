package com.github.biconou.subsonic.rest;

import com.github.biconou.subsonic.TestCaseUtils;
import junit.framework.TestCase;
import net.sourceforge.subsonic.TestServer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;

/**
 * Created by remi on 07/05/2016.
 */
public class RestTestCase extends TestCase {

  private static String baseResources = "/com/github/biconou/subsonic/rest/restTestCase/";

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Prepare database
    TestCaseUtils.prepareDataBase(baseResources);

    TestCaseUtils.setSubsonicHome(baseResources);

    // load spring context
    //ApplicationContext context = TestCaseUtils.loadSpringApplicationContext(baseResources);

    // delete index
    //TestCaseUtils.deleteIndex(context);

    //net.sourceforge.subsonic.service.MediaScannerService mediaScannerService = (MediaScannerService)context.getBean("mediaScannerService");
    //TestCaseUtils.execScan(mediaScannerService);
  }


  public void testLaunchRestServer() throws Exception {

    Server server = new Server(8080);

    WebAppContext webapp = new WebAppContext();
    webapp.setContextPath("/testRest");

    String baseDir = RestTestCase.class.getResource(baseResources).getFile();

    webapp.setWar(baseDir + "/webapp");

    server.setHandler(webapp);

    server.start();
    server.join();
  }
}
