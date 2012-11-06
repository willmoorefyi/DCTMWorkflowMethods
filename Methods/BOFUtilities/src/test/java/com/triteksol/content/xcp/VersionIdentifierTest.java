/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 5, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.content.xcp;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.IDfId;
import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 *
 */
public class VersionIdentifierTest extends DocumentumIntegrationTest {
   private static Logger log = LoggerFactory.getLogger(VersionIdentifierTest.class);
   
   private DocumentumContentTest contentTest;
   private IDfDocument document;
   
   @Before public void setupTest() throws Exception {
      this.contentTest = new DocumentumContentTest(getSession());
      this.document = this.contentTest.createDocumentWithDefaults();
   }

   @Test public void test_identifierConstructor() throws Exception {
      log.debug("Testing creating version identifier");

      IDfDocument doc = this.document;
      IDfId id = doc.getChronicleId();
      VersionIdentifier ident = new VersionIdentifier(id);
      log.debug("Identifier created, beginning tests");
      Assert.assertEquals("Chronicle ID should match for getObjectByQualification", id, ((IDfDocument)getSession().getObjectByQualification(ident.getQualification())).getChronicleId());
      log.debug("Testing with new version");
      doc.checkout();
      IDfDocument docNew = null;
      try {
         docNew = (IDfDocument)getSession().getObject(doc.checkin(false, null));
         Assert.assertFalse("Checkin has created a new document ID, old document ID should not match", doc.getObjectId().equals(docNew.getObjectId()));
         Assert.assertEquals("Chronicle ID should match for getObjectByQualification, even for new version", docNew.getChronicleId(), ((IDfDocument)getSession().getObjectByQualification(ident.getQualification())).getChronicleId());
         log.debug("Testing complete, begin cleanup");
         docNew.destroy();
      }
      catch(Exception e) {
         if(docNew != null) {
            docNew.destroy();
         }
         throw e;
      }
   }
   
   @After public void teardownTest() throws Exception {
      this.contentTest.destroyDocuments();
   }
}
