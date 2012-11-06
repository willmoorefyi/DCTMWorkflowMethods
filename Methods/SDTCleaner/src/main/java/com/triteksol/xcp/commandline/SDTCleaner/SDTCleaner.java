/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 17, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.SDTCleaner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfSession;
import com.triteksol.workflow.xcp.SDTUtility;
import com.triteksol.xcp.commandline.base.DocumentumCommandLineBase;

/**
 * @author wmoore
 *
 */
public class SDTCleaner extends DocumentumCommandLineBase {
   private static final Logger log = LoggerFactory.getLogger(SDTCleaner.class);
   static final String OPT_SDT = "sdts";

   /**
    * The main method - execute the cleaner program
    * @param args
    */
   public static void main(String[] args) {
      log.info("Starting the cleaner utility with arguments {}", new Object[] { args });
      try {
         SDTCleaner runner = new SDTCleaner();
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
      Option sdts = OptionBuilder.withArgName("SDTs").hasArgs()
            .withDescription("The Structured Data Types to erase from the system (workflows must be completely removed)").withLongOpt(OPT_SDT)
            .withValueSeparator(',').isRequired().create("s");

      opts.addOption(sdts);
      
      return opts;
   }

   @Override
   public boolean doWork(IDfSession session, CommandLine line) {
      String[] sdts;
      sdts = line.getOptionValues(OPT_SDT);

      log.info("Executing the SDT cleaner utility");
      if(sdts != null) {
         log.info("Cleaning Structured Data Types specified in input parameter: {}", sdts.toString());
         for(String sdt : sdts) {
            cleanSDT(sdt, session);
         }
      }
      
      return true;
   }
   
   /**
    * Main work method to clean the Structured Data Type
    * @param sdtName  The Structured Data Type to clean up behind
    * @param session       The Documentum session
    * @return True if the SDT is deleted, false if it is ignored
    */
   public boolean cleanSDT(String sdtName, IDfSession session) {
      log.debug("Cleaning Structured Data Type '{}'", sdtName);
      
      SDTUtility utility = new SDTUtility(session);
      
      //cleanup the database tables
      utility.emptyAllSDTTables(sdtName);
      
      //delete the structured data type
      return utility.deleteSDT(sdtName);
   }
}
