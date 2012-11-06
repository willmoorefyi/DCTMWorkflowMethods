/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jul 1, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wmoore
 *
 */
public class DataParserTest {
   private static final Logger log = LoggerFactory.getLogger(DataParserTest.class);
   
   private static final String EMPTY_FILENAME = "/empty.csv";
   private static final String VALID_FILENAME = "/workflow.csv";
   
   @Test public void testEmptyFile() {
      log.info("Starting empty file test");
      String filename = getClass().getResource(EMPTY_FILENAME).getFile();
      log.debug("retrieved file name '{}'", filename);
      Assert.assertNull("Empty file should return null from data set", DataParser.readData(filename));
   }
   
   @Test(expected=RuntimeException.class) public void testNoFile() {
      log.info("Starting failure test - no file");
      String filename = "/dev/null/therenevershouldbeafilehere.fail";
      DataParser.readData(filename);
   }
   
   @Test public void testValidFile() {
      log.info("starting test - success");
      String filename = getClass().getResource(VALID_FILENAME).getFile();
      log.debug("retrieved file name '{}'", filename);
      List<Map<String, Object>> data = DataParser.readData(filename);
      Assert.assertEquals("Input has 1 row", data.size(), 1);
   }
}
