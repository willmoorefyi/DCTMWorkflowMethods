/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 27, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfProcessEx;
import com.documentum.bpm.sdt.IDfPrimitiveType;
import com.documentum.bpm.sdt.IDfStructuredDataType;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.triteksol.content.xcp.FolderUtils;
import com.triteksol.workflow.xcp.ProcessBuilder;
import com.triteksol.workflow.xcp.ProcessDestroyer;
import com.triteksol.workflow.xcp.SDTBuilder;
import com.triteksol.workflow.xcp.SDTUtility;
import com.triteksol.workflow.xcp.WorkflowLauncherTestHarness;


/**
 * Helper test class that can be used to generate a default workflow utilizing many Documentum functions. Replaces the ProcessBuilder in many scenarios for simple workflows 
 * 
 * @author wmoore
 *
 */
public class ProcessTestHarness  {
   private static final Logger log = LoggerFactory.getLogger(ProcessTestHarness.class);
   
   public static final String PROP_PACKAGENAME = "com.triteksol.xcp.packagename";
   public static final String PROP_QUEUENAME = "com.triteksol.xcp.queue";
   
   private Properties defaultProps;
   
   private IDfId processId;
   private String processName;
   private String sdtName;
   private String packageName;
   private IDfId folderId;
   private String queueName;
   
   /**
    * Instantiate a new process test harness
    * @throws IOException
    */
   public ProcessTestHarness() throws IOException {
      InputStream is = WorkflowLauncherTestHarness.class.getResourceAsStream(ProcessBuilder.WORKFLOW_PROPERTY_FILE_NAME);
      defaultProps = new Properties();
      defaultProps.load(is);

      this.processName = defaultProps.getProperty(ProcessBuilder.PROP_PROCESSNAME);
      this.packageName = defaultProps.getProperty(PROP_PACKAGENAME);
      this.queueName = defaultProps.getProperty(PROP_QUEUENAME);
   }
   
   /**
    * Setup a new Process template
    * @param session The Documentum session
    * @param withPackage If True, adds a package to the workflow, otherwise creates a workflow without a package
    * @return
    * @throws DfException
    * @throws Exception
    */
   public IDfProcessEx setupProcess(IDfSession session, boolean withPackage) throws DfException, Exception {
      //read local values for now      
      String folderLocation = defaultProps.getProperty(ProcessBuilder.PROP_PROCESS_FOLDERLOCATION);
      String folderName = defaultProps.getProperty(ProcessBuilder.PROP_PROCESS_FOLDERNAME);
      
      if(this.processName == null || this.packageName == null || this.queueName == null || 
            folderLocation == null || folderName == null) {
         throw new RuntimeException(String.format("Required input property not set for test harness buildout. Set all required properties in file '%s' on classpath", ProcessBuilder.WORKFLOW_PROPERTY_FILE_NAME));
      }
      
      try {
         log.debug("Creating folder '{}/{}'", folderLocation, folderName);
         FolderUtils folderUtils = new FolderUtils(session);
         IDfFolder folder = folderUtils.createFolderSafe(folderLocation, folderName);
         this.folderId = folder.getObjectId();
         
         log.debug("Creating default Structured Data Type");
         IDfStructuredDataType sdt = SDTBuilder.startBuilding(session).addAttribute("attr1", IDfPrimitiveType.STRING, "").addAttribute("attr2", IDfPrimitiveType.STRING, "").buildSDT();
         this.sdtName = sdt.getName();
   
         log.debug("Creating process '{}'", this.processName);
         ProcessBuilder builder = ProcessBuilder.startBuilding(session);
         IDfProcessEx process;
         if(withPackage) {
            process = builder.setProcessName(processName).addPackage(packageName)
                  .addSimpleActivity("activity1").addQueueActivity("activity2", this.queueName).addQueueActivity("activity3", this.queueName)
                  .addQueueActivity("activity4", this.queueName).startProcess().addSDTVariable(sdt.getName(), sdt)
                  .addSimpleProcessVariable("variable1", IDfPrimitiveType.STRING).buildProcess();
         } 
         else {
            process = builder.setProcessName(processName)
                  .addSimpleActivity("activity1").addQueueActivity("activity2", this.queueName).addQueueActivity("activity3", this.queueName)
                  .addQueueActivity("activity4", this.queueName).startProcess().addSDTVariable(sdt.getName(), sdt)
                  .addSimpleProcessVariable("variable1", IDfPrimitiveType.STRING).buildProcess();
         }
         this.processId = process.getObjectId();
         log.debug("Process successfully created");
         return process;
      }
      catch(Exception e) {
         log.error("Error occurred while buildng test harness, Attempting to tear down objects already built");
         tearDownProcess(session, true);
         throw e;
      }

   }
   
   /**
    * Tear down the process objects created by this test harness.  Equivalent to calling {@link #tearDownProcess(IDfSession, boolean, boolean)} with <em>true, false</em>
    */
   public void tearDownProcess(IDfSession session) {
      tearDownProcess(session, true, false);
   }

   /**
    * Tear down the process objects created by this test harness.  Equivalent to calling {@link #tearDownProcess(IDfSession,  boolean, boolean)} with <em>{cleanWorkflows}, false</em>
    * @param cleanWorkflows True to clean up any errant workflow objects prior to destroying the process, false to ignore (and throw an error if active workflows still exist)
    */
   public void tearDownProcess(IDfSession session, boolean cleanWorkflows) {
      tearDownProcess(session, cleanWorkflows, false);
   }
   
   /**
    * Tear down the process objects created by this test harness. 
    * @param session The Documentum Session
    * @param cleanWorkflows True to clean up any errant workflow objects prior to destroying the process, false to ignore (and throw an error if active workflows still exist)
    * @param ignoreErrors True to log error messages at a WARN level, false to print entire stack traces at the ERROR level.
    */
   public void tearDownProcess(IDfSession session, boolean cleanWorkflows, boolean ignoreErrors) {
      log.info("Tearing down Process Utility test harness, ignoring errors? {}", ignoreErrors);
      ProcessDestroyer procDestroyer = new ProcessDestroyer(session);
      SDTUtility sdtUtility = new SDTUtility(session);
      FolderUtils folderUtils = new FolderUtils(session);
      
      //destroy the process template and associated workflows
      try {
         if(this.processId != null) {
            IDfProcessEx process = (IDfProcessEx)session.getObject(this.processId);
            try {
               procDestroyer.destroyWorkflowInstances(process);
            }
            catch(Exception e) {
               if(!ignoreErrors) {
                  log.error("Unable to destroy workflow isntances for Process Template due to inner error", e);
               } else {
                  log.warn("Unable to destroy workflow instances for Process Template");
               }
            }
            //retrieve the folder and destroy the process
            if(this.folderId != null) {
               IDfFolder folder = (IDfFolder)session.getObject(this.folderId);
               procDestroyer.destroyProcessTemplate(process, folder, true);
            }
         }
      }
      catch(Exception e) {
         if(!ignoreErrors) {
            log.error("Unable to teardown Process Template due to inner error", e);
         } else {
            log.warn("Unable to teardown Process Template");
         }
      }
      
      //destroy the folder
      try {
         if(this.folderId != null) {
            IDfFolder folder = (IDfFolder)session.getObject(this.folderId);
            folderUtils.destroyFolder(folder, true);
         }
      }
      catch(Exception e) {
         if(!ignoreErrors) {
            log.error("Unable to delete folder due to inner error", e);
         } else {
            log.warn("Unable to delete Folder");
         }
      }
      
      try {
         if(this.sdtName != null) {
            sdtUtility.deleteSDT(sdtName);
         }
      }
      catch(Exception e) {
         if(!ignoreErrors) {
            log.error("Unable to teardown SDT due to inner error", e);
         } else {
            log.warn("Unable to teardown SDT");
         }
      }
      log.info("Teardown complete");
   }

   /**
    * @return the process
    */
   public IDfId getProcessId() {
      return this.processId;
   }

   /**
    * @return the processName
    */
   public String getProcessName() {
      return this.processName;
   }

   /**
    * @return the sdtName
    */
   public String getSdtName() {
      return this.sdtName;
   }

   /**
    * @return the packageName
    */
   public String getPackageName() {
      return this.packageName;
   }

   /**
    * @return the folderId
    */
   public IDfId getFolderId() {
      return this.folderId;
   }

   /**
    * @return the queueName
    */
   public String getQueueName() {
      return this.queueName;
   }
   
}
