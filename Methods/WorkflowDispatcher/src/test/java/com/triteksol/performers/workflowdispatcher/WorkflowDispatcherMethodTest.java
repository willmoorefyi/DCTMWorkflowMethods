/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jul 9, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.performers.workflowdispatcher;

import org.junit.Before;
import org.junit.Test;

import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 *
 */
public class WorkflowDispatcherMethodTest extends DocumentumIntegrationTest {
   
   public static final String DISPATCHER_RESOURCE_FILE = "/dctm-worfklow-dispatcher.properties";
   
   String parent_wf;
   String child_wf;

   @Before public void setup_dispatcher() {
      try {
         //InputStream is = getClass().getResourceAsStream( DISPATCHER_RESOURCE_FILE );
         //Properties props = new Properties();
         //props.load(is);
         
         parent_wf = "Test Workflow Dispatcher";
         child_wf = "Switching Transaction Process";
         
      } catch (Exception e) {
         throw new RuntimeException("Could not create dispatcher configuration", e);
      }
   }
   
   @Test public void test_wf_dispatch() {
      //WorkflowLauncher.launchWorkflows(getSession(), parent_wf, "/");
   }
}
