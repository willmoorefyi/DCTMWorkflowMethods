/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 8, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfWorkflowEx;
import com.documentum.bpm.IDfWorkitemEx;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.client.IDfWorkitem;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.triteksol.content.xcp.DocumentumContentTest;
import com.triteksol.content.xcp.TrexCPVirtualFolder;
import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 *
 * TODO Write tests to handle >1 package
 * TODO Write tests to handle retrieving attachments / packages by Document ID
 */
public class WorkflowUtilityTest extends DocumentumIntegrationTest {
   private static Logger log = LoggerFactory.getLogger(WorkflowUtilityTest.class);
   
   public static final String PROP_PACKAGE_OBJECT_ID = "com.triteksol.xcp.packageObjectId";
   
   //static members created in the BeforeClass method
   private static ProcessTestHarness testHarness;
   //private static String packageObjectId;
   
   private DocumentumContentTest contentTest;
   private IDfDocument document;
   
   //non-static methods that are specific to each test
   private WorkflowLauncherTestHarness workflowLauncher;
   private List<IDfId> wfObjIds;
   private WorkflowUtility workflowUtility;

   @BeforeClass public static void setupProcessUtilityTest() throws Exception {
      log.debug("Setting up WorkflowUtiltity test class");
      DocumentumIntegrationTest integrationTest = new DocumentumIntegrationTest();
      try {
         integrationTest.setUpSession();
         IDfSession session = integrationTest.getSession();
         WorkflowUtilityTest.testHarness = new ProcessTestHarness();
         WorkflowUtilityTest.testHarness.setupProcess(session, true);
         InputStream is = WorkflowUtilityTest.class.getResourceAsStream(ProcessBuilder.WORKFLOW_PROPERTY_FILE_NAME);
         Properties props = new Properties();
         props.load(is);
         //WorkflowUtilityTest.packageObjectId = props.getProperty(PROP_PACKAGE_OBJECT_ID);
      }
      finally {
         integrationTest.tearDownSession();
      }
      log.debug("WorkflowUtiltity test class setup successfully");
   }
   
   @Before public void setupProcessUtilityTest_Before() throws Exception {
      log.debug("Launching workflows for unit test");

      this.contentTest = new DocumentumContentTest(getSession());
      this.document = this.contentTest.createDocumentWithDefaults();
      
      workflowLauncher = new WorkflowLauncherTestHarness(getSession());
      wfObjIds = workflowLauncher.createWorkflows(WorkflowUtilityTest.testHarness.getProcessName(), 2, this.document.getObjectId().toString());
      this.workflowUtility = new WorkflowUtility(getSession());
   }
   
   @After public void teardownProcessUtiltiyTest_After() throws DfException {
      log.debug("Destroying workflows for unit test");
      this.contentTest.destroyDocuments();
      workflowLauncher.teardownWorkflowTest(wfObjIds);
   }
   
   @AfterClass public static void tearDownProcessUtilityTest() throws Exception {
      log.debug("Tearing down the ProcessUtilityTest");
      DocumentumIntegrationTest integrationTest = new DocumentumIntegrationTest();
      try {
         integrationTest.setUpSession();
         IDfSession session = integrationTest.getSession();
         WorkflowUtilityTest.testHarness.tearDownProcess(session, true, true);
         log.debug("Testing Complete");
      }
      finally {
         integrationTest.tearDownSession();
      }
   }
   
   @Test public void test_getActiveWorkitemsFromWorkflow() throws Exception {
      log.debug("testing getting active workitems from workflow");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      List<IDfWorkitemEx> workitems = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow);
      Assert.assertTrue("List of work items should be not null and non-zero", workitems != null && workitems.size() > 0);
      IDfWorkitemEx workitem = workitems.get(0);
      Assert.assertEquals("Workflow object ID should match from retrieved workitem and input workflow", workitem.getWorkflowId(), workflow.getObjectId());
      Assert.assertTrue("Runtime state should be active or dormat", workitem.getRuntimeState() == IDfWorkitem.DF_WI_STATE_DORMANT || workitem.getRuntimeState() == IDfWorkitem.DF_WI_STATE_ACQUIRED);
   }
   
   @Test public void test_saveTrexCPVirtualFolderAsAttachments() throws Exception {
      log.debug("Testing saving the TrexCP Virtual folder as attachments");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      IDfDocument doc2 = this.contentTest.createDocumentWithDefaults();
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      folder.addDocument(this.document, false);
      folder.addDocument(doc2, false);
      this.workflowUtility.saveTrexCPVirtualFolderAsAttachments(getSession(), workitem, folder);

      IDfCollection attachments = workitem.getAttachments();
      Set<IDfId> attachmentIds = new HashSet<IDfId>();
      while(attachments != null && attachments.next()) {
         IDfTypedObject obj = attachments.getTypedObject();
         attachmentIds.add(obj.getId(WorkflowUtility.FIELD_ATTACHMENT_OBJECT_IDS));
      }
      Assert.assertTrue("work item should have document 1 as attachment", attachmentIds.contains(this.document.getObjectId()));
      Assert.assertTrue("work item should have document 2 as attachment", attachmentIds.contains(doc2.getObjectId()));
   }

   @Test public void test_saveTrexCPVirtualFolderAsAttachments_ExistingDocs() throws Exception {
      log.debug("Testing saving the TrexCP Virtual folder as attachments when attachments list exists and has values");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      workitem.addAttachment("dm_document", this.document.getObjectId());
      IDfDocument doc2 = this.contentTest.createDocumentWithDefaults();
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      folder.addDocument(this.document, false);
      folder.addDocument(doc2, false);
      this.workflowUtility.saveTrexCPVirtualFolderAsAttachments(getSession(), workitem, folder);

      IDfCollection attachments = workitem.getAttachments();
      Set<IDfId> attachmentIds = new HashSet<IDfId>();
      while(attachments != null && attachments.next()) {
         IDfTypedObject obj = attachments.getTypedObject();
         attachmentIds.add(obj.getId(WorkflowUtility.FIELD_ATTACHMENT_OBJECT_IDS));
      }
      Assert.assertTrue("work item should have document 1 as attachment", attachmentIds.contains(this.document.getObjectId()));
      Assert.assertTrue("work item should have document 2 as attachment", attachmentIds.contains(doc2.getObjectId()));
   }
   
   @Test public void test_getObjectFromPackage() throws Exception {
      log.debug("Testing retrieving a single object from a package");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      IDfSysObject wfPackage = this.workflowUtility.getObjectFromPackage(workitem);
      Assert.assertEquals("Package object ID should match pre-defined package", this.document.getObjectId(), wfPackage.getObjectId());
   }
   
   @SuppressWarnings("unchecked")
   @Test public void test_getAllPackageObjects() throws Exception {
      log.debug("Testing retrieving all object from a package");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      List<IDfDocument> wfPackages = (List<IDfDocument>)this.workflowUtility.getAllPackageObjects(workitem);
      Assert.assertEquals("Should have 1 package", 1, wfPackages.size());
      Assert.assertEquals("Package object ID should match pre-defined package", this.document.getObjectId(), wfPackages.get(0).getObjectId());
   }

   @Test public void test_getAllPackageObjectsAsVirtualFolder() throws Exception {
      log.debug("Testing retrieving all object from a package as a virtual folder");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      TrexCPVirtualFolder folder = this.workflowUtility.getAllPackageObjectsAsVirtualFolder(workitem, true);
      Assert.assertEquals("Should have 1 package", 1, folder.getAllDocuments(getSession()).size());
      Assert.assertTrue("Package document should be in the folder", folder.containsDocument(this.document));
   }

   @SuppressWarnings("unchecked")
   @Test public void test_getAllAttachmentObjects() throws Exception {
      log.debug("Testing retrieving all object from an attachments list");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      workitem.addAttachment("dm_document", this.document.getObjectId());
      List<IDfDocument> wfAttachments = (List<IDfDocument>)this.workflowUtility.getAllAttachmentObjects(workitem);
      Assert.assertEquals("Should have 1 attachment", 1, wfAttachments.size());
      Assert.assertEquals("Attachment object ID should match created document", this.document.getObjectId(), wfAttachments.get(0).getObjectId());
      
      IDfDocument doc2 = this.contentTest.createDocumentWithDefaults();
      try {
         workitem.addAttachment("dm_document", doc2.getObjectId());
         wfAttachments = (List<IDfDocument>)this.workflowUtility.getAllAttachmentObjects(workitem);
         Assert.assertEquals("Should have 2 attachment", 2, wfAttachments.size());
         Assert.assertEquals("Attachment object ID should match created documnet", doc2.getObjectId(), wfAttachments.get(1).getObjectId());
      }
      finally {
         doc2.destroy();
      }
   }
   
   @Test public void test_getAllAttachmentObjectsAsVirtualFolder() throws Exception {
      log.debug("Testing retrieving all object from an attachments list as a virtual folder");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      workitem.addAttachment("dm_document", this.document.getObjectId());
      TrexCPVirtualFolder folder = this.workflowUtility.getAllAttachmentObjectsAsVirtualFolder(workitem, true);
      Assert.assertEquals("Should have 1 attachment", 1, folder.getAllDocuments(getSession()).size());
      Assert.assertTrue("Attachment document should be in the folder", folder.containsDocument(this.document));
      
      IDfDocument doc2 = this.contentTest.createDocumentWithDefaults();
      try {
         workitem.addAttachment("dm_document", doc2.getObjectId());
         folder = this.workflowUtility.getAllAttachmentObjectsAsVirtualFolder(workitem, true);
         Assert.assertEquals("Should have 2 attachment", 2, folder.getAllDocuments(getSession()).size());
         Assert.assertTrue("Attachment document should be in the folder", folder.containsDocument(this.document));
         Assert.assertTrue("Attachment document 2 should be in the folder", folder.containsDocument(doc2));
      }
      finally {
         doc2.destroy();
      }
   }

   @SuppressWarnings("unchecked")
   @Test public void test_removeAllAttachments() throws Exception {
      log.debug("Testing remove all object from an attachments list");
      IDfId wfObjId = wfObjIds.get(0);
      IDfWorkflowEx workflow = (IDfWorkflowEx)getSession().getObject(wfObjId);
      IDfWorkitemEx workitem = this.workflowUtility.getActiveWorkitemsFromWorkflow(workflow).get(0);
      workitem.addAttachment("dm_document", this.document.getObjectId());
      IDfDocument doc2 = this.contentTest.createDocumentWithDefaults();
      try {
         workitem.addAttachment("dm_document", doc2.getObjectId());
         this.workflowUtility.removeAllAttachments(workitem);
         List<IDfDocument> wfAttachments = (List<IDfDocument>)this.workflowUtility.getAllAttachmentObjects(workitem);
         Assert.assertEquals("Should have 0 attachments after removal", 0, wfAttachments.size());
      }
      finally {
         doc2.destroy();
      }
   }
}
