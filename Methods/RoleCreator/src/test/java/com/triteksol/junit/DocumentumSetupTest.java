/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 22, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.junit;

import java.io.InputStream;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;

import com.documentum.fc.client.IDfSession;
import com.triteksol.xcp.commandline.base.DocumentumSessionManager;

/**
 * @author wmoore
 *
 */
public class DocumentumSetupTest {
   public final static String USERNAME_PROP = "com.triteksol.xcp.username";
   public final static String PASSWORD_PROP = "com.triteksol.xcp.password";
   public final static String DOCBASE_PROP = "com.triteksol.xcp.repository";
   
   String username;
   String password;
   String docbase;
   
   DocumentumSessionManager sessMgr;
   IDfSession session;

   @Before
   public void setUpSession() {
      try {
         InputStream is = getClass().getResourceAsStream( "/dctm-connect.properties" );
         Properties props = new Properties();
         props.load(is);
         this.username = props.getProperty(USERNAME_PROP);
         this.password = props.getProperty(PASSWORD_PROP);
         this.docbase = props.getProperty(DOCBASE_PROP);
         
         sessMgr = new DocumentumSessionManager();
         session = sessMgr.getSession(this.username, this.password, this.docbase);
      } catch (Exception e) {
         throw new RuntimeException("Could not create Documentum session", e);
      }
   }

   @After
   public void tearDownSession() throws Exception {
      sessMgr.releaseSession();
   }

   public IDfSession getSession() {
      return session;
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
