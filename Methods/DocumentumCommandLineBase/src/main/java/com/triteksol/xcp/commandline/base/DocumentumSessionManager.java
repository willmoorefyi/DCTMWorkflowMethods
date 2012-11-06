/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 9, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.base;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.DfAuthenticationException;
import com.documentum.fc.client.DfIdentityException;
import com.documentum.fc.client.DfPrincipalException;
import com.documentum.fc.client.DfServiceException;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfLoginInfo;

/**
 * @author wmoore
 *
 */
public class DocumentumSessionManager {
   private IDfSessionManager sessionManager;
   private IDfSession session;

   public IDfSession getSession(String userName, String pwd, String docBase) throws DfIdentityException, 
         DfAuthenticationException, DfPrincipalException, DfServiceException, DfException {
      IDfClientX clientX = new DfClientX();
      IDfClient client = clientX.getLocalClient();
      IDfLoginInfo loginInfo = clientX.getLoginInfo();
      IDfSessionManager sessionManager = client.newSessionManager();
      loginInfo.setUser(userName);
      loginInfo.setPassword(pwd);
      sessionManager.setIdentity(docBase, loginInfo);
      this.sessionManager = sessionManager;
      this.session = this.sessionManager.newSession(docBase);
      return this.session;
   }
   
   public void releaseSession() {
      if (this.sessionManager != null) {
         if (this.session != null) {
            this.sessionManager.release(this.session);
            this.session = null;
         }
         this.sessionManager = null;
      }
   }
}
