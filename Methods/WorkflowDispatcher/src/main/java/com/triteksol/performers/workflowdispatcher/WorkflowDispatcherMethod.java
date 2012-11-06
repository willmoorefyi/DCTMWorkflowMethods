/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jul 9, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.performers.workflowdispatcher;

import java.io.PrintWriter;

import com.documentum.bpm.IDfWorkitemEx;
import com.documentum.bpm.rtutil.WorkflowMethod;
import com.documentum.fc.client.IDfModule;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfWorkflow;
import com.documentum.fc.client.IDfWorkitem;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfProperties;

/**
 * @author wmoore
 *
 */
public class WorkflowDispatcherMethod extends WorkflowMethod implements IDfModule {

   private static final String PARENT_VARIABLE_NAME = "SwitchTransactionIds";

   private static final String TREXCP_SDT_NAME = "trexcp_fields";
   private static final String TREXCP_ACTION_FIELD = "T_Action";
   private static final String ACTION_VALUE = "Complete";
   
   /* (non-Javadoc)
    * @see com.documentum.bpm.rtutil.WorkflowMethod#doTask(com.documentum.fc.client.IDfWorkitem, com.documentum.fc.common.IDfProperties, java.io.PrintWriter)
    */
   @Override
   protected int doTask(IDfWorkitem workitem, IDfProperties props, PrintWriter pw) throws Exception {
      try {
         pw.write(String.format("Executing server method \"%s\"\n", WorkflowDispatcherMethod.class.getName()));
         
         //retrieve the session
         IDfSession session = workitem.getSession();
         
         //retrieve the list of transaction workflow IDs as a comma-separated list.
         IDfWorkitemEx workitemEx = (IDfWorkitemEx)workitem;
         String switchIds = (String)workitemEx.getPrimitiveVariableValue(PARENT_VARIABLE_NAME);
         pw.write("Retrieved switch workflow IDs: " + switchIds);
         
         if(switchIds != null && !switchIds.equals("")) {
            //break the result up and iterate over all the results
            for(String switchWorkflowId : switchIds.split(",")) {
               pw.write("Dispatching workitem: " + switchWorkflowId);
               IDfWorkitemEx transactionWorkflow = (IDfWorkitemEx) session.getObjectByQualification(String.format("dmi_workitem where r_workflow_id = '%s'and r_runtime_state < %d", switchWorkflowId, IDfWorkflow.DF_WF_STATE_FINISHED));
               transactionWorkflow.setStructuredDataTypeAttrValue(TREXCP_SDT_NAME, TREXCP_ACTION_FIELD, ACTION_VALUE);
               transactionWorkflow.acquire();
               transactionWorkflow.complete();
            }
         }
         
         return 0;
      }
      catch(DfException e) {
         try {
            pw.write("Error occurred while trying to dispatch work related to work item \"" + workitem.getObjectId().toString() + "\", error returned was: " + e.getMessage());
         } catch (DfException ex) { /* do nothing */ }
         return 1;
      }
   }



}
