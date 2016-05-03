#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package ${package}app;

import java.util.Scanner;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.k2.core.Application;

public class ${classPrefix}ApplicationTest {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(
      ${classPrefix}ApplicationTest.class);

  private Application application;

  @Before public void setUp() {
    log.trace("Entering setUp");
    application = new ${classPrefix}Application();
    application.run(new String[0]);
    log.trace("Leaving setUp");
  }

  @After public void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void helloController() throws Exception {
    String endpoint = "http://localhost:8081/${classPrefix.toLowerCase()}/hi.html";
    try (Scanner scanner = new Scanner(new URL(endpoint).openStream())) {
      scanner.useDelimiter("\\A");
      assertThat(scanner.next(), is("Hello there"));
    }
  }
}
