/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 20, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.base;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfSession;
import com.triteksol.junit.DocumentumIntegrationTest;

/**
 * @author wmoore
 * 
 */
public class DocumentumCommandLineBaseTest extends DocumentumIntegrationTest {
   private static final Logger log = LoggerFactory.getLogger(DocumentumCommandLineBaseTest.class);

   @Test
   public void testDocumentumCommandLineBaseParsing() {
      log.info("Executing parsing test");
      List<String> args = startArgs();
      // args.add("-" + DocumentumCommandLineBase.OPT_WF);
      // args.add("Switching Assistance Master Workflow");

      try {
         CommandLine cli = new DocumentumCommandLineBaseMock().parseCommandLine(args.toArray(new String[args.size()]));
         Assert.assertEquals("Verify users", getUsername(), cli.getOptionValue(DocumentumCommandLineBase.OPT_USER));
         Assert.assertEquals("Verify password", getPassword(), cli.getOptionValue(DocumentumCommandLineBase.OPT_PWD));
         Assert.assertEquals("Verify docbase", getDocbase(), cli.getOptionValue(DocumentumCommandLineBase.OPT_DOCBASE));
      } catch (ParseException ex) {
         throw new RuntimeException("Error occurred during test case. ", ex);
      }
   }

   @Test(expected = MissingOptionException.class)
   public void testDocumentumCommandLineBaseBadArgs_Empty() throws ParseException {
      DocumentumCommandLineBase mock = new DocumentumCommandLineBaseMock();
      List<String> args = new ArrayList<String>();
      Assert.assertFalse("Empty command line should not be valid", mock.verifyArgs(mock.parseCommandLine(args.toArray(new String[args.size()]))));

      log.info("Passed empty docbase test");
      log.info("Passed empty password test");
   }

   @Test(expected = MissingOptionException.class)
   public void testDocumentumCommandLineBaseBadArgs_NoDocbase() throws ParseException {
      DocumentumCommandLineBase mock = new DocumentumCommandLineBaseMock();
      List<String> args = startArgs().subList(0, 4);
      // remove the docbase
      Assert.assertFalse("Missing Docbase command line should not be valid", mock.verifyArgs(mock.parseCommandLine(args.toArray(new String[args.size()]))));
   }

   @Test(expected = MissingOptionException.class)
   public void testDocumentumCommandLineBaseBadArgs_NoUser() throws ParseException {
      DocumentumCommandLineBase mock = new DocumentumCommandLineBaseMock();
      List<String> args = startArgs().subList(2, 6);
      // remove the user
      Assert.assertFalse("Missing user command line should not be valid", mock.verifyArgs(mock.parseCommandLine(args.toArray(new String[args.size()]))));
      log.info("Passed empty username test");

   }

   @Test(expected = MissingOptionException.class)
   public void testDocumentumCommandLineBaseBadArgs_NoPassword() throws ParseException {
      DocumentumCommandLineBase mock = new DocumentumCommandLineBaseMock();
      List<String> args = startArgs();
      // remove the password
      args.subList(2, 4).clear();
      Assert.assertFalse("Missing password command line should not be valid", mock.verifyArgs(mock.parseCommandLine(args.toArray(new String[args.size()]))));
   }

   /**
    * Helper method to add the starting arguments
    * 
    * @return
    */
   public List<String> startArgs() {
      List<String> args = new ArrayList<String>();
      args.add("-" + DocumentumCommandLineBase.OPT_USER);
      args.add(getUsername());
      args.add("-" + DocumentumCommandLineBase.OPT_PWD);
      args.add(getPassword());
      args.add("-" + DocumentumCommandLineBase.OPT_DOCBASE);
      args.add(getDocbase());
      return args;
   }

   public class DocumentumCommandLineBaseMock extends DocumentumCommandLineBase {
      @Override
      public Options addOptions(Options opts) {
         return opts;
      }

      @Override
      public boolean doWork(IDfSession session, CommandLine line) {
         return true;
      }

   }
}
