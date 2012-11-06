/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 17, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.WorkflowCleaner;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfSession;
import com.triteksol.workflow.xcp.ProcessDestroyer;
import com.triteksol.xcp.commandline.base.DocumentumCommandLineBase;

/**
 * @author wmoore
 *
 */
public class WorkflowCleaner extends DocumentumCommandLineBase {
   private static final Logger log = LoggerFactory.getLogger(WorkflowCleaner.class);

   static final String OPT_WF = "workflows";
   static final String OPT_WF_SHORT = "w";
   static final String OPT_TERMINATE = "terminateWF";
   static final String OPT_TERMINATE_SHORT = "t";

   /**
    * The main method - execute the cleaner program
    * @param args
    */
   public static void main(String[] args) {
      log.info("Starting the cleaner utility with arguments {}", new Object[] { args });
      try {
         WorkflowCleaner runner = new WorkflowCleaner();
         System.exit(runner.run(args) ? 1 : 0);
      } catch(RuntimeException ex) {
         log.error("A runtime exception occurred during the program's execution", ex);
         System.err.println("The application failed to execute successfully and returned the following exception: " + ex.getMessage());
         System.exit(1);
      }
   }

   @Override 
   @SuppressWarnings("static-access")
   public Options addOptions(Options opts) {
      Option workflows = OptionBuilder.withArgName("workflowNames").hasArgs()
            .withDescription("The Workflows to erase from the system (removes all active workflows and all versions)").withLongOpt(OPT_WF)
            .withValueSeparator(',').isRequired().create(OPT_WF_SHORT);

      Option terminateWorkflowsOnly = OptionBuilder.withDescription("If \"True\", will only delete workflows, not process templates")
            .withLongOpt(OPT_TERMINATE).create(OPT_TERMINATE_SHORT);
      
      opts.addOption(workflows);
      opts.addOption(terminateWorkflowsOnly);
      
      return opts;
   }

   @Override
   public boolean doWork(IDfSession session, CommandLine line) {
      String[] workflows;

      log.info("Executing the Workflow cleaner utility");
      workflows = line.getOptionValues(OPT_WF);
      boolean terminateWfOnly = line.hasOption(OPT_TERMINATE);
      
      if(workflows != null) {
         log.info("Cleaning workflows specified in input parameter: {}", workflows.toString());
         for(String workflow : workflows) {
            cleanWorkflow(workflow, session, true, terminateWfOnly);
         }
      }
      
      return true;
   }
   
   /**
    * Clean up the process template by deleting all active workflows and uninstall then deleting the process template.
    * @param processName    The workflow to delete
    * @param session       The Documentum session
    * @param allVersions   If True, recursively delete & uninstall all versions of the process template.  If false, only delete the current version.
    * @param terminateWfOnly  If True, only terminate the workflows, don't uninstall and delete the parent WF
    * @return True if the workflow was successfully cleaned up, false otherwise
    */
    public boolean cleanWorkflow(String processName, IDfSession session, boolean allVersions, boolean terminateWfOnly) {
       ProcessDestroyer processDestroyer = new ProcessDestroyer(session);
       boolean processed = false;
       
       //repeat the loop
       while(processed = (processDestroyer.destroyProcessTemplate(processName, true) == true) && allVersions) {
         log.debug("Getting the next prior version of the workflow as we are cleaning all versions");
       }

       log.debug("No more processes left, exiting cleanup workflow process");
       return processed;
    }
   
}
