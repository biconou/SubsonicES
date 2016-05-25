package com.github.biconou.subsonic.rest;

import com.github.biconou.subsonic.TestCaseUtils;
import com.github.biconou.subsonic.service.MediaScannerService;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContextEvent;

/**
 * Created by remi on 22/05/2016.
 */
public class TestContextLoaderListener extends ContextLoaderListener {

  @Override
  public void contextInitialized(ServletContextEvent event) {
    super.contextInitialized(event);

    ApplicationContext context = getContextLoader().getCurrentWebApplicationContext();

    // delete index
    TestCaseUtils.deleteIndexes(context);

    MediaScannerService mediaScannerService = (MediaScannerService) context.getBean("mediaScannerService");
    TestCaseUtils.execScan(mediaScannerService);

  }
}
