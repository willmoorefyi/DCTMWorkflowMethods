/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 9, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.base;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 *
 */
public class DocumentumSessionManagerTest extends DocumentumIntegrationTest {
   private static final Logger log = LoggerFactory.getLogger(DocumentumSessionManagerTest.class);

   @Test
   public void testLogon() {
      DocumentumSessionManager sessMgr = new DocumentumSessionManager();
      log.info("Logging in with configuration properties user: \"{}\", password: \"{}\", docbase: \"{}\"", new String[] {getUsername(),getPassword(),getDocbase()});
      try {
         sessMgr.getSession(getUsername(), getPassword(), getDocbase());
      } catch (Exception e) {
         Assert.fail("exception during login: " + e.getMessage());
      }
      finally {
         sessMgr.releaseSession();
      }
   }
}
