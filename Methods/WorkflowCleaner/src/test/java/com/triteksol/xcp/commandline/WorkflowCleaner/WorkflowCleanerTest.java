/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 17, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.WorkflowCleaner;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfProcessEx;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.triteksol.junit.DocumentumIntegrationTest;

import com.triteksol.workflow.xcp.ProcessTestHarness;
import com.triteksol.workflow.xcp.WorkflowLauncherTestHarness;
import com.triteksol.xcp.commandline.WorkflowCleaner.WorkflowCleaner;

/**
 * @author wmoore
 *
 */
public class WorkflowCleanerTest extends DocumentumIntegrationTest {
   private static final Logger log = LoggerFactory.getLogger(WorkflowCleanerTest.class);

   @Test public void test_run_wfcount() {
      WorkflowCleanerCommandLineMock mock = new WorkflowCleanerCommandLineMock();
      
      List<String> args = new ArrayList<String>();
      args.add("-" + WorkflowCleaner.OPT_USER);
      args.add(getUsername());
      args.add("-" + WorkflowCleaner.OPT_PWD);
      args.add(getPassword());
      args.add("-" + WorkflowCleaner.OPT_DOCBASE);
      args.add(getDocbase());
      args.add("-" + WorkflowCleaner.OPT_WF);
      //create a long list of workflows so we can verify it is properly parsing names
      args.add("ach,bill payments,close accounts,deluxe switch process,direct deposit process,meacbpm rendezvous process,new account opening process,switching assistance master workflow,trexcp outbound email workflow");

      mock.run(args.toArray(new String[args.size()]));
      
      Assert.assertEquals("Expected to parse input workflow string to ", 9, mock.countWF);
   }

   @Test public void test_deleteProcess() throws Exception {
      log.debug("Setting up workflow cleaner test");      
      ProcessTestHarness testHarness = new ProcessTestHarness();
      IDfProcessEx process = testHarness.setupProcess(getSession(), false);
      try {
         String processName = process.getObjectName();
         WorkflowLauncherTestHarness workflowLauncher = new WorkflowLauncherTestHarness(getSession());
         List<IDfId> wfObjIds = workflowLauncher.createWorkflows(process, 2, null);
         log.debug("Setup complete, executing");
         try {
            log.info("Executing WorkflowCleaner test delete installed process");
            new WorkflowCleaner().cleanWorkflow(processName, getSession(), true, false);
            Assert.assertNull("Process should be destroyed", getSession().getObjectByQualification(String.format("dm_process where object_name='%s'", processName )));
         }
         finally {
            log.debug("Tearing down processes");
            workflowLauncher.teardownWorkflowTest(wfObjIds);
         }
      }
      finally {
         testHarness.tearDownProcess(getSession(), true, true);
         log.debug("Testing complete");
      }
   }

   @Test public void test_deleteUninstalledProcess() throws DfException {
      log.info("Executing WorkflowCleaner test delete uninstalled process");
      final String procName = "tdaqihvhbmaxboehgakvnfhvq";
      IDfProcessEx proc = (IDfProcessEx)getSession().newObject("dm_process");
      proc.setObjectName(procName);
      proc.save();
      log.debug("Created proces template with ID '{}'", proc.getObjectId().toString());
      new WorkflowCleaner().cleanWorkflow(procName, getSession(), true, false);
      Assert.assertNull("Process should be destroyed", getSession().getObjectByQualification("dm_process where object_name='" + procName + "'"));
   }
   
   class WorkflowCleanerCommandLineMock extends WorkflowCleaner {
      private int countWF=0;
      @Override
      public boolean cleanWorkflow(String processName, IDfSession session, boolean allVersions, boolean terminateWfOnly) {
         countWF++;
         return true;
      }
   }
}
