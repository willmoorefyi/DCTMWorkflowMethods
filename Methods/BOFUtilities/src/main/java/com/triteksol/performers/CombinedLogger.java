package com.triteksol.performers;

import java.io.PrintWriter;

import org.apache.log4j.Logger;


/**
 * A combined log that includes both a PrintWriter and a log4j Logger.  Useful for Documentum Workflow Methods, where the method signature includes a 
 * {@link PrintWriter} to output log messages to.
 * 
 * @author TriTek Solutions, Inc.
 *
 */
public class CombinedLogger {
   private Logger log;
   private PrintWriter writer;
   
   /**
    * Instantiate a CombinedLogger with only a Log4j Logger.  The PrintWriter can be set later using the {@link #setWriter(PrintWriter)} method.
    * @param log the log to set
    */
   public CombinedLogger(Logger log) {
      this.log = log;
      this.writer = null;
   }
   
   /**
    * Instantiate a CombinedLogger with only a PrintWriter. The Log4j Logger can be set later using the {@link #setLog(Logger)} method.
    * @param writer the writer to set
    */
   public CombinedLogger(PrintWriter writer) {
      this.log = null;
      this.writer = writer;
   }

   /**
    * Instantiate a CombinedLogger with both Log4j logger and a Printwriter
    * @param log the log to set
    * @param writer the writer to set
    */
   public CombinedLogger(Logger log, PrintWriter writer) {
      this.log = log;
      this.writer = writer;
   }

   /**
    * Set the Log4j Logger for use by the CombinedLogger
    * @param log the log to set
    */
   public void setLog(Logger log) {
      this.log = log;
   }

   /**
    * Set the PrintWriter to be used by the CombinedLogger
    * @param writer the writer to set
    */
   public void setWriter(PrintWriter writer) {
      this.writer = writer;
   }

   public void fatal(String message) {
      if (this.log != null) {
         this.log.error(message);
      }
      if (this.writer != null) {
         this.writer.println(message);
      }
   }

   public void error(String message) {
      if (this.log != null) {
         this.log.error(message);
      }
      if (this.writer != null) {
         this.writer.println(message);
      }
   }

   public void error(String message, Throwable t) {
      if (this.log != null) {
         this.log.error(message, t);
      }
      if (this.writer != null) {
         this.writer.println(String.format("%s %s", message, t.getMessage()));
      }
   }

   public void info(String message) {
      if (this.log != null) {
         this.log.info(message);
      }
      if (this.writer != null) {
         this.writer.println(message);
      }
   }

   public void debug(String message) {
      if (this.log != null) {
         this.log.debug(message);
      }
      boolean debug = this.log == null ? true : this.log.isDebugEnabled();
      if (debug && this.writer != null) {
         this.writer.println(message);
      }
   }
}
