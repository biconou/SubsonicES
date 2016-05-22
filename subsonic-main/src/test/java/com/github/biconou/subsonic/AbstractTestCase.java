package com.github.biconou.subsonic;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

/**
 * Created by remi on 22/05/2016.
 */
public class AbstractTestCase  extends TestCase {

  protected final MetricRegistry metrics = new MetricRegistry();
  protected ConsoleReporter reporter = null;
  protected JmxReporter jmxReporter = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    jmxReporter = JmxReporter.forRegistry(metrics).build();
    jmxReporter.start();

  }
}
