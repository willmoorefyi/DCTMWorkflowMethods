/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Apr 20, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline.base;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.triteksol.xcp.commandline.base.DocumentumSessionManager;

/**
 * @author wmoore
 * 
 */
public abstract class DocumentumCommandLineBase {
   private static final Logger log = LoggerFactory.getLogger(DocumentumCommandLineBase.class);

   public static final String OPT_USER = "user";
   public static final String OPT_PWD = "password";
   public static final String OPT_DOCBASE = "docbase";

   /**
    * The primary processing thread. This method will parse the command line,
    * verify the command-line arguments are valid, then create the Documentum
    * session and invoke the doWork method.
    * 
    * The "main" method of the child classes should invoke this method
    * 
    * @param args
    *           The command-line arguments
    * @return
    *           True if the calling method "doWork" completed successfully, false otherwise (including scenarios where the method is never invoked)
    */
   public final boolean run(String[] args) {
      CommandLine line;
      // start by parsing the command line
      try {
         line = parseCommandLine(args);
      } catch (MissingArgumentException ex) {
         log.error("A required argument was not provided", ex);
         System.err.println("A required argument was not provided to the program:" + ex.getMessage());
         return false;
      } catch (ParseException ex) {
         log.error("An exception occurred while parsing the command line", ex);
         System.err.println("An exception occurred while parsing the command line:" + ex.getMessage());
         return false;
      }

      String userName, pwd, docBase;

      if (!verifyArgs(line)) {
         throw new RuntimeException("A required parameter was not set for the application to run.  Please make sure all required parameters have been defined.");
      }
      
      userName = line.getOptionValue(OPT_USER);
      pwd = line.getOptionValue(OPT_PWD);
      docBase = line.getOptionValue(OPT_DOCBASE);

      log.info("All necessary command-line paramters set.  Executing the Documentum command-line utility");
      log.debug("Utilizing runner parameters username:\"{}\", password: \"{}\", docbase: \"{}\"", new String[] { userName, pwd, docBase });

      DocumentumSessionManager sessMgr = new DocumentumSessionManager();
      try {
         // retrieve the Documentum session
         IDfSession session = sessMgr.getSession(userName, pwd, docBase);
         // do the work
         return doWork(session, line);
      } catch (DfException e) {
         throw new RuntimeException("Error occurred during login", e);
      } finally {
         // release the Documentum session
         sessMgr.releaseSession();
      }
   }

   /**
    * Create the input options
    * 
    * @return the Options
    */
   @SuppressWarnings("static-access")
   final Options createOptions() {
      Options opts = new Options();
      // create the username option
      Option username = OptionBuilder.withArgName("userName").hasArg().withDescription("User to connect to the repository").withLongOpt(OPT_USER).isRequired()
            .create("u");
      // create the password option
      Option password = OptionBuilder.withArgName("password").hasArg().withDescription("Password to connect to the repository").withLongOpt(OPT_PWD)
            .isRequired().create("p");
      // create the repository option
      Option repository = OptionBuilder.withArgName("docbase").hasArg().withDescription("The Documentum repository to connect to").withLongOpt(OPT_DOCBASE)
            .isRequired().create("d");
      // add the previously created options to the collection
      opts.addOption(username);
      opts.addOption(password);
      opts.addOption(repository);

      return opts;
   }

   /**
    * Verify the required arguments are present on the command line.
    * 
    * @param line
    *           The parsed command line input
    * @return True of all the required arguments are present, false otherwise
    */
   boolean verifyArgs(CommandLine line) {
      if (line.hasOption(OPT_USER) && line.hasOption(OPT_PWD) && line.hasOption(OPT_DOCBASE)) {
         return true;
      } else {
         return false;
      }
   }

   /**
    * Parse the input command line
    * 
    * @param args
    *           the input arguments on the command line
    * @return A parsed command line object
    * @throws ParseException
    */
   CommandLine parseCommandLine(String[] args) throws ParseException {
      Options opts = createOptions();
      opts = addOptions(opts);
      return new GnuParser().parse(opts, args);
   }

   /**
    * The sublcasses should override this method to add their additional
    * commnad-line options, to handle specific use cases
    * 
    * @param opts
    *           The Options object representing the default options necessary to
    *           connect to the repository (Username, Password, Docbase)
    * @param caller
    *           The calling class, used to handle use-case specific scenarios.
    * @return The final list of command-line options
    */
   public abstract Options addOptions(Options opts);

   /**
    * The subclasses should override this method to do the actual processing
    * necessary. Within this method the application provides a valid Documentum
    * session and the command-line parameters. The sessions will be managed by
    * the caller, so the client can feel free to throw exceptions without
    * worrying about session management.
    * 
    * @param session
    *           The Documentum session
    * @param line
    *           The remaining command-line parameters
    * @return True if processing was successful, false otherwise
    */
   public abstract boolean doWork(IDfSession session, CommandLine line);
}
