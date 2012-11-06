/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jul 1, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.exolab.castor.types.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.common.IDfAttr;
import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 *
 */
public class WorkflowLauncherTest extends DocumentumIntegrationTest {
   private static final Logger log = LoggerFactory.getLogger(WorkflowLauncherTest.class);
   
   @Test(expected=RuntimeException.class) public void testBadFilename() {
      log.info("Testing bad filename");
      WorkflowLauncher wf = new WorkflowLauncher();
      List<String> args = new ArrayList<String>();
      args.add("-" + WorkflowLauncher.OPT_USER);
      args.add(getUsername());
      args.add("-" + WorkflowLauncher.OPT_PWD);
      args.add(getPassword());
      args.add("-" + WorkflowLauncher.OPT_DOCBASE);
      args.add(getDocbase());
      args.add("-" + WorkflowLauncher.OPT_WF_NAME);
      args.add("fail");
      args.add("-" + WorkflowLauncher.OPT_WF_INPUT_FILE);
      args.add("/dev/null/therenevershouldbeafilehere.fail");
      wf.run(args.toArray(new String[args.size()]));
   }
   
   
   @Test public void formattedPropertyValues_success() {
      Assert.assertEquals("For string values input string should match output string", "Value", WorkflowLauncher.getFormattedProperty(IDfAttr.DM_STRING, "Value"));
      org.joda.time.DateTime date = new org.joda.time.DateTime(new Date());
      Assert.assertEquals("Parsed dates should match input date after milliseconds have been stripped", new DateTime(date.withMillisOfSecond(0).toDate()).toString(), WorkflowLauncher.getFormattedProperty(IDfAttr.DM_TIME, WorkflowLauncher.DATETIME_FORMAT.print(date)));
   }
   
   @Test(expected = UnsupportedOperationException.class) public void formattedPropertyValues_Arrays() {
      WorkflowLauncher.getFormattedProperty(IDfAttr.DM_STRING, new String[] {"1","2"});
   }
   
}
