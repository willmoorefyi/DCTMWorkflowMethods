/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 7, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.sdt.IDfPrimitiveType;
import com.documentum.bpm.sdt.IDfStructuredDataType;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.common.IDfId;
import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 *
 */
public class SDTUtilityTest extends DocumentumIntegrationTest {
   private static final Logger log = LoggerFactory.getLogger(SDTUtilityTest.class);

   @Test public void test_deleteSDT_Bad() {
      //this test is supposed to throw a "SDTNotFoundException", but the Documentum documentation is wrong.  The method throws a nullpointerexception
      SDTUtility utility = new SDTUtility(getSession());
      log.info("Executing SDTCleaner delete SDT bad data");
      Assert.assertFalse("We should return false from the cleaner method if the SDT does not exist", utility.deleteSDT("badSDTNameForJUnitTest"));
   }
   
   @Test public void test_deleteSDT() throws Exception {
      log.info("Executing SDTCleaner delete SDT good data");
      SDTUtility utility = new SDTUtility(getSession());
      
      Assert.assertNull("", utility.getSDTByName("badSDTNameForJUnitTest"));
      
      ProcessTestHarness testHarness = new ProcessTestHarness();
      testHarness.setupProcess(getSession(), false);
      //get the SDT and SDT Utility
      String sdtName = testHarness.getSdtName();
      log.debug("Using SDT '{} for test", sdtName);
      try {
         WorkflowLauncherTestHarness launcher = new WorkflowLauncherTestHarness(getSession());
         List<IDfId> workflowIds = launcher.createWorkflows(testHarness.getProcessName(), 1);
         //verify we cannot delete an SDT that has active processes
         try { 
            //this should throw an exception as the SDT is in use
            utility.deleteSDT(sdtName);
            Assert.fail("The previous call to delete the SDT should fail as the SDT is in use");
         }
         catch(Exception e) {
            log.debug("Could not delete SDT, this is expected, error message: {}", e.getMessage());
         }
         finally {
            launcher.teardownWorkflowTest(workflowIds);
         }
         
         //verify we can't delete an SDT that is part of an installed / in-use process
         try {
            utility.deleteSDT(sdtName);
            Assert.fail("The previous call to delete the SDT should fail as the SDT is part of an installed SDT");
         }
         catch(Exception e) {
            log.debug("Could not delete SDT, this is expected, error message: {}", e.getMessage());
         }

         log.debug("Verifying SDT still exists");
         Assert.assertNotNull("SDT lookup should return SDT, as it should still be in the repository", utility.getSDTByName(sdtName));
         ProcessDestroyer destroyer = new ProcessDestroyer(getSession());
         destroyer.destroyProcessTemplate(testHarness.getProcessName(), true);
         Assert.assertTrue("We should return true from the cleaner method when the SDT is successfully destroyed", utility.deleteSDT(sdtName));
         Assert.assertNull("", utility.getSDTByName(sdtName));
         log.debug("Destroyed SDT");
      }
      finally {
         testHarness.tearDownProcess(getSession(), true, true);
      }
   }
   
   //this is not a good test, but I can't figure out how to replicate the scenario as it should not happen in a healthy system (but appears to happen regularly enough).
   //possibly this is a result of having aborted workflows and not cleaning them up properly?
   //TODO fix this test for cleanupSDTTables
   @Test public void test_cleanupSDTTables() throws Exception {
      log.info("Executing SDTCleaner clean SDT table");
      SDTUtility utility = new SDTUtility(getSession());
      
      //use the default SDT name and property name from the properties file, for simplicity
      InputStream is = this.getClass().getResourceAsStream(SDTBuilder.SDT_PROPERTY_FILE_NAME);
      Properties props = new Properties();
      props.load(is);
      String sdtName = props.getProperty(SDTBuilder.PROP_SDTNAME);
      //concat the name so we don't get deadlocks from using the same SDT name multiple times in the same session
      sdtName = sdtName.concat("_").concat(Integer.toString(new Random().nextInt(1000000)));
      log.debug("Creating test SDT");
      IDfStructuredDataType sdt = SDTBuilder.startBuilding(getSession(), sdtName).addAttribute("attr1", IDfPrimitiveType.STRING, "")
            .addAttribute("attr2", IDfPrimitiveType.INT, new Integer(1)).buildSDT();
      
      log.debug("Built SDT");
      try {
         utility.emptyAllSDTTables(sdtName);
   
         IDfQuery query = new DfQuery();
         String dql = String.format("select element_type_name, report_type_name from dmc_wfsd_type_info where sdt_name = '%s'", sdtName);
         log.debug("Executing query \"{}\"", dql);
         query.setDQL(dql);
         IDfCollection results = query.execute(getSession(), IDfQuery.DF_READ_QUERY);
         Assert.assertTrue("SDT should have an associated table", results.next());
         String tableName = results.getString("element_type_name");
         String reportName = results.getString("report_type_name");
         log.debug("Found tables, object data '{}', report data '{}'", tableName, reportName);
         
         Assert.assertNull("SDT Table should be empty", getSession().getObjectByQualification(tableName + " where workflow_id NOT IN (select r_object_id from dm_workflow)"));
      }
      catch (Exception e) {
         //hope this works at this point
         log.error("Exception occurred during processing");
         throw e;
      }
      finally {
         utility.deleteSDT(sdt);
      }
   }
}
