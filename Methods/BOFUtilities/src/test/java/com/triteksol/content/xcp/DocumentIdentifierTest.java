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
public class DocumentIdentifierTest extends DocumentumIntegrationTest {
   private static Logger log = LoggerFactory.getLogger(DocumentIdentifierTest.class);
   
   private DocumentumContentTest contentTest;
   private IDfDocument document;
   
   @Before public void setupTest() throws Exception {
      this.contentTest = new DocumentumContentTest(getSession());
      this.document = this.contentTest.createDocumentWithDefaults();
   }
   
   @After public void teardownTest() throws Exception {
      this.contentTest.destroyDocuments();
   }
   
   
   @Test public void test_identifierConstructor() throws Exception {
      log.debug("Testing creating document identifier");
      IDfDocument doc = this.document;
      IDfId id = doc.getObjectId();
      DocumentIdentifier ident = new DocumentIdentifier(id);
      log.debug("Identifier created, beginning tests");
      Assert.assertEquals("Document ID should match for getObjectByQualification", id, getSession().getObjectByQualification(ident.getQualification()).getObjectId());
      log.debug("Testing with new version");
      doc.checkout();
      IDfDocument docNew = null;
      try {
         docNew = (IDfDocument)getSession().getObject(doc.checkin(false, null));
         Assert.assertFalse("Checkin has created a new document ID, old document ID should not match", doc.getObjectId().equals(docNew.getObjectId()));
         Assert.assertFalse("Document ID should no longer match", docNew.getObjectId().equals(((IDfDocument)getSession().getObjectByQualification(ident.getQualification())).getObjectId()));
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
}
