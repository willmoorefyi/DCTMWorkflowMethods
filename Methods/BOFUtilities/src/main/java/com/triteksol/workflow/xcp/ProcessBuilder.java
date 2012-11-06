/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 6, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfActivityEx;
import com.documentum.bpm.IDfProcessEx;
import com.documentum.bpm.sdt.DfStructuredDataTypeDAOFactory;
import com.documentum.bpm.sdt.DfStructuredDataTypeFactory;
import com.documentum.bpm.sdt.IDfPrimitiveType;
import com.documentum.bpm.sdt.IDfStructuredDataType;
import com.documentum.bpm.sdt.IDfStructuredDataTypeDAO;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.triteksol.content.xcp.FolderUtils;

/**
 * Utility class to build process templates. Useful for testing.
 * 
 * Builds a single process template with all activities and process installed in a single folder.  Activities are connected sequentially.
 * 
 * @author wmoore
 *
 */
public class ProcessBuilder {
   private static final Logger log = LoggerFactory.getLogger(ProcessBuilder.class);
   
   //default workflow properties, for consistency
   public static final String WORKFLOW_PROPERTY_FILE_NAME = "/dctm-workflow.properties";
   public static final String PROP_PROCESSNAME = "com.triteksol.xcp.processname";
   public static final String PROP_PROCESS_FOLDERNAME = "com.triteksol.xcp.foldername";
   public static final String PROP_PROCESS_FOLDERLOCATION = "com.triteksol.xcp.folderlocation";
   
   static final String PACKAGE_TYPE = "dm_document";
   static final IDfId PACKAGE_ID = DfId.DF_NULLID;
   static final String PACKAGE_LABEL = "CURRENT";

   Properties defaultProps;
   IDfSession session;
   String processName;
   IDfProcessEx process;
   List<IDfActivityEx> activities;
   Map<IDfId, String> inputActivityPortMapping;
   Map<IDfId, String> outputActivityPortMapping;
   List<String> packageNames;
   IDfFolder folder;
   
   /**
    * PRocessBuilder constructor.  Invoke using {@link ProcessBuilder#startBuilding(IDfSession)}
    * @param session
    */
   private ProcessBuilder(IDfSession session) {
      log.info("Creating new process builder");
      InputStream is = getClass().getResourceAsStream(ProcessBuilder.WORKFLOW_PROPERTY_FILE_NAME);
      defaultProps = new Properties();
      try {
         defaultProps.load(is);
      } catch (IOException e) {
         log.warn("Unable to load default properties.  May not be an issue if all parameters are defined by client.  Message: " + e.getMessage());
      }
      this.session = session;
      activities = new ArrayList<IDfActivityEx>();
      packageNames = new ArrayList<String>();
      inputActivityPortMapping = new HashMap<IDfId, String>();
      outputActivityPortMapping = new HashMap<IDfId, String>();
      folder = null;
   }
   
   /**
    * Start building a new process
    * @param session
    * @return
    */
   public static ProcessBuilder startBuilding(IDfSession session) {
      return new ProcessBuilder(session);
   }
   
   /**
    * Set the Process name
    * @param processName The process name to set
    * @return
    */
   public ProcessBuilder setProcessName(String processName) {
      if(processName == null) { throw new RuntimeException("Process Name must have a valid value and must be called first"); }
      log.debug("Setting process name '{}'", processName);
      this.processName = processName;
      return this;
   }
   
   /**
    * Set the default process name, if it has not already been set
    */
   private void setDefaultProcessName() {
      log.debug("Setting default process name");
      String processName = defaultProps.getProperty(PROP_PROCESSNAME);
      setProcessName(processName);
   }
   
   /**
    * Create a folder for the process and activities
    * @param folder The folder to use to contain all the process configurations
    * @return
    * @throws DfException
    */
   public ProcessBuilder setFolder(IDfFolder folder) throws DfException {
      if(this.folder != null) { throw new RuntimeException("Can not call setFolder twice, or call setFolder after another ProcessBuilder method besides SetMethodName"); }
      if(folder == null) { throw new RuntimeException("Cannot call setFolder with a null folder location"); }
      
      if(this.processName == null) {
         this.setDefaultProcessName();
      }
      log.debug("Using folder with ID '{}' and path '{}'", folder.getObjectId(), folder.getFolderPath(0));
      this.folder = folder;
      return this;
   }
   
   /**
    * Set the default folder, if it has not already been set
    * @throws DfException
    */
   private void setDefaultFolder() throws DfException {
      log.debug("Setting default folder name");
      String folderLocation = defaultProps.getProperty(PROP_PROCESS_FOLDERLOCATION);
      String folderName = defaultProps.getProperty(PROP_PROCESS_FOLDERNAME);
      
      /*
      IDfFolder defaultFolder = (IDfFolder)session.getObjectByQualification(String.format("dm_folder WHERE object_name = '%1$s' AND FOLDER('%2$s')", folderName, folderLocation));
      log.debug("Creating folder: '{}/{}'", folderLocation, folderName);
      if(folder == null) {
         folder = (IDfFolder)this.session.newObject("dm_folder");
         folder.setObjectName(folderName);
         folder.link(folderLocation);
         folder.save();
         folder.fetch(null);
         log.debug("Created folder with ID '{}' and path '{}'", folder.getObjectId(), folder.getFolderPath(0));
      }
      else {
         log.debug("existing folder found, returning");
      }
      */
      FolderUtils folderUtils = new FolderUtils(session);
      IDfFolder defaultFolder = folderUtils.createFolderUnsafe(folderLocation, folderName);
      setFolder(defaultFolder);
   }
   
   public ProcessBuilder addPackage(String pacakgeName) throws DfException {
      if(!activities.isEmpty()) { throw new RuntimeException("Cannot add a package after activities have been added"); }
      if(folder == null) {
         this.setDefaultFolder();
      }
      log.debug("Adding package '{}' to process configuration");
      this.packageNames.add(pacakgeName);
      return this;
   }
   

   /**
    * Create a normal activity.  Most useful for initiate steps, where there can be no processor. A full process must have at least 4 activities (initiate, begin, step, end)
    * @param session The Documentum Session
    * @param activityName The name of the activity
    * @param folder The folder to file the Activity into
    * @return The new activity
    * @throws DfException
    */
   public ProcessBuilder addSimpleActivity(String activityName) throws DfException {
      if(folder == null) {
         this.setDefaultFolder();
      }
      log.debug("Creating activity {}", activityName);
      IDfActivityEx activity = createActivityHelper(activityName);
      activity.save();
      activity.fetch(null);
      log.debug("Successfully created activity {}", activityName);
      
      activities.add(activity);
      
      return this;
   }

   /**
    * Create a queue-processing activity. A full process must have at least 4 activities (initiate, begin, step, end)
    * @param session The Documentum Session
    * @param activityName The name of the activity
    * @param folder The folder to file the Activity into
    * @param queueName The name of the queue that work will be placed into
    * @return The new activity
    * @throws DfException
    */
   public ProcessBuilder addQueueActivity(String activityName, String queueName) throws DfException {
      if(folder == null) {
         this.setDefaultFolder();
      }
      log.debug("Creating activity {}", activityName);
      IDfActivityEx activity = createActivityHelper(activityName);
      activity.setPerformerFlag(1);
      activity.setPerformerType(10);
      activity.setPerformerName(queueName);
      activity.save();
      activity.fetch(null);
      log.debug("Successfully created activity {}", activityName);
      
      activities.add(activity);
      
      return this;
   }
   
   /**
    * Helper to create an activity
    * @param activityName
    * @return
    * @throws DfException
    */
   private IDfActivityEx createActivityHelper(String activityName) throws DfException {
      //if this is the first activity, it is the initiate activity
      IDfActivityEx activity = (IDfActivityEx)session.newObject("dm_activity");
      activity.setObjectName(activityName);
      activity.link(folder.getFolderPath(0));
      activity.save();
      activity.fetch(null);
      
      if(activities.isEmpty()) {
         //this is the first activity
         activity.setTriggerThreshold(0);
      }
      else {
         //this is not the first activity
         activity.setTriggerThreshold(1);
         log.debug("Setting input port on activity");
         String inputPortName = activityName + ":Input"; 
         activity.addPort(inputPortName, IDfActivityEx.PORT_TYPE_INPUT);
         for(String packageName : packageNames) {
            activity.addPackageInfoEx(inputPortName, packageName, PACKAGE_TYPE, PACKAGE_ID, PACKAGE_LABEL, null, 1);
         }
         inputActivityPortMapping.put(activity.getObjectId(), inputPortName);
         
         log.debug("Setting output port and package settings on previous activity");
         IDfActivityEx prevActivity = activities.get(activities.size()-1);
         String outputPortName = prevActivity.getObjectName() + ":Output";
         prevActivity.addPort(outputPortName, IDfActivityEx.PORT_TYPE_OUTPUT);
         for(String packageName : packageNames) {
            prevActivity.addPackageInfoEx(outputPortName, packageName, PACKAGE_TYPE, PACKAGE_ID, PACKAGE_LABEL, null, 1);
         }
         prevActivity.save();
         outputActivityPortMapping.put(prevActivity.getObjectId(), outputPortName);
      }
      
      return activity;
   }
   
   /**
    * Start building the process.  Call when all activities have been created and you want to start setting process-level parameters
    * @return
    * @throws DfException
    */
   public ProcessBuilder startProcess() throws DfException {
      if(activities.size() < 4) { throw new RuntimeException("A process must have 4 activities: initiate, begin, step, end"); }
      log.debug("Finished building activities, building process");
      this.process = (IDfProcessEx)session.newObject("dm_process");
      this.process.setObjectName(processName);
      this.process.link(folder.getFolderPath(0));
      this.process.save();
      this.process.fetch(null);
      log.debug("Created proces template with ID '{}'", this.process.getObjectId().toString());
      return this;
   }
   
   /**
    * Setup a simple SDT.  Will have <em>n</em> attributes, and each attribute will be a string with name attr<em>n</em>.
    * @param session The Documentum Session
    * @param sdtName The name of the SDT
    * @param attrCount The number of attributes
    * @return A new SDT that has been installed and is ready to use
    * @throws Exception
    */
   public IDfStructuredDataType setupSimpleSDT(IDfSession session, String sdtName, int attrCount) throws Exception {
      log.debug("Creating the structured data type");
      DfStructuredDataTypeFactory factory = DfStructuredDataTypeFactory.getInstance();
      IDfStructuredDataType sdt = factory.createStructuredDataType(sdtName, sdtName, "No Description");
      
      for(int i = 0; i < attrCount; i++) {
         sdt.addAttribute("attr"+i, "attr"+i, "No Description", IDfPrimitiveType.STRING, "", false, false, false);
      }
      log.debug("Completed setup of SDT, committing to repository");
      IDfStructuredDataTypeDAO sdtDAO = DfStructuredDataTypeDAOFactory.getDocbaseDAO(session.getSessionManager(), session.getDocbaseName());
      sdtDAO.createNewStructuredDataType(sdt);
      log.debug("Created SDT");
      return sdt;
   }
   
   /**
    * Add a simple variable type to the process
    * @param variableName The variable name
    * @param type The simple variable type
    * @return
    * @throws DfException
    */
   public ProcessBuilder addSimpleProcessVariable(String variableName, IDfPrimitiveType type) throws DfException {
      if(this.process == null) { throw new RuntimeException("Cannot add process variables until the process has started. Call StartProcess first"); }
      if(type == null) { throw new RuntimeException("Cannot call addSimpleProcessVariable with an invalid primitive type"); }
      log.debug("Adding variable '{}' of primitive type '{}", variableName, type);
      this.process.addVariable(variableName, type);
      return this;
   }
   
   /**
    * Add a simple variable type to the process
    * @param variableName The variable name
    * @param type The simple variable type
    * @return
    * @throws DfException
    */
   public ProcessBuilder addSDTVariable(String variableName, IDfStructuredDataType sdt) throws DfException {
      if(this.process == null) { throw new RuntimeException("Cannot add process variables until the process has started. Call StartProcess first"); }
      if(sdt == null) { throw new RuntimeException("Cannot call addSDTVariable with a null SDT"); }
      log.debug("Adding variable '{}' of SDT type '{}", variableName, sdt.getName());
      this.process.addVariable(sdt.getName(), sdt);
      return this;
   }
   
   /**
    * Finalize the process 
    * @return
    */
   public IDfProcessEx buildProcess() throws DfException {
      if(this.activities.size() < 4) { throw new RuntimeException("A process must have 4 activities: initiate, begin, step, end"); }
      if(this.process == null) { throw new RuntimeException("Cannot finalize a process until the process build has started"); }
      
      log.debug("Finalizing process.  Setting up activity links");
      for(int i=0; i<this.activities.size(); i++) {
         IDfActivityEx activity = this.activities.get(i);
         String activityType;
         if(i==0) {
            activityType = "initiate";
         }
         else if(i==1) {
            activityType = "begin";
         }
         else if(i==this.activities.size()-1) {
            activityType = "end";
         }
         else {
            activityType = "step";
         }
         this.process.addActivity(activity.getObjectName(), activity.getObjectId(), activityType, 1);
         if(i !=0) {
            log.debug("Creating link for current activity and previous activity");
            IDfActivityEx prevActivity = this.activities.get(i-1);
            this.process.addLink("link"+i, prevActivity.getObjectName(), outputActivityPortMapping.get(prevActivity.getObjectId()), 
                  activity.getObjectName(), inputActivityPortMapping.get(activity.getObjectId()));
         }
      }
      
      this.process.save();
      this.process.fetch(null);
      log.debug("Process links created.  Saving and validating activities");
      for(IDfActivityEx activity : this.activities) {
         activity.validate();
      }
      for(IDfActivityEx activity : this.activities) {
         activity.install();
      }
      
      log.debug("Activities validated and installed.  Moving to process");
      this.process.validate();
      this.process.install(false, false);
      
      log.debug("Process created successfully");
      return this.process;
   }
}
