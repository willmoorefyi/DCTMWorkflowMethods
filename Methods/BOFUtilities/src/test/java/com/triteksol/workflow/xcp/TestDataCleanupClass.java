/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 7, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.triteksol.content.xcp.FolderUtils;
import com.triteksol.junit.DocumentumIntegrationTest;
import com.triteksol.workflow.xcp.ProcessDestroyer;

/**
 * Class to clean up bad test data.  Run from eclipse
 * 
 * @author wmoore
 *
 */
public class TestDataCleanupClass extends DocumentumIntegrationTest { 
   private static final Logger log = LoggerFactory.getLogger(TestDataCleanupClass.class);

   @Test public void test() {
      ProcessDestroyer procDestroyer = new ProcessDestroyer(getSession());
      try {
            procDestroyer.destroyProcessTemplate("JUnit Test Workflow", true);
      }
      catch(Exception e) {
            log.error("Unable to teardown Process Template due to inner error", e);
      }
      try {
            procDestroyer.teardownSDT("junit_test_sdt");
            procDestroyer.teardownSDT("testSDT");
      }
      catch(Exception e) {
            log.error("Unable to teardown SDT due to inner error", e);
      }
      try {
            FolderUtils folderUtils = new FolderUtils(getSession());
            folderUtils.destroyFolder(folderUtils.retrieveFolder("/System/Applications", "JUnit Test Workflow"));
      }
      catch(Exception e) {
            log.error("Unable to delete folder due to inner error", e);
      }
   }
}
