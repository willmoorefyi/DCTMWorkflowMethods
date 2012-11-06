/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 7, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.sdt.DfStructuredDataTypeDAOFactory;
import com.documentum.bpm.sdt.IDfStructuredDataType;
import com.documentum.bpm.sdt.IDfStructuredDataTypeDAO;
import com.documentum.bpm.sdt.SDTNotFoundException;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author wmoore
 *
 */
public class SDTUtility {
   private static final Logger log = LoggerFactory.getLogger(SDTUtility.class);

   private IDfSession session;
   
   public SDTUtility(IDfSession session) {
      this.session = session;
   }
   
   /**
    * Retrieve the SDT object by name
    * @param sdtName
    * @return
    * @throws Exception
    */
   public IDfStructuredDataType getSDTByName(String sdtName) throws Exception {
      IDfStructuredDataTypeDAO sdtDAO = DfStructuredDataTypeDAOFactory.getDocbaseDAO(session.getSessionManager(), session.getDocbaseName());
      Enumeration<IDfStructuredDataType> sdts = sdtDAO.findStructuredDataTypeByName(sdtName);
      if(sdts.hasMoreElements()) {
         //SDT names are unique, so this should only ever have more than 1 element
         return sdts.nextElement();
      }
      else {
         return null;
      }
   }
   
   /**
    * Delete any unreferenced objects remaining in the SDT tables (active and report), allowing the SDT table to be deleted.
    * @param sdt The Structured Data Type to clean from
    */
   public void emptyAllSDTTables(IDfStructuredDataType sdt) {
      if(sdt == null) { throw new RuntimeException("SDT cannot be null"); }
      emptyAllSDTTables(sdt.getName());
   }

   /**
    * Delete any unreferenced objects remaining in the SDT tables (active and report), allowing the SDT table to be deleted.
    * @param sdtName  The Structured Data Type to clean up behind
    */
   public void emptyAllSDTTables(String sdtName) {
      if(sdtName == null || "".equals(sdtName)) { throw new RuntimeException("A named structured data type is required to attempt to delete!"); }
      
      log.debug("Cleaning SDT tables for type '{}'", sdtName);
      try {
         IDfQuery query = new DfQuery();
         String dql = String.format("select element_type_name, report_type_name from dmc_wfsd_type_info where sdt_name = '%s'", sdtName);
         log.debug("Executing query \"{}\"", dql);
         query.setDQL(dql);
         IDfCollection results = query.execute(this.session, IDfQuery.DF_READ_QUERY);
         if(results.next()) {
            String tableName = results.getString("element_type_name");
            String reportName = results.getString("report_type_name");
            
            log.debug("Cleaning element table of unreferenced SDT values");
            //there should always be a element data table
            deleteSDTTable(tableName);
            
            if(reportName != null && !"".equals(reportName)) {
               log.debug("Cleaning report table of unreferenced SDT values");
               deleteSDTTable(reportName);
            } else {
               log.debug("SDT '{}' has no reportable tables to be cleaned, ignoring", sdtName);
            }
         } else {
            log.debug("No database information found for type specified by: '{}', ignoring", sdtName);
         }
      } catch(DfException ex) {
         throw new RuntimeException("Error occurred while attempting to clean the database for SDT: " + sdtName, ex);
      }
   }
   
   /**
    * Clean up the SDT lightweight data object table specified.  Occasionally unreferenced SDT data is left in these tables,
    * and this method mimics the dmclean component whereby objects without parent IDs are deleted. 
    * @param tableName  The table to clean
    * @throws DfException
    */
   public void deleteSDTTable(String tableName) throws DfException {
      IDfQuery query = new DfQuery();
      String dql = String.format("delete %s objects where workflow_id NOT IN (select r_object_id from dm_workflow)", tableName);
      log.debug("executing query \"{}\"", dql);
      query.setDQL(dql);
      IDfCollection results2 = query.execute(this.session, IDfQuery.EXEC_QUERY);
      if(results2.next()) {
         log.info("Deleted '{}' objects from table '{}'", results2.getString("objects_deleted"), tableName);
      }
   }
   
   /**
    * Delete the structured data type
    * @param sdt The Structured Data Type to delete
    * @return True if the structured data type is cleaned up, false otherwise
    */
   public void deleteSDT(IDfStructuredDataType sdt) {
      if(sdt == null) { throw new RuntimeException("Can not delete null structured data type"); }
      try {
         IDfStructuredDataTypeDAO sdtDAO = DfStructuredDataTypeDAOFactory.getDocbaseDAO(session.getSessionManager(), session.getDocbaseName());
         sdtDAO.deleteStructuredDataType(sdt.getName());
         log.info("SDT successfully deleted from the repository");
      } catch (SDTNotFoundException e) {
         throw new RuntimeException("Exception occurred trying to delete the Structured Data Type, the SDT does not exist.", e);
      } catch (DfException e) {
         throw new RuntimeException("Execption occurred trying to delete Structured Data Type", e);
      } catch (Exception e) {
         throw new RuntimeException("A generic exception occurred while trying to delete Structured Data Type ", e);
      }
   }
   
   /**
    * Delete the structured data type
    * @param sdtName  The Structured Data Type to delete
    * @return True if the structured data type is cleaned up, false otherwise
    */
   public boolean deleteSDT(String sdtName) {
      if(sdtName == null || "".equals(sdtName)) { throw new RuntimeException("A named structured data type is required to attempt to delete!"); }
      
      log.debug("Deleting SDT \"{}\"", sdtName);
      try {
         IDfStructuredDataType sdt = getSDTByName(sdtName);
         if(sdt != null) {
            //delete the structured data type
            deleteSDT(sdt);
            return true;
         } else {
            log.info("SDT \"{}\" does not exist in the repository", sdtName);
            return false;
         }
      } catch (Exception e) {
         throw new RuntimeException("A generic exception occurred while trying to delete Structured Data Type " + sdtName, e);
      }
   }
}
