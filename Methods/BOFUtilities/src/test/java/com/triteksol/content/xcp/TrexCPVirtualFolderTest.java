/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 5, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.content.xcp;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;
import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 *
 */
public class TrexCPVirtualFolderTest extends DocumentumIntegrationTest {
   private static Logger log = LoggerFactory.getLogger(TrexCPVirtualFolderTest.class);

   private DocumentumContentTest contentTest;
   private IDfDocument document;
   
   @Before public void setupTest() throws Exception {
      this.contentTest = new DocumentumContentTest(getSession());
      this.document = this.contentTest.createDocumentWithDefaults();
   }
   
   @After public void teardownTest() throws Exception {
      this.contentTest.destroyDocuments();
   }

   @Test public void test_documents_empty() throws DfException {
      //test an empty virtual folder
      log.info("Testing virtual folders with documents - empty folder");
      List<IDfDocument> allDocs;
      IDfDocument docFromFolder;
      
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      Assert.assertNull("Empty constructed folder should have null for folder ID", folder.getFolderID());
      Assert.assertTrue("Empty constructed folder should have no documents", folder.getAllDocuments(getSession()).isEmpty());

      //test adding documents to the folder
      folder.addDocument(this.document, false);
      allDocs = folder.getAllDocuments(getSession());
      Assert.assertEquals("Should only have 1 document in the list of all documents in the folder after adding 1 document", 1, allDocs.size());
      docFromFolder = allDocs.get(0);
      Assert.assertEquals("Document in folder should match document passed-in", this.document.getObjectId(), docFromFolder.getObjectId());
      
      //test re-adding the same document
      folder.addDocument(this.document, false);
      allDocs = folder.getAllDocuments(getSession());
      Assert.assertEquals("Should only have 1 document in the list of all documents in the folder after adding the same document twice", 1, allDocs.size());
      docFromFolder = allDocs.get(0);
      Assert.assertEquals("Document in folder should match document passed-in", this.document.getObjectId(), docFromFolder.getObjectId());
      
      //test adding a second document
      IDfDocument doc2 = this.contentTest.createDocumentWithDefaults();
      try {
         folder.addDocument(doc2, false);
         allDocs = folder.getAllDocuments(getSession());
         Assert.assertEquals("Should have 2 documents in the list of all documents in the folder", 2, allDocs.size());
         Assert.assertEquals("Should have 2 results in folder ID", folder.getFolderID().split(TrexCPVirtualFolder.SEPARATOR_CHAR).length, 2);
      }
      finally {
         doc2.destroy();
      }
   }
   
   @Test public void test_documents_nonempty() throws DfException {
      log.info("Testing virtual folders with documents - empty folder");
      List<IDfDocument> allDocs;
      IDfDocument docFromFolder;
      //test creating a new virtual folder with a single document
      DocumentumIdentifier docIdent = new DocumentIdentifier(this.document.getObjectId());
      List<DocumentumIdentifier> docIdents = new ArrayList<DocumentumIdentifier>();
      docIdents.add(docIdent);
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder(docIdents);
      allDocs = folder.getAllDocuments(getSession());
      Assert.assertEquals("Should only have 1 document in the list of all documents in the newly constructed folder", 1, allDocs.size());
      docFromFolder = allDocs.get(0);
      Assert.assertEquals("Document in folder should match document passed-in", this.document.getObjectId(), docFromFolder.getObjectId());
      
      //test creating a new virtual folder with a string identifier
      TrexCPVirtualFolder folder2 = new TrexCPVirtualFolder(folder.getFolderID());
      allDocs = folder2.getAllDocuments(getSession());
      Assert.assertEquals("Should only have 1 document in the list of all documents in the newly constructed folder", 1, allDocs.size());
      docFromFolder = allDocs.get(0);
      Assert.assertEquals("Document in folder should match document passed-in", this.document.getObjectId(), docFromFolder.getObjectId());
      List<DocumentumIdentifier> sourceIdentifiers = folder.getAllDocumentumIdentifiers();
      List<DocumentumIdentifier> targetIdentifiers = folder2.getAllDocumentumIdentifiers();
      Assert.assertTrue("Target folder should contain all the documents as the source folder when built with the same ID", 
            sourceIdentifiers.containsAll(targetIdentifiers) && targetIdentifiers.containsAll(sourceIdentifiers));
   }

   @Test public void test_versions_empty_noversioning() throws DfException {
      //test an empty virtual folder
      log.info("Testing virtual folders with documents");
      List<IDfDocument> allDocs;
      IDfDocument docFromFolder;
      
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      Assert.assertNull("Empty constructed folder should have null for folder ID", folder.getFolderID());
      Assert.assertTrue("Empty constructed folder should have no documents", folder.getAllDocuments(getSession()).isEmpty());
      
      //test adding documents to the folder
      folder.addDocument(this.document, true);
      allDocs = folder.getAllDocuments(getSession());
      Assert.assertEquals("Should only have 1 document in the list of all documents in the folder after adding 1 document", 1, allDocs.size());
      docFromFolder = allDocs.get(0);
      Assert.assertEquals("Document in folder should match document passed-in", this.document.getObjectId(), docFromFolder.getObjectId());
      
      //test re-adding the same document
      folder.addDocument(this.document, true);
      allDocs = folder.getAllDocuments(getSession());
      Assert.assertEquals("Should only have 1 document in the list of all documents in the folder after adding the same document twice", 1, allDocs.size());
      docFromFolder = allDocs.get(0);
      Assert.assertEquals("Document in folder should match document passed-in", this.document.getObjectId(), docFromFolder.getObjectId());
   }
   
   @Test public void test_versions_nonempty() throws DfException {
      //test versioning
      log.debug("Testing with new version");
      List<IDfDocument> allDocs;
      IDfDocument docFromFolder;
      
      IDfDocument doc = this.document;
      IDfDocument docNew = null;
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      folder.addDocument(doc, true);
      try {
         //test versioning a document in the folder
         doc.checkout();
         docNew = (IDfDocument)getSession().getObject(doc.checkin(false, null));
         Assert.assertFalse("Checkin has created a new document ID, old document ID should not match", doc.getObjectId().equals(docNew.getObjectId()));
         allDocs = folder.getAllDocuments(getSession());
         Assert.assertEquals("Should only have 1 document in the list of all documents in the folder after versioning document in the folder", 1, allDocs.size());
         docFromFolder = allDocs.get(0);
         Assert.assertEquals("Document added to folder by version should match when the document is versioned", docNew.getObjectId(), docFromFolder.getObjectId());

      }
      finally {
         if(docNew != null) {
            log.debug("Testing complete, begin cleanup");
            docNew.destroy();
         }
         else {
            log.debug("docNew is null, must have been destroyed in test");
         }
      }
   }
      

   @Test public void test_versions_empty() throws DfException {
      log.debug("Testing with new version");
      List<IDfDocument> allDocs;
      IDfDocument docFromFolder;
      
      IDfDocument doc = this.document;
      IDfDocument docNew = null;
      try {
         //test versioning a document in the folder
         doc.checkout();
         docNew = (IDfDocument)getSession().getObject(doc.checkin(false, null));
         //test creating a new virtual folder with a single document
         DocumentumIdentifier docIdent = new VersionIdentifier(this.document.getChronicleId());
         List<DocumentumIdentifier> docIdents = new ArrayList<DocumentumIdentifier>();
         docIdents.add(docIdent);
         TrexCPVirtualFolder folder = new TrexCPVirtualFolder(docIdents);
         allDocs = folder.getAllDocuments(getSession());
         Assert.assertEquals("Should only have 1 document in the list of all documents in the newly constructed folder", 1, allDocs.size());
         docFromFolder = allDocs.get(0);
         Assert.assertEquals("Document in folder should match chronicle series of original document used to create folder", doc.getChronicleId(), docFromFolder.getChronicleId());
         Assert.assertEquals("Document in folder should match object ID of new document when new version exists", docNew.getObjectId(), docFromFolder.getObjectId());
         
         //test creating a new virtual folder with a string identifier
         TrexCPVirtualFolder folder2 = new TrexCPVirtualFolder(folder.getFolderID());
         allDocs = folder2.getAllDocuments(getSession());
         Assert.assertEquals("Should only have 1 document in the list of all documents in the newly constructed folder", 1, allDocs.size());
         docFromFolder = allDocs.get(0);
         Assert.assertEquals("Document in folder should match chronicle series of document used to create the folder when new version exists", docNew.getObjectId(), docFromFolder.getObjectId());
         List<DocumentumIdentifier> sourceIdentifiers = folder.getAllDocumentumIdentifiers();
         List<DocumentumIdentifier> targetIdentifiers = folder2.getAllDocumentumIdentifiers();
         Assert.assertTrue("Target folder should contain all the documents as the source folder when built with the same ID", 
               sourceIdentifiers.containsAll(targetIdentifiers) && targetIdentifiers.containsAll(sourceIdentifiers));
         
      }
      finally {
         if(docNew != null) {
            log.debug("Testing complete, begin cleanup");
            docNew.destroy();
         }
         else {
            log.debug("docNew is null, must have been destroyed in test");
         }
      }
   }
   
   @Test(expected = Exception.class) public void test_badDocId() {
      log.debug("testing bad doc ID");
      new TrexCPVirtualFolder("bad document ID");
   }
   
   @Test public void containsDocument_test() throws DfException {
      log.debug("Testing contains document function of TrexCP Virtual Folder");
      IDfDocument docOrig = this.document;
      IDfDocument docNew = null;
      IDfDocument doc2 = this.contentTest.createDocumentWithDefaults();
      try {
         TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
         folder.addDocument(docOrig, true);
         Assert.assertTrue("Folder must contain document just added", folder.containsDocument(new VersionIdentifier(docOrig)));
         Assert.assertFalse("Folder should not contain document ont added yet", folder.containsDocument(new DocumentIdentifier(doc2)));

         docOrig.checkout();
         docNew = (IDfDocument)getSession().getObject(docOrig.checkin(false, null));

         Assert.assertTrue("Folder must contain old version of document previously added using version identifier", folder.containsDocument(new VersionIdentifier(docOrig)));
         Assert.assertTrue("Folder must contain new version of document previously added using version identifier", folder.containsDocument(new VersionIdentifier(docNew)));
         Assert.assertFalse("Folder should not contain document ont added yet", folder.containsDocument(new DocumentIdentifier(doc2)));
         
         folder.addDocument(new DocumentIdentifier(doc2));
         Assert.assertTrue("Folder must contain document just added", folder.containsDocument(new DocumentIdentifier(doc2)));
      }
      finally {
         doc2.destroy();
         if(docNew != null) {
            docNew.destroy();
         }
      }
   }
   
   @Test public void containsDocument_test_DocumentbyIDfDoc() throws DfException {
      log.debug("Testing contains document function of TrexCP Virtual Folder with Document native type - Document");
      IDfDocument doc = this.document;
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      folder.addDocument(new DocumentIdentifier(doc));
      Assert.assertTrue("Folder should contain document added as DocumentIdentifier, even when checking by IDfDocument", folder.containsDocument(doc));
   }
   
   @Test public void containsDocument_test_VersionbyIDfDoc() throws DfException {
      log.debug("Testing contains document function of TrexCP Virtual Folder with Document native type - Version");
      IDfDocument doc = this.document;
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      folder.addDocument(new VersionIdentifier(doc));
      Assert.assertTrue("Folder should contain document added as DocumentIdentifier, even when checking by IDfDocument", folder.containsDocument(doc));
   }
   
   @Test public void removeDocument_test() throws DfException {
      log.debug("Test removing documents");
      IDfDocument doc = this.document;
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      
      Assert.assertFalse("Document has not been added, removeDocument should return false", folder.removeDocument(doc));
      folder.addDocument(doc, true);
      Assert.assertTrue("Removing document filed in folder should return true", folder.removeDocument(new VersionIdentifier(doc)));
   }
   
   @Test public void removeDocument_test_nativeDocument() throws DfException {
      log.debug("Test removing documents");
      IDfDocument doc = this.document;
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      
      folder.addDocument(doc, true);
      Assert.assertTrue("Removing document filed in folder should return true", folder.removeDocument(doc));
   }
   
   
   
   /*
   IDfDocument getRandomDocument(IDfSession session, String typeName, IDfId toExclude) throws DfException {
      String localTypeName = typeName == null || typeName.length() == 0 ? "dm_document" : typeName;
      Integer localRandom = new Integer(100);
      IDfEnumeration results = session.getObjectsByQuery(String.format("SELECT r_object_id, i_vstamp, r_object_type, r_aspect_name, i_is_replica, i_is_reference FROM %1$s ENABLE(RETURN_TOP %2$s)", localTypeName, localRandom.toString()), localTypeName);
      
      IDfDocument returnVal = null;
      int rand = new Random().nextInt(localRandom.intValue());
      for(int i=0; i < rand && results.hasMoreElements(); i++) {
         returnVal = (IDfDocument)results.nextElement();
      }
      
      if(returnVal.getObjectId().equals(toExclude)) {
         log.debug("Re-running random doc retrieval as we are using our excluded input value");
         return getRandomDocument(session, localTypeName, toExclude);
      }
      return returnVal;
   }
   */
}
