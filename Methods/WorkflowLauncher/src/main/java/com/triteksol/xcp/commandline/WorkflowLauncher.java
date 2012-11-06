/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jul 1, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.exolab.castor.types.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfActivityEx;
import com.documentum.bpm.IDfProcessEx;
import com.documentum.bpm.IDfProcessVariableMetaData;
import com.documentum.bpm.IDfWorkflowEx;
import com.documentum.bpm.ProcessVariableNotFoundException;
import com.documentum.bpm.sdt.SDTAttributeNotFoundException;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfWorkflowBuilder;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfList;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.triteksol.xcp.commandline.base.DocumentumCommandLineBase;

/**
 * @author wmoore
 *
 */
public class WorkflowLauncher extends DocumentumCommandLineBase {
   private static final Logger log = LoggerFactory.getLogger(WorkflowLauncher.class);
   
   public static final String OPT_WF_NAME = "workflow";
   public static final String OPT_WF_NAME_SHORT = "w";
   public static final String OPT_WF_INPUT_FILE = "file";
   public static final String OPT_WF_INPUT_FILE_SHORT = "f";
   
   public static final DateTimeFormatter DATETIME_FORMAT = ISODateTimeFormat.dateHourMinuteSecond();
   
   /**
    * The main method - execute the program
    * @param args
    */
   public static void main(String[] args) {
      log.info("Starting the workflow launcher utility with arguments {}", new Object[] { args });
      try {
         WorkflowLauncher runner = new WorkflowLauncher();
         System.exit(runner.run(args) ? 1 : 0);
      } catch(RuntimeException ex) {
         log.error("A runtime exception occurred during the program's execution", ex);
         System.err.println("The application failed to execute successfully and returned the following exception: " + ex.getMessage());
         System.exit(1);
      }
   }

   /* (non-Javadoc)
    * @see com.triteksol.xcp.commandline.base.DocumentumCommandLineBase#addOptions(org.apache.commons.cli.Options)
    */
   @Override
   @SuppressWarnings("static-access")
   public Options addOptions(Options opts) {
      Option workflow = OptionBuilder.withArgName(OPT_WF_NAME).hasArgs()
            .withDescription("The workflow to create in the system").withLongOpt(OPT_WF_NAME)
            .isRequired().create(OPT_WF_NAME_SHORT);

      Option inputfile = OptionBuilder.withArgName(OPT_WF_INPUT_FILE).hasArg()
            .withDescription("The role to create in the system").withLongOpt(OPT_WF_INPUT_FILE)
            .isRequired().create(OPT_WF_INPUT_FILE_SHORT);
      
      opts.addOption(workflow);
      opts.addOption(inputfile);
      
      return opts;
   }

   /* (non-Javadoc)
    * @see com.triteksol.xcp.commandline.base.DocumentumCommandLineBase#doWork(com.documentum.fc.client.IDfSession, org.apache.commons.cli.CommandLine)
    */
   @Override
   public boolean doWork(IDfSession session, CommandLine line) {
      log.info("Executing Workflow launcher utility");
      String workflow = line.getOptionValue(OPT_WF_NAME);
      String filename = line.getOptionValue(OPT_WF_INPUT_FILE);
      
      log.debug("Beginning workflow launcher for command line parameters: workflow '{}' input file '{}'", workflow, filename);
      
      File inputFile = new File(filename);
      if(!inputFile.exists()) {
         URL inputFileUrl = getClass().getResource(filename);
         if(inputFileUrl != null) {
            filename = inputFileUrl.getFile();
         }
         else {
            log.error("Input file '{}' could not be found using either an absolute or relative path locator", filename);
            throw new RuntimeException(String.format("Input file '%s' could not be found using either an absolute or relative path locator", filename));
         }
      }
      
      launchWorkflows(session, workflow, filename);
      
      return true;
   }
   
   /**
    * The method that launches workflows based on the properties in an input file and the workflow name
    * @param session
    * @param workflowName
    * @param inputFile
    */
   public static final void launchWorkflows(IDfSession session, String workflowName, String inputFile) {
      log.info("Entering workflow launcher");
      List<String> launchedWorkflowIds = null;
      List<Map<String, Object>> data = DataParser.readData(inputFile);
      if (data == null) {
         return ;
      }
      
      
      launchedWorkflowIds = launchWorkflows(session, workflowName, data);

      log.info("Successfully launched {} workflows without exception", launchedWorkflowIds.size());
      
      return;
   }

   static final IDfWorkflowBuilder getWorkflowBuilder(IDfSession session, String workflowName) {
      log.debug("Retrieving workflow builder for process '{}'", workflowName);
      try {
         IDfId processId = session.getIdByQualification(String.format("dm_process where object_name = '%s' and r_definition_state = 2", workflowName));
         IDfWorkflowBuilder builder = session.newWorkflowBuilder(processId);
         log.info("Process '{}' version '{}' being used to launch work.", workflowName, builder.getProcess().getVersionLabels().getImplicitVersionLabel());
         int startStatus = builder.getStartStatus();
         switch(startStatus) {
            case IDfWorkflowBuilder.DF_WB_CAN_START:
               log.debug("Selected process can be started.");
               break;
            default:
               log.info("Selected process '{}' can't be started.", workflowName);
               throw new RuntimeException(String.format("The selected process '%s' can't be used to start workflow as it is not in a startable state", workflowName));
         }
         log.debug("Successfully created workflow builder");
         return builder;
      } catch (DfException e) {
         log.error("Error occurred retrieving workflow builder for workflow '{}'", workflowName);
         throw new RuntimeException(String.format("Error occurred retrieving workflow builder for workflow '%s'", workflowName), e);
      }
   }
   
   /**
    * Launch workflows
    * @param session
    * @param builder
    * @param data
    * @return
    */
   static final List<String> launchWorkflows(IDfSession session, String workflowName, List<Map<String, Object>> data) {
      log.debug("launching workflows");
      List<String> results = new ArrayList<String>();
      try {
         for(Map<String, Object> dataFields : data) {
            log.debug("Launching workflow...");
            IDfWorkflowBuilder builder = getWorkflowBuilder(session, workflowName);
            IDfProcessEx process = (IDfProcessEx)builder.getProcess();
            IDfId workflowId = builder.initWorkflow();
            IDfWorkflowEx workflow = (IDfWorkflowEx) builder.getWorkflow();
            IDfActivityEx startActivity = (IDfActivityEx)session.getObject((IDfId)builder.getStartActivityIds().get(0));
            List<String> excludedProperties = new ArrayList<String>();
            // set properties
            for(String propertyName : dataFields.keySet()) {
               if(!setWFValue(process, workflow, propertyName, dataFields.get(propertyName))) {
                  //add this to the excluded properties for later
                  excludedProperties.add(propertyName);
               }
            }
            
            log.debug("Properties set, starting workflow");
            builder.runWorkflow();
            log.debug("Workflow successfully started, adding packages");
            processPackageProperties(session, builder, startActivity, excludedProperties, dataFields);
            log.debug("Packages successfully processed");
            results.add(workflowId.getId());
         }
         
      }
      catch(DfException e) {
         if(results.isEmpty()) {
            //if the data set is empty, we launched zero workflows, so just throw the exception
            throw new RuntimeException("Error occurred trying to launch workflows for input process and data set", e);
         }
         else {
            //otherwise, log the error and return the data set
            log.error("Error occurred while trying to launch workflows for input process and data set", e);
         }
      }
      return results;
   }
   
   /**
    * 
    * @param session
    * @param builder
    * @param startActivity
    * @param propertyNames
    * @param properties
    * @return
    */
   static final void processPackageProperties(IDfSession session, IDfWorkflowBuilder builder, IDfActivityEx startActivity, List<String> propertyNames, Map<String, Object> properties) {
      log.debug("Parsing packages for workflow out of input properties");
      try {
         int portCount = startActivity.getPortCount();
         for(int i=0; i<portCount; i++) {
            if(startActivity.getPortType(i).equalsIgnoreCase("INPUT") ) {
               String inputPortName = startActivity.getPackageName(0);
               if(propertyNames.contains(inputPortName)) {
                  log.debug("Package '{}' was in the excluded property list", inputPortName);
                  String packageValue = (String)getFormattedProperty(IDfAttr.DM_ID, properties.get(inputPortName));
                  if(packageValue == null || "".equals(packageValue)) {
                     log.debug("Package '{}' not present in property file", inputPortName);
                  }
                  else {
                     log.debug("Found package for property '{}', adding to workflow", inputPortName);
                     
                     IDfDocument packageObject = (IDfDocument)session.getObject(new DfId(packageValue));
                     DfList list = new DfList();
                     list.append(packageObject.getObjectId());
                     
                     log.debug("Adding package to workflow");
                     //assume the package type is the same
                     builder.addPackage(startActivity.getObjectName(), startActivity.getPortName(i), inputPortName, startActivity.getPackageType(i), 
                           "", false, list);
                     log.debug("Package successfully added to workflow");
                  }
               }
            }
         }
      }
      catch(DfException e) {
         log.error("Error occurred while parsing package data");
         throw new RuntimeException("Error occurred while parsing package data", e);
      }
      
   }
   
   /**
    * Set the value of the workflow property, either attribute or package value
    * @param builder
    * @param process
    * @param startActivity
    * @param workflow
    * @param propertyName
    * @param propertyValue
    * @return true if the property was processed successfully, false otherwise
    */
   static boolean setWFValue(IDfProcessEx process,  IDfWorkflowEx workflow, String propertyName, Object propertyValue) {
      log.debug("Setting property '{}' to value '{}'", propertyName, propertyValue);
      try {
         if (propertyValue == null || "".equals(propertyValue)) {
            log.debug("Not setting field {} value because the value is null.", propertyName);
            return true;
         }
         
         int dataType = -1;
         String[] propertySplitName = StringUtils.split(propertyName, "|"); 
         String variableRoot = propertySplitName[0];
         log.debug("Variable Root: '{}'", variableRoot);
         try {
            IDfProcessVariableMetaData metaData = process.getVariableMetaData(variableRoot);
            if(metaData.isPrimitiveType()) {
               log.debug("Property '{}' is a primitive property", propertyName);
               dataType = metaData.getPrimitiveType().getId();
               log.debug("Data type for property: {}", dataType);
               Object propertyFinalValue = getFormattedProperty(dataType, propertyValue);
               log.debug("Found property value '{}'", propertyFinalValue);
               workflow.setPrimitiveObjectValue(variableRoot, propertyFinalValue);
            }
            else {
               log.debug("Property '{}' is a structured data type property", propertyName);
               String attributeName = propertySplitName[1] == null ? null : propertySplitName[1];
               log.debug("Using property attribute name '{}'", attributeName);
               dataType = metaData.getStructuredDataType().getAttribute(attributeName).getType().getId();
               log.debug("Data type for property: {}", dataType);
               Object propertyFinalValue = getFormattedProperty(dataType, propertyValue);
               log.debug("Found property value '{}'", propertyFinalValue);
               if(propertyFinalValue.getClass().isArray()) {
                  workflow.setStructuredDataTypeAttrValues(variableRoot, attributeName, (Object[])propertyFinalValue);
               } else {
                  workflow.setStructuredDataTypeAttrValue(variableRoot, attributeName, propertyFinalValue);
               }
            }
            log.debug("Successfully set property value");
         }
         catch (ProcessVariableNotFoundException e) {
            log.debug("No process variable for root '{}', will check packages later", variableRoot);
            return false;
         }
         catch(SDTAttributeNotFoundException e) {
            log.error("User specified an SDT property, but the SDT attribute value is not valid");
            throw new RuntimeException("User specified an SDT property, but the SDT attribute value is not valid", e);
         }
      }
      catch(DfException e) {
         log.error("DFC Exception occurred while trying to set process property '{}' to value '{}'", propertyName, propertyValue);
         throw new RuntimeException(String.format("DFC Exception occurred while trying to set process property '%s' to value '%s'", propertyName, propertyValue), e);
      }
      return true;
   }
   
   /**
    * Format the input property from a string into the appropriate type
    * @param dataType   The input data type
    * @param propertyValue The input data value
    * @return  The formatted property value
    */
   static final Object getFormattedProperty(int dataType, Object propertyValue) {
      if(propertyValue == null) {
         log.debug("Input property is null");
         return null;
      }
      if(propertyValue.getClass().isArray())  {
         throw new UnsupportedOperationException("Array values not currently supported");
      }
      else {
         switch (dataType) {
         case IDfAttr.DM_ID: 
            return StringUtils.defaultIfEmpty(propertyValue.toString(), "");
         case IDfAttr.DM_STRING:
            return StringUtils.defaultIfEmpty(propertyValue.toString(), " ");
         case IDfAttr.DM_TIME:
            Date date = DATETIME_FORMAT.parseDateTime(propertyValue.toString()).toDate();
            log.debug("Parsed date into datetime: {}", date);
            return new DateTime(date).toString();
         default:
            throw new RuntimeException(String.format("Property type %s is an unsupported type", dataType));
         }
      }
   }
}
