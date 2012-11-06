/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jul 1, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;


/**
 * 
 * @author Julio Espinoza-Sokal
 * @author TriTek Solutions, Inc.
 *
 */
public class DataParser {
   private static final Logger log = LoggerFactory.getLogger(DataParser.class);

   /**
    * @param filename
    * @return
    */
   static List<Map<String, Object>> readData(String filename) {
      log.debug("Parsing input filename '{}' into data map", filename);
      CSVReader reader = null;
      try {
         reader = new CSVReader(new InputStreamReader(new FileInputStream(filename)));
   
         String[] keys = reader.readNext();
         if (keys == null) {
            log.warn("Data file has no data lines.");
            return null;
         }
         
         log.debug("Beginning to parse the input file");
   
         List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
   
         String[] line = null;
         while ((line = reader.readNext()) != null) {
            HashMap<String, Object> returnMap = new HashMap<String, Object>();
            if(line.length != keys.length) {
               throw new RuntimeException("Input file is not a valid CSV, uneven number of key fields and values on one of the input file lines.");
            }
            for (int i = 0; i < keys.length; i++) {
               String key = keys[i];
               String value = line[i];
               if (value.indexOf('|') >= 0) {
                  returnMap.put(key, value.split("\\|"));
               }
               else {
                  returnMap.put(key, value);
               }
            }
            dataList.add(returnMap);
         }
         log.debug("Completed processing successfully, read {} lines from input file", dataList.size());
         return dataList;
      }
      catch (FileNotFoundException e) {
         log.error("Data file '{}' not found.", filename);
         throw new RuntimeException(String.format("Data file '%s' not found.", filename), e);
      }
      catch (IOException e) {
         log.error("Failed to read CSV file specified by filename '{}'.", filename);
         throw new RuntimeException(String.format("Failed to read CSV file specified by filename '%s'", filename), e);
      }
      finally {
         try {
            if(reader != null) {
               reader.close();
            }
         }
         catch (IOException e) {
            log.error("Failed to close CSVReader.");
            throw new RuntimeException("Failed to close CSVReader.", e);
         }
      }
   }

}