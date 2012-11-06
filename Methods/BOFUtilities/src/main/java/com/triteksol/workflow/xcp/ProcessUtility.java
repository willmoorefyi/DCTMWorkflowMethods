/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 6, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfActivityEx;
import com.documentum.bpm.IDfProcessEx;
import com.documentum.bpm.utils.ProcessDataUtils;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfWorkflow;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfList;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfList;

/**
 * Utility class to work with process objects
 * 
 * @author TriTek Solutions, Inc.
 *
 */
public class ProcessUtility {
   private static Logger log = LoggerFactory.getLogger(ProcessUtility.class);
   
   static final String QUERY_RESET_SHARING = "EXECUTE exec_sql with query='UPDATE dmc_wfsd_element_parent_s SET i_orig_parent = ''0000000000000000'', i_sharing_type = ''0000000000000000'' WHERE r_object_id = ''%s'''";
   
   private IDfSession session;
   
   public ProcessUtility(IDfSession session) {
      this.session = session;
   }
   
   /**
    * Retrieve the specified named process template from the repository.  This will retrieve the current version of the process template
    * @param processName   The name of the process template to retrieve
    * @param session       The Documentum session
    * @return The Documentum process template object, or <em>null</em> if the named template doesn't exist.
    */
   public IDfProcessEx getProcess(String processName) {
      assert processName != null : "A valid process name is required when calling getProcess";
      log.debug("Retrieving process {}", processName);
      try {
         return (IDfProcessEx) this.session.getObjectByQualification(String.format("dm_process where object_name = '%s'", processName));
      } catch (DfException e) {
         throw new RuntimeException("A server error occurred while retreiving the process template " + processName, e);
      }
   }
   
   /**
    * Terminate the active workflow instances of the passed-in process template.
    * @param proc    The process to terminate
    */
   public void terminateChildWorkflows(IDfProcessEx proc) {
      assert proc != null : "Process template to terminate child workflows is required!";
      try {
         log.debug("Terminate child workflows of parent workflow process");
         if(proc.hasAbortedWorkflow()) {
            log.debug("Process template has aborted workflows, terminating");
            proc.destroyAbortedWorkflows();
         }
         IDfId[] workflows = ProcessDataUtils.getWorkflowInstances(proc.getObjectId(), this.session, null);
         for (IDfId id : workflows){
            if ((id != null) && (!id.isNull())) {
              IDfWorkflow workflow = (IDfWorkflow) session.getObject(id);
              log.debug("Aborting and destroying workflow {}", workflow.getObjectId());
              switch(workflow.getRuntimeState()) {
              case IDfWorkflow.DF_WF_STATE_RUNNING :
                 //abort the workflow if it is running
                 workflow.abort();
              case IDfWorkflow.DF_WF_STATE_DORMANT :
              case IDfWorkflow.DF_WF_STATE_FINISHED :
              case IDfWorkflow.DF_WF_STATE_HALTED :
              case IDfWorkflow.DF_WF_STATE_TERMINATED :
                 //destroy the workflow
                 workflow.destroy();
                 break;
              case IDfWorkflow.DF_WF_STATE_UNKNOWN :
              default :
                 log.debug("Workflow is in state {}", workflow.getRuntimeState());
                 throw new RuntimeException("Workflow '" + workflow.getObjectId() + "' is in an unknown state and cannot be destroyed");
              }
            }
         }
      }
      catch(DfException e) {
         throw new RuntimeException("An error occurred cleaning up the process template", e);
      }
   }
   
   /**
    * Terminate the audit trail entries for the given workflow
    * @param process The process to cleanup audit entries
    */
   public boolean terminateAuditTrailEntries(IDfProcessEx proc) {
      assert proc != null : "Process template to cleanup audit trail entries is required!";
      assert session != null : "A valid Documentum session is required!";
      
      IDfCollection col = null;
      try {
         log.debug("Terminating workflow audit trail for process \"{}\" with name \"{}\"", proc.getObjectId(), proc.getObjectName());
         //construct the parameters for the API method PURGE_AUDIT
         IDfList args = new DfList(), dataTypes = new DfList(), values = new DfList();
         args.appendString("DELETE_MODE");         dataTypes.appendString("S");     values.appendString("SINGLE_VERSION");
         args.appendString("OBJECT_ID");           dataTypes.appendString("S");     values.appendString(proc.getObjectId().toString());
         args.appendString("PURGE_NON_ARCHIVED");  dataTypes.appendString("B");     values.appendString("T");
         args.appendString("COMMIT_SIZE");         dataTypes.appendString("I");     values.appendString("0");
         //execute the purge audit legacy API call, as no matching call exists in DFC
         col = session.apply(null, "PURGE_AUDIT", args, dataTypes, values);
         log.debug("purge executed successfully");
         //we should only have 1 result
         col.next();
         log.debug("Query executed successfully? \"{}\" Objects Destroyed: {}", col.getString("result"), col.getString("deleted_objects"));
         return col.getString("result").equals("T");
      } catch (DfException e) {
         throw new RuntimeException("Error occured while attempting to terminate the audit trail of a given workflow", e);
      } finally {
         if(col != null) {
            try {
               col.close();
            } catch(DfException e) {
               log.error("Error occurred while closing the DFC collection returned from the query", e);
            }
         }
      }
   }

   /**
    * Retrieve process parameter objects associated with this process template, to destroy after the template has been removed.
    * Note that this method only retrieves process parameters that are stored in the same folder as the process.  While this is not a hard and fast requirement, it is common practice to do so.
    * @param proc    The process template
    * @return a collection of IDfPersistentObject objects, representing the parameters for the process template
    */
   public List<String> retrieveProcessParameterNames(IDfProcessEx proc) {
      assert proc != null : "Process template to retrieve process parameter entries is required!";
      
      try {
         log.debug("Retrieving process parameters associated with template '{}'", proc.getObjectName());
         List<String> paramNames = Arrays.asList(proc.getParameterNames());
         return paramNames;
      } catch (DfException ex) {
         throw new RuntimeException("Error occurred while attempting to retrieve process parameters for input process", ex);
      }
   }
   
   /**
    * Retrieve the process variable parents, to clean up process variable parent objects created when a process is installed.
    * @param proc
    * @return
    */
   public void resetProcessVariableParents(IDfProcessEx proc) {
      assert proc != null : "Process template to retrieve process variable parent objects is required";

      try {
         log.debug("Retrieving process variable parent objects, to clean up lightweight system objects, associated with template {}", proc.getObjectName());
         //List<IDfId> variableParentIds = new ArrayList<IDfId>();
         
         for(String variable : proc.getVariableNames()) {
            log.debug("processing variable {}", variable);
            IDfId parentId = proc.getParentObjectId(variable);
            
            IDfQuery query = new DfQuery();
            
            String dql = String.format(QUERY_RESET_SHARING, parentId);
            log.debug("Executing query \"{}\"", dql);
            query.setDQL(dql);
            query.execute(this.session, IDfQuery.DF_EXEC_QUERY);
            
            log.debug("processed variable {}", variable);
         }
         
      } catch (DfException ex) {
         throw new RuntimeException("Error occurred while attempting to retrieve parent variable IDs associated with input process", ex);
      }
   }
   
   /**
    * Destroy the process parameters for the process template
    * @param params The process parameter names, retrieved earlier.
    * @param folder the folder where the process parameter lives
    */
   public void destroyProcessParameters(List<String> paramNames, IDfFolder folder) {
      assert paramNames != null : "The list of param names for the process may not be null";
      assert folder != null : "The containing folder for a process may not be null!";
      
      try {
         String folderId = folder.getObjectId().toString();
         for(String paramName : paramNames) {
            IDfPersistentObject param = session.getObjectByQualification(String.format("dmc_process_parameter where object_name='%1$s' and any i_folder_id='%2$s'", paramName, folderId)); 
            if(param != null) {
               log.debug("Destroying parameter named '{}'", paramName);
               param.destroy();
            } else {
               log.debug("Param named '{}' not found, ignoring", paramName);
            }
         }
      } catch(DfException ex) {
         throw new RuntimeException("Failed destroying process parameters for associated process template", ex);
      }
   }
   
   /**
    * Retrieve all the activities for the given process
    * @param proc    The process to destroy
    * @return The list of all activities for the given process
    */
   public List<IDfActivityEx> retrieveAttachedActivities(IDfProcessEx proc) {
      assert proc != null : "Process template to retrieve activities is required!";
      assert session != null : "A valid Documentum session is required!";
      
      List<IDfActivityEx> activities = new ArrayList<IDfActivityEx>(); 
      try {
         for(int i = 0; i< proc.getActivityCount(); i++) {
            activities.add((IDfActivityEx)this.session.getObject(proc.getActivityDefId(i)));
         }
      } catch (DfException e) {
         throw new RuntimeException("Error occurred retrieving activities for process", e);
      }
      return activities;
   }
   
   /**
    * Cleanup the listed activities
    * @param activities The activities to uninstall and destroy
    */
   public void destroyActivities(List<IDfActivityEx> activities) {
      assert activities != null : "A list of activities to destroy is required!";
      
      try {
         for(IDfActivityEx activity : activities) {
            String activityName = activity.getObjectName();
            log.debug("Found activity '{}' with name '{}', attempting to uninstall and destroy", activity.getObjectId(), activityName);
            String actState = activity.getDefinitionState();
            //I shit you not this method returns a string that contains an int representation of the state.  Oh this is also completely undocumented.
            if(actState.equals("2")) {
               log.debug("Uninstalling activity '{}'", activityName);
               activity.uninstall();
            }
            if(actState.equals("2") || actState.equals("1") || actState.equals("0")) {
               log.debug("Destroying activity '{}'", activityName);
               activity.destroy();
            } else {
               throw new RuntimeException("Acitivity '" + activityName + "' is in an unknown state: " + actState);
            }
         }
      }
      catch(DfException e) {
         throw new RuntimeException("Error occurred while attempting to uninstall and destroy an activity", e);
      }
   }
   
   /**
    * Uninstall and destroy the passed-in process
    * @param proc    The process to destroy
    */
   public void destroyProcess(IDfProcessEx proc) {
      assert proc != null : "Process template to destroy is required!";
      
      try {
         log.debug("Uninstalling and destroying process template \"{}\" with name \"{}\"", proc.getObjectId(), proc.getObjectName());
         switch(proc.getDefinitionState()) {
         case 2:
            proc.uninstall();
            log.debug("Process successfully uninstalled");
         case 1:
         case 0:
            proc.destroy();
            log.debug("Process destroyed");
            break;
         default:
              throw new RuntimeException("Unexpected process state discovered for process template " + proc.getObjectName());
         }
      }
      catch(DfException e) {
         throw new RuntimeException("Error occurred while attempting to uninstall and destroy the process template", e);
      }
      return;
   }

}
