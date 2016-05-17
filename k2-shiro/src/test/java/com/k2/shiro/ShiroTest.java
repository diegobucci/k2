/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.shiro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.TextConfigurationRealm;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;

import com.k2.core.Application;
import com.k2.core.ModuleContext;
import com.k2.core.Registrator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** The Shiro module integration tests.
 */
public class ShiroTest {

  /** The class logger. */
  private final Logger log = LoggerFactory.getLogger(ShiroTest.class);

  private Application application;

  private CloseableHttpClient httpClient = HttpClientBuilder.create()
      .setRedirectStrategy(new LaxRedirectStrategy()).build();

  private Executor executor;

  private static String home = "http://localhost:8081";
  private static String hiUrl = home + "/test/hi.html";
  private static String loginUrl = home + "/test/login.html";
  private static String logoutUrl = home + "/logout";

  @Before public void setUp() {
    log.trace("Entering setUp");

    application = new TestApplication();
    application.run(new String[] {"--shiro.password=x"});

    executor = Executor.newInstance(httpClient);

    log.trace("Leaving setUp");
  }

  @After public void tearDown() throws InterruptedException {
    application.stop();
  }

  @Test public void getModuleBean() throws Exception {
    assertThat(application.getBean(Shiro.class, "shiro"),
        is(not(nullValue())));
  }

  @Test public void securityManager() {
    assertThat(application.getBean(Shiro.class, "securityManager"),
        is(not(nullValue())));
  }

  @Test public void shiroFilter() {
    assertThat(application.getBean(Shiro.class, "shiroFilter"),
        is(not(nullValue())));
  }

  @Test public void loginLogoutFlow() throws Exception {

    String page;

    // Get the hi page, unauthenticated. This should give the login page.
    page = executor.execute(Request.Get(hiUrl)).returnContent().asString();
    assertThat(page, startsWith("Hi, who are you?"));

    // Attempt a login, this should result in the hi page.
    page = executor.execute(
        Request.Post(loginUrl).bodyForm(Form.form()
            .add("username", "test").add("password", "test").build())
        ).returnContent().asString();

    assertThat(page, startsWith("Hello, test"));

    // Logout. This should result in the login page.
    page = executor.execute(Request.Get(logoutUrl)).returnContent().asString();
    assertThat(page, startsWith("Hi, who are you?"));
  }

  @Test public void landing() throws Exception {
    String page;
    page = executor.execute(Request.Get(home)).returnContent().asString();
    assertThat(page, startsWith("Hi, who are you?"));
  }

  /////////////////////////////////////////////////////////////////////
  ///////////    The test application and module  /////////////////////
  /////////////////////////////////////////////////////////////////////

  /* A test controller.
   */
  @Controller
  public static class HiController {
    // A login endpoint (mapped to login.html).
    @RequestMapping(value = "/login.html")
    public HttpEntity<String> login(
        @Value("#{request.getAttribute('shiroLoginFailure')}")
        final String error) {
      String form = "Hi, who are you? <form method='POST'>"
          + "<input type='text' name='username'/>"
          + "<input type='password' name='password'/>"
          + "<input type='submit'/></form>";
      if (error != null) {
        form += "Wrong username or password";
      }
      return new HttpEntity<String>(form);
    }

    // A hi endpoint (hi.html).
    @RequestMapping(value = "/hi.html", method = RequestMethod.GET)
    public HttpEntity<String> hi() {
      String page = "Hello, "
          + SecurityUtils.getSubject().getPrincipal().toString()
          + " <a href='/logout'>logout</a>";
      return new HttpEntity<String>(page);
    }
  };

  /* A module named test that serves the hi.html and anonymous.html endpoints.
   */
  @Component("test")
  public static class TestModule implements Registrator {

    @Override
    public void addRegistrations(final ModuleContext moduleContext) {
      ShiroRegistry shiroRegistry = moduleContext.get(ShiroRegistry.class);
      shiroRegistry.registerLoginUrl("/login.html");
      shiroRegistry.registerEndpoint("/hi.html", "authc");
    }

    @Bean public HiController hiController() {
      return new HiController();
    }
  };

  /* The test application.
   */
  @Configuration
  public static class TestApplication extends Application {
    public TestApplication() {
      super(new Shiro(), new TestModule());
      setLandingUrl("/test/hi.html");
    }

    public static void main(final String ... args) {
      Application application = new TestApplication();
      application.run(new String[] {"--shiro.password=x"});
    }

    @Bean public Realm realm() {
      TextConfigurationRealm realm = new TextConfigurationRealm();
      realm.addAccount("test", "test");
      return realm;
    }
  }
}

