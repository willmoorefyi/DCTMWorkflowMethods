package com.triteksol.junit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfLoginInfo;

public class DocumentumIntegrationTest {
   private static Logger log = LoggerFactory.getLogger(DocumentumIntegrationTest.class);
   
   public final static String USERNAME_PROP = "com.triteksol.xcp.username";
   public final static String PASSWORD_PROP = "com.triteksol.xcp.password";
   public final static String DOCBASE_PROP = "com.triteksol.xcp.repository";

   private IDfSessionManager sessionManager;
   private IDfSession session;
   
   String username;
   String password;
   String docbase;

   @Before public void setUpSession() {
      try {
         log.info("Setting up session");
         InputStream is = getClass().getResourceAsStream( "/dctm-connect.properties" );
         Properties props = new Properties();
         props.load(is);
         this.username = props.getProperty(USERNAME_PROP);
         this.password = props.getProperty(PASSWORD_PROP);
         this.docbase = props.getProperty(DOCBASE_PROP);
         log.debug("Properties read, username='{}', password='{}', docbase='{}'", new String[] {this.username, this.password, this.docbase});
         IDfClientX clientX = new DfClientX();
         IDfClient client = clientX.getLocalClient();
         IDfLoginInfo loginInfo = clientX.getLoginInfo();
         IDfSessionManager sessionManager = client.newSessionManager();
         log.debug("Created session manager");
         loginInfo.setUser(this.username);
         loginInfo.setPassword(this.password);
         sessionManager.setIdentity(this.docbase, loginInfo);
         log.debug("Set identity");
         this.sessionManager = sessionManager;
         this.session = this.sessionManager.newSession(this.docbase);
         log.info("Session created");
      } catch (IOException e) {
         throw new RuntimeException("IO exception occurred trying to read properties file to configure Documentum session", e);
      } catch (DfException e) {
         throw new RuntimeException("Exception occurred trying to establish Documentum session", e);
      }
   }

   @After public void tearDownSession() throws Exception {
      log.info("Tearing down session");
      if (this.sessionManager != null) {
         if (this.session != null) {
            this.sessionManager.release(this.session);
            this.session = null;
         }
         this.sessionManager = null;
      }
   }

   public IDfSession getSession() {
      return this.session;
   }
   /**
    * @return the username
    */
   public String getUsername() {
      return username;
   }

   /**
    * @return the password
    */
   public String getPassword() {
      return password;
   }

   /**
    * @return the docbase
    */
   public String getDocbase() {
      return docbase;
   }
}
