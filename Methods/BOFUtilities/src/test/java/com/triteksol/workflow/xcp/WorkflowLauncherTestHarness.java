/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 27, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfActivityEx;
import com.documentum.bpm.IDfProcessEx;
import com.documentum.bpm.IDfWorkitemEx;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfWorkflow;
import com.documentum.fc.client.IDfWorkflowBuilder;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfList;
import com.documentum.fc.common.IDfId;

/**
 * @author wmoore
 *
 */
public class WorkflowLauncherTestHarness {
   private static final Logger log = LoggerFactory.getLogger(WorkflowLauncherTestHarness.class);

   static final int ACQUIRE_RETRY_COUNT = 10;
   static final int ACQUIRE_RETRY_SLEEP_MILLIS = 1000;
   
   private IDfSession session;
   
   /**
    * Create a new WorkflowLauncherTestHarness
    * @param session
    */
   public WorkflowLauncherTestHarness(IDfSession session) {
      this.session = session;
   }
   
   /**
    * Create workflows for the passed-in process
    * @param processName
    * @param numProcesses
    * @return
    * @throws DfException
    */
   public List<IDfId> createWorkflows(String processName, Integer numProcesses) throws DfException {
      return createWorkflows(processName, numProcesses, null);
   }
   
   /**
    * Create workflows for the passed-in process
    * @param processName
    * @param numProcesses
    * @param packageObjectId
    * @return
    * @throws DfException
    */
   public List<IDfId> createWorkflows(String processName, Integer numProcesses, String packageObjectId) throws DfException {
      IDfProcessEx dfProc = (IDfProcessEx) session.getObjectByQualification(String.format("dm_process where object_name = '%s'", processName));
      log.debug("Retrieved IDfProcess instance {}", dfProc == null ? null : dfProc.getObjectName());
      return createWorkflows(dfProc, numProcesses, packageObjectId);
   }
   
   /**
    * Create workflows for the passed-in process
    * @param process
    * @param numProcesses
    * @return
    * @throws DfException
    */
   public List<IDfId> createWorkflows(IDfProcessEx process, Integer numProcesses) throws DfException {
      return createWorkflows(process, numProcesses, null);
   }
   
   /**
    * Create workflows for the passed-in process
    * @param process
    * @param numProcesses
    * @param packageObjectId
    * @return
    * @throws DfException
    */
   public List<IDfId> createWorkflows(IDfProcessEx process, Integer numProcesses, String packageObjectId) throws DfException {
      assert process != null : "Process cannot be null when attempting to create processes!"; 
      log.debug("Launching {} number of processes for process name '{}'", numProcesses, process.getObjectName());

      List<IDfId> returnVal = new ArrayList<IDfId>();
      for(int i=0; i<numProcesses; i++) {
         IDfWorkflowBuilder wfBuilder = session.newWorkflowBuilder(process.getObjectId());
         log.debug("Retrieved workflow builder for process ");
         //retrieve the start activity to handle process parameters - only works if we have 1 start activity, which should be true for test workflows
         IDfActivityEx startActivity = (IDfActivityEx)session.getObject((IDfId)wfBuilder.getStartActivityIds().get(0));
         wfBuilder.initWorkflow();
         wfBuilder.runWorkflow();
         processPackageProperties(wfBuilder, startActivity, packageObjectId);
         IDfWorkflow wfObj = wfBuilder.getWorkflow();
         returnVal.add(wfObj.getObjectId());
         log.debug("Created workflow object '{}'", wfObj.getObjectId());
      }
      log.debug("Finished creating workflows");
      return returnVal;
   }
   
   /**
    * Sets the package properties for a given workflow to a specified packageObjectId
    * @param builder The WorkflowBuilder being used to launch the workflow
    * @param startActivity The starting activity for the process
    * @param packageObjectId The object ID that the package values should be set to
    * @throws DfException
    */
   void processPackageProperties(IDfWorkflowBuilder builder, IDfActivityEx startActivity, String packageObjectId) throws DfException {
      log.debug("Parsing packages for workflow");
      //only process packages if the workflow has them
      //NOTE: This doesn't guarantee we have any packages.  PackageCount is always equal to PortCount, even if the workflow has no real "packages"
      if(startActivity.getPackageCount() > 0) {
         //If the package is required, this will cause the workflow to "stall" waiting for a real package to be committed
         IDfId objectDfId = packageObjectId == null || packageObjectId.length() == 0 ? DfId.DF_NULLID : new DfId(packageObjectId);
         int portCount = startActivity.getPortCount();
         for(int i=0; i<portCount; i++) {
            //only try to set the package on input ports, don't touch outputs yet
            String portType = startActivity.getPortType(i);
            String packageType = startActivity.getPackageType(i);
            if("INPUT".equalsIgnoreCase(portType) && packageType != null & packageType.length() > 0) {
               String inputPackageName = startActivity.getPackageName(i);
               //valid packages must have a name
               if(inputPackageName != null && inputPackageName.length() > 0) {
                  DfList list = new DfList();
                  list.append(objectDfId);
                  builder.addPackage(startActivity.getObjectName(), startActivity.getPortName(i), inputPackageName, startActivity.getPackageType(i), "", false, list);
               }
            }
         }
      }
   }
   
   /**
    * Tear down the created workflows by destroying them
    * @param session 
    * @param objIds
    */
   public void teardownWorkflowTest(List<IDfId> objIds) {
      log.info("Tearing down workflow instances");
      try {
         //IDfSession session = getSession();
         for(IDfId objId : objIds) {
            IDfWorkflow wfObj = (IDfWorkflow) session.getObjectByQualification(String.format("dm_workflow where r_object_id = '%s'", objId.toString()));
            if(wfObj != null) {
               log.debug("Destroying object with id '{}' as it was not destroyed during the test case", objId);
               try {
                  //abort the workflow object
                  wfObj.abort();
               } catch (Exception e) {
                  log.warn("Unable to abort workflow '{}', trying to destroy it", objId, e);
               }
               try {
                  //destroy the workflow object
                  wfObj.destroy();
               } catch (Exception e) {
                  log.error("Unable to destroy workflow '{}' after attempting to abort it", objId, e);
               }
            } else {
               log.debug("No object with ID '{}', ignoring", objId);
            }
         }
      } catch(Exception e) {
         throw new RuntimeException("Could not destroy workflow instances due to underlying system error", e);
      }
   }
   
   /**
    * Loop to retry work item acquisition. Useful for testing workflow methods, where the work needs to be acquired.
    * The vstamp can't be zero as the work item will need to be updated at least once prior to moving into the step
    * @param workitem
    * @throws Exception
    */
   public void workitemAcquireRetry(IDfWorkitemEx workitem) throws Exception {
      for(int i=0; i<ACQUIRE_RETRY_COUNT; i++) {
         workitem.fetch(null);
         try {
            if(workitem.getVStamp() == 0) {
               log.debug("Work item has not transitioned, re-fetching, sleeping, retrying");
            } else {
               workitem.acquire();
               return;
            }
         } catch(Exception e) {
            //do nothing
         }
         Thread.sleep(ACQUIRE_RETRY_SLEEP_MILLIS);
      }
      throw new RuntimeException("Could not acquire work item");
   }
}
