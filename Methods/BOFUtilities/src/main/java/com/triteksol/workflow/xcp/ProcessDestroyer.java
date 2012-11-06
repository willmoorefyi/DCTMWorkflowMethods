/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 7, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfActivityEx;
import com.documentum.bpm.IDfProcessEx;
import com.documentum.bpm.sdt.DfStructuredDataTypeDAOFactory;
import com.documentum.bpm.sdt.IDfStructuredDataTypeDAO;
import com.documentum.bpm.sdt.SDTNotFoundException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * Utility class to destroy a process.  Useful for testing.
 * 
 * @author wmoore
 *
 */
public class ProcessDestroyer {
   private static final Logger log = LoggerFactory.getLogger(ProcessBuilder.class);

   private IDfSession session;
   private ProcessUtility procUtil; 
   
   public ProcessDestroyer(IDfSession session) {
      this.session = session;
      procUtil = new ProcessUtility(this.session);
   }
   
   /**
    * Destroy the active process instances for a given process
    * @param processName The name of the process which has workflows to delete, which will then be used to retrieve the underlying process
    */
   public void destroyWorkflowInstances(String processName) {
      assert processName != null && processName.length() > 0 : "Cannot destroy instances of a null process";
      IDfProcessEx proc = procUtil.getProcess(processName);
      destroyWorkflowInstances(proc);
   }
   
   /**
    * Destroy the active process instances for a given process
    * @param process The name of the process for which workflows will be deleted
    */
   public void destroyWorkflowInstances(IDfProcessEx process) {
      assert process != null : "Cannot destroy instances of a null process";
      try {
         log.debug("Destroying workflow instances of process '{}'", process.getObjectName());
      }
      catch (DfException e) {
         throw new RuntimeException("Error Occurred trying to evalute Process to destroy workflow instances", e);
      }
      procUtil.terminateChildWorkflows(process);
   }

   /**
    * Destroy the process template by deleting all active workflows (if terminateWorkflows == true) and then uninstalling and deleting the process template.
    * @param processName The name of process to delete, which will then be used to retrieve the underlying process
    * @param terminateWorkflows If true, then terminate child workflows before destroy process.  Setting this to "false" can be used as a sanity check
    * @return True if the workflow was successfully cleaned up, false otherwise
    */
   public boolean destroyProcessTemplate(String processName, boolean terminateWorkflows) {
      assert processName != null && processName.length() > 0 : "Cannot destroy a null Process Template";
      IDfProcessEx proc = procUtil.getProcess(processName);
      if(proc != null) {
         log.debug("Found process with name '{}', destroying", processName);
         destroyProcessTemplate(proc, null, terminateWorkflows);
         return true;
      }
      else {
         log.debug("No process with name '{}', found, ignoring", processName);
         return false;
      }
   }
   
   /**
    * Destroy the process template by deleting all active workflows (if terminateWorkflows == true) and then uninstalling and deleting the process template.
    * @param processName The name of process to delete, which will then be used to retrieve the underlying process
    * @param terminateWorkflows If true, then terminate child workflows before destroy process.  Setting this to "false" can be used as a sanity check
    * @return True if the workflow was successfully cleaned up, false otherwise
    */
   public void destroyProcessTemplate(IDfProcessEx proc, boolean terminateWorkflows) {
      assert proc != null : "Cannot destroy instances of a null process";
      destroyProcessTemplate(proc, null, terminateWorkflows);
   }

   /**
    * Destroy the process template by deleting all active workflows (if terminateWorkflows == true) and then uninstalling and deleting the process template.
    * @param processName The process to delete
    * @param folder The folder where the process and its parameters are contained
    * @param terminateWorkflows If true, then terminate the workflows before destroy process.  Setting this to "false" can be used as a sanity check
    * @return True if the process was successfully cleaned up, false otherwise
    */
   public void destroyProcessTemplate(IDfProcessEx proc, IDfFolder folder, boolean terminateWorkflows) {
      try {
         log.debug("Cleaning up workflow \"{}\", cleaning up child workflows? {}", proc.getObjectName(), terminateWorkflows);
      } catch (DfException e) {
         throw new RuntimeException("Error Occurred trying to evalute Process to destroy the Process Template", e);
      }

      if (terminateWorkflows) {
         // terminate the child workflow entries first - this is a sanity check so we want to stop if we blow up
         procUtil.terminateChildWorkflows(proc);
      }

      // delete the audit trail entries for the process
      try {
         procUtil.terminateAuditTrailEntries(proc);
      }
      catch (Exception e) {
         log.error("Unable to terminate Audit trail entries, continuing ...");
      }

      // retrieve the activities
      List<IDfActivityEx> activities = procUtil.retrieveAttachedActivities(proc);
      
      try {
         // retrieve the process parameters
         List<String> paramNames = procUtil.retrieveProcessParameterNames(proc);
   
         try {
            // retrieve the parent IDs of the process variables
            procUtil.resetProcessVariableParents(proc);
      
            // uninstall and destroy the process completely
            procUtil.destroyProcess(proc);
         }
         finally {
            // destroy the process parameters
            if(folder != null) {
               procUtil.destroyProcessParameters(paramNames, folder);
            }
         }
      }
      finally {
         // now that the process is uninstalled, clean up the activities
         procUtil.destroyActivities(activities);
      }

      log.debug("Completed destroying process");
   }
   
   /**
    * TODO move this to a separate SDT Destroyer class
    * Tear down (destroy) the specified SDT
    * @param session The Documentum Session
    * @param sdtName the SDT to destroy
    * @throws Exception
    */
   public void teardownSDT(String sdtName) throws Exception {
      log.debug("Destroying the SDT");
      IDfStructuredDataTypeDAO sdtDAO = DfStructuredDataTypeDAOFactory.getDocbaseDAO(this.session.getSessionManager(), this.session.getDocbaseName());
      try {
         sdtDAO.lookupStructuredDataType(sdtName);
         log.debug("Deleting Structured Data Type '{}'", sdtName);
         sdtDAO.deleteStructuredDataType(sdtName);
      } catch(SDTNotFoundException ex) {
         log.debug("no SDT '{}' found, ignoring", sdtName);
      }
      
   }
    
   /**
    * Retrieve the folder a given process is filed into. Note that this method
    * assumes the process is only filed in the primary folder.
    * 
    * @param proc
    *           The process to retrieve the primary folder
    * @return The folder the process template is stored in
    */
   IDfFolder retrieveFolder(IDfProcessEx proc, IDfSession session) {
      assert proc != null : "Process template to destroy is required!";

      try {
         IDfFolder folder = (IDfFolder) session.getObject(proc.getFolderId(0));
         return folder;
      } catch (DfException e) {
         throw new RuntimeException("An error occurred while deleting the process template folder", e);
      }
   }
}
