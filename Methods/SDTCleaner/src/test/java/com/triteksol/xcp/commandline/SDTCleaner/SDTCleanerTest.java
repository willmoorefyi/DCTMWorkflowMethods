/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 17, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.SDTCleaner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.sdt.IDfPrimitiveType;
import com.documentum.bpm.sdt.IDfStructuredDataType;
import com.documentum.fc.client.IDfSession;
import com.triteksol.junit.DocumentumIntegrationTest;
import com.triteksol.workflow.xcp.SDTBuilder;
import com.triteksol.workflow.xcp.SDTUtility;
import com.triteksol.xcp.commandline.SDTCleaner.SDTCleaner;

/**
 * @author wmoore
 * 
 */
public class SDTCleanerTest extends DocumentumIntegrationTest {
   private static final Logger log = LoggerFactory.getLogger(SDTCleanerTest.class);

   @Test
   public void test_run() {
      SDTCleanerCommandLineMock mock = new SDTCleanerCommandLineMock();

      List<String> args = new ArrayList<String>();
      args.add("-" + SDTCleaner.OPT_USER);
      args.add(getUsername());
      args.add("-" + SDTCleaner.OPT_PWD);
      args.add(getPassword());
      args.add("-" + SDTCleaner.OPT_DOCBASE);
      args.add(getDocbase());
      args.add("-" + SDTCleaner.OPT_SDT);
      args.add("SDT1,SDT2,SDT3");

      mock.run(args.toArray(new String[args.size()]));

      Assert.assertEquals("Expected to parse input workflow string to ", 3, mock.countSDT);
   }

   @Test public void test_cleaner() throws Exception {
      log.info("Executing SDTCleaner delete SDT in use");
      SDTUtility utility = new SDTUtility(getSession());
      
      //use hte default SDT name and property name from the properties file, for simplicity
      InputStream is = SDTBuilder.class.getResourceAsStream(SDTBuilder.SDT_PROPERTY_FILE_NAME);
      Properties props = new Properties();
      props.load(is);
      String sdtName = props.getProperty(SDTBuilder.PROP_SDTNAME);
      
      log.debug("Creating test SDT");
      IDfStructuredDataType sdt = SDTBuilder.startBuilding(getSession(), sdtName).addAttribute("attr1", IDfPrimitiveType.STRING, "")
            .addAttribute("attr2", IDfPrimitiveType.INT, new Integer(1)).buildSDT();
      
      Assert.assertNotNull("new SDT should be valid", utility.getSDTByName(sdtName));
      
      try {
         log.debug("Deleting SDT: {}", sdt.getName());
         new SDTCleaner().cleanSDT(sdt.getName(), getSession());
      } catch (Exception e) {
         log.debug("Error occurred in cleaner, destroying SDT");
         utility.deleteSDT(sdt);
         throw e;
      }
      
      Assert.assertNull("Cleaend and Deleted SDT should now be null", utility.getSDTByName(sdtName));
   }

   class SDTCleanerCommandLineMock extends SDTCleaner {
      private int countSDT = 0;

      @Override
      public boolean cleanSDT(String sdtName, IDfSession session) {
         countSDT++;
         return true;
      }
   }
}
