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
import java.util.Properties;
import java.util.Random;

import org.exolab.castor.xml.schema.SchemaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.sdt.DfStructuredDataTypeDAOFactory;
import com.documentum.bpm.sdt.DfStructuredDataTypeFactory;
import com.documentum.bpm.sdt.IDfPrimitiveType;
import com.documentum.bpm.sdt.IDfStructuredDataType;
import com.documentum.bpm.sdt.IDfStructuredDataTypeDAO;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author wmoore
 *
 */
public class SDTBuilder {
   private static final Logger log = LoggerFactory.getLogger(SDTBuilder.class);

   public static final String SDT_PROPERTY_FILE_NAME = "/dctm-workflow.properties";
   public static final String PROP_SDTNAME = "com.triteksol.xcp.sdtname";
   
   private IDfSession session;
   IDfStructuredDataType sdt;

   /**
    * SDTBuilder constructor.  To build call {@link #startBuilding(IDfSession)}.
    * @param session
    */
   private SDTBuilder(IDfSession session, String sdtName) throws DfException {
      this.session = session;

      DfStructuredDataTypeFactory factory = DfStructuredDataTypeFactory.getInstance();
      this.sdt = factory.createStructuredDataType(sdtName, sdtName, "No Description");
   }
   
   /**
    * Create a new SDTBuilder.  Convience method for {@link #startBuilding(IDfSession, String)} with a second parameter of <em>null</em>
    * @param session The Documentum session
    * @return
    * @throws DfException
    * @throws Exception
    */
   public static SDTBuilder startBuilding(IDfSession session) throws DfException, Exception {
      return SDTBuilder.startBuilding(session, null);
   }
   
   /**
    * Create a new SDTBuilder
    * @param session The Documentum session
    * @param sdtName The name of the Structured Data Type to create
    * @return
    * @throws DfException
    * @throws Exception
    */
   public static SDTBuilder startBuilding(IDfSession session, String sdtName) throws DfException, Exception {
      if(session == null) { throw new RuntimeException("Valid session required to build a new SDT"); }
      String localSdtName = sdtName;
      log.debug("Starting new SDTBuilder");
      if(sdtName == null || sdtName.length() == 0) { 
         InputStream is = SDTBuilder.class.getResourceAsStream(SDT_PROPERTY_FILE_NAME);
         Properties props = new Properties();
         props.load(is);
         localSdtName = props.getProperty(PROP_SDTNAME);
         log.debug("Read sdt name '{}' from properties file", localSdtName);
         if(localSdtName == null || localSdtName.length() == 0) { 
            throw new RuntimeException("No SDT name specified at creation, and unable to read the SDT name from the properties file");
         }
         //concat the name so we don't get deadlocks from using the same SDT name multiple times in the same session
         localSdtName = localSdtName.concat("_").concat(Integer.toString(new Random().nextInt(1000000)));
      }
      else {
         SDTUtility utility = new SDTUtility(session);
         if(utility.getSDTByName(localSdtName) != null) {
            throw new RuntimeException(String.format("SDT already exists in repository with name '%s'", localSdtName));
         }
      }
      
      log.debug("Building SDT with name '{}'", localSdtName);
      return new SDTBuilder(session, localSdtName);
   }
   
   /**
    * Add an attribute to the current SDT
    * @param attrName The Attribute name (and display name)
    * @param type The SDT type, from {@link IDfPrimitiveType}
    * @param defaultValue The default value
    * @return
    * @throws DfException
    * @throws SchemaException
    */
   public SDTBuilder addAttribute(String attrName, IDfPrimitiveType type, Object defaultValue) throws DfException, SchemaException {
      if(attrName == null || attrName.length() == 0) { throw new RuntimeException("Must specify a valid attribute name"); }
      log.debug("Adding attribute '{}' of type '{}'", attrName, type);
      this.sdt.addAttribute(attrName, attrName, "No Description", type, defaultValue, false, false, false);
      return this;
   }
   
   /**
    * Finalize building the SDT and return the result
    * @return
    * @throws DfException
    * @throws Exception
    */
   public IDfStructuredDataType buildSDT() throws DfException, Exception {
      log.debug("Creating SDT");
      IDfStructuredDataTypeDAO sdtDAO = DfStructuredDataTypeDAOFactory.getDocbaseDAO(this.session.getSessionManager(), this.session.getDocbaseName());
      sdtDAO.createNewStructuredDataType(sdt);
      log.debug("SDT Created");
      return this.sdt;
   }
}
