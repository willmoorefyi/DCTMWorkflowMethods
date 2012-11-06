/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 6, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfActivityEx;
import com.documentum.bpm.IDfProcessEx;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.triteksol.content.xcp.FolderUtils;
import com.triteksol.workflow.xcp.ProcessTestHarness;
import com.triteksol.workflow.xcp.WorkflowLauncherTestHarness;
import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * Test harness for {@link ProcessUtility}
 * 
 * @author TriTek Solutions, Inc.
 *
 */
public class ProcessUtilityTest extends DocumentumIntegrationTest {
   public static Logger log = LoggerFactory.getLogger(ProcessUtilityTest.class);
   
   //static members created in the BeforeClass method
   private static ProcessTestHarness testHarness;
   private static String packageObjectId;
   
   //non-static methods that are specific to each test
   ProcessUtility procUtil;
   WorkflowLauncherTestHarness workflowLauncher;
   List<IDfId> wfObjIds;
   
   @BeforeClass public static void setupProcessUtilityTest() throws Exception {
      DocumentumIntegrationTest integrationTest = new DocumentumIntegrationTest();
      try {
         integrationTest.setUpSession();
         IDfSession session = integrationTest.getSession();
         testHarness = new ProcessTestHarness();
         testHarness.setupProcess(session, false);
      }
      finally {
         integrationTest.tearDownSession();
      }
   }
   
   @Before public void setupProcessUtilityTest_Before() throws DfException {
      log.debug("Launching workflows for unit test");
      this.workflowLauncher = new WorkflowLauncherTestHarness(getSession());
      this.wfObjIds = workflowLauncher.createWorkflows(ProcessUtilityTest.testHarness.getProcessName(), 2, packageObjectId);
      this.procUtil = new ProcessUtility(getSession());
   }
   
   @After public void teardownProcessUtiltiyTest_After() throws DfException {
      log.debug("Destroying workflows for unit test");
      workflowLauncher.teardownWorkflowTest(wfObjIds);
   }
   
   @Test public void test_getProcess_workflow() {
      log.info("Executing ProcessUtilty getProcess workflow definition test");
      
      IDfProcessEx proc = this.procUtil.getProcess("bad_workflow_name");
      Assert.assertNull("bad workflow should be null", proc);

      proc = this.procUtil.getProcess(ProcessUtilityTest.testHarness.getProcessName());
      Assert.assertNotNull("The process template for a valid workflow should not be null", proc);
   }
   
   @Test public void test_terminateChildWorkflow() {
      log.info("Executing WorkflowCleaner terminate child workflows");
      try {
         IDfProcessEx proc = this.procUtil.getProcess(ProcessUtilityTest.testHarness.getProcessName());
         this.procUtil.terminateChildWorkflows(proc);
         //we should not have any workflow items left
         Assert.assertNull("No child workflow objects should remain for process template", getSession().getObjectByQualification("dm_workflow where process_id='" + proc.getObjectId() + "'"));
         log.info("terminate child workflows successful");
      } catch (Exception e) {
         throw new RuntimeException("Error occurred during terminate child workflows test case", e);
      }
   }
   
   @Test public void test_terminateAuditTrails() {
      log.info("Executing WorkflowCleaner terminate AuditTrails");
      try {
         IDfProcessEx proc = this.procUtil.getProcess(ProcessUtilityTest.testHarness.getProcessName());
         this.procUtil.terminateAuditTrailEntries(proc);
         //we should not have any audit trail items left for the given process id
         Assert.assertNull("No audit trail objects should remain for process template", getSession().getObjectByQualification("dm_audittrail where audited_obj_id='" + proc.getObjectId() + "'"));
         log.info("terminate audit trail successful");
      } catch (Exception e) {
         throw new RuntimeException("Error occurred during terminate trail test case", e);
      }
   }
   
   //TODO this will break if the test cases are run multi-threaded.  We cannot destroy the process template if other test cases are using it
   @Test public void test_destroyProcessTemplateAndFoldersAndActivities() throws DfException {
      // we need to group destroying the activities, the workflow, and the folder in a single test method 
      log.info("Executing WorkflowCleaner destroy ProcessTemplate and activities (all must be destroyed together)");
      IDfSession session = getSession();
      IDfProcessEx proc = procUtil.getProcess(ProcessUtilityTest.testHarness.getProcessName());

      //destroy active workflows
      this.procUtil.terminateChildWorkflows(proc);
      List<IDfActivityEx> activities = this.procUtil.retrieveAttachedActivities(proc);
      for(IDfActivityEx activity : activities) {
         log.info("Found Activity '{}' with name '{}'", activity.getObjectId(), activity.getObjectName());
      }
      IDfFolder folder = (IDfFolder)session.getObject(proc.getFolderId(0));
      List<String> paramNames = this.procUtil.retrieveProcessParameterNames(proc);
      this.procUtil.destroyProcess(proc);
      //verify the process was destroyed
      Assert.assertNull("Process should be destroyed", session.getObjectByQualification(String.format("dm_process where object_name='%s'", ProcessUtilityTest.testHarness.getProcessName() )));
      
      this.procUtil.destroyActivities(activities);
      //verify the activities were destroyed
      for(IDfActivityEx activity : activities) {
         Assert.assertNull("Activities should be destroyed", session.getObjectByQualification("dm_activity where r_object_id='" + activity.getObjectId() + "'"));
      }

      this.procUtil.destroyProcessParameters(paramNames, folder);
      //need to destroy the parameters to successfully clean up the folder
      String folderId = folder.getObjectId().toString();
      FolderUtils folderUtils = new FolderUtils(session);
      folderUtils.destroyFolder(folder);
      Assert.assertNull("Folder should be destroyed", session.getObjectByQualification("dm_folder where r_object_id='" + folderId + "'"));
      
      log.info("successfully destroyed Process Template");
   }
   
   @AfterClass public static void tearDownProcessUtilityTest() throws Exception {
      log.debug("Tearing down the ProcessUtilityTest");
      DocumentumIntegrationTest integrationTest = new DocumentumIntegrationTest(); 
      try {
         integrationTest.setUpSession();
         IDfSession session = integrationTest.getSession();
         ProcessUtilityTest.testHarness.tearDownProcess(session, true, true);
         log.debug("Testing Complete");
      }
      finally {
         integrationTest.tearDownSession();
      }
   }
}
