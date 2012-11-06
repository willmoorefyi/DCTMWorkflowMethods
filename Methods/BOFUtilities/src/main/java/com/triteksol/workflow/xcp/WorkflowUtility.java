/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 6, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.workflow.xcp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.bpm.IDfWorkflowEx;
import com.documentum.bpm.IDfWorkitemEx;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfEnumeration;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.client.IDfWorkitem;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.triteksol.content.xcp.DocumentIdentifier;
import com.triteksol.content.xcp.TrexCPVirtualFolder;
import com.triteksol.content.xcp.VersionIdentifier;

//TODO Build a unit test class for WorkflowPackageUtility

/**
 * Utility class to make working with workflow packages and attachments easier
 * 
 * @author wmoore
 *
 */
public class WorkflowUtility {
   private static Logger log = LoggerFactory.getLogger(WorkflowUtility.class);
   
   public static final String DQL_ACTIVE_WORKITEM = "SELECT wi.r_object_id, wi.i_vstamp, wi.i_is_replica FROM dmi_workitem wi where wi.r_workflow_id = '%1$s'" +
         "AND wi.r_runtime_state IN (" + IDfWorkitem.DF_WI_STATE_DORMANT + "," + IDfWorkitem.DF_WI_STATE_ACQUIRED + ")";

   public static final String TREXCP_SDT_NAME = "trexcp_fields";
   public static final String TREXCP_ATTACHMENTS_FIELD = "T_Attachments";
   public static final String FIELD_ATTACHMENT_OBJECT_IDS = "r_component_id";
   public static final String FIELD_ATTACHMENT_ID = "r_object_id";
   
   private IDfSession session;
   
   public WorkflowUtility(IDfSession session) {
      this.session = session;
   }
   
   public List<IDfWorkitemEx> getActiveWorkitemsFromWorkflow(IDfWorkflowEx workflow) throws DfException {
      if(workflow == null) { throw new RuntimeException("Input workflow when retrieving active work items cannot be null"); }
      log.debug("Retrieving all active work items from passed-in workflow '{}'", workflow.getObjectId());
      List<IDfWorkitemEx> returnVal = new ArrayList<IDfWorkitemEx>();
      
      String query = String.format(DQL_ACTIVE_WORKITEM, workflow.getObjectId());
      IDfEnumeration results = session.getObjectsByQuery(query, "dmi_workitem");
      while(results.hasMoreElements()) {
         returnVal.add((IDfWorkitemEx)results.nextElement());
      }
      
      log.debug("Successfully found {} work items for the passed-in workflow", returnVal.size());
      return returnVal;
   }
   
   /**
    * Retrieve a TrexCP-style virtual folder for a given workitem
    * @param workitem
    * @return
    * @throws DfException
    */
   public TrexCPVirtualFolder getVirtualFolderFromWorkItem(IDfWorkitemEx workitem) throws DfException {
      String attachments = (String)workitem.getStructuredDataTypeAttrValue(TREXCP_SDT_NAME, TREXCP_ATTACHMENTS_FIELD);
      return new TrexCPVirtualFolder(attachments);
   }
   
   /**
    * Save the TrexCP Virtual folder to the work item.  Saves the workitem
    * @param workitem
    * @param folder
    * @throws DfException
    */
   public void saveTrexCPVirtualFolderOnWorkitem(IDfWorkitemEx workitem, TrexCPVirtualFolder folder) throws DfException {
      workitem.setStructuredDataTypeAttrValue(TREXCP_SDT_NAME, TREXCP_ATTACHMENTS_FIELD, folder.getFolderID());
      workitem.save();
      workitem.fetch(null);
   }
   
   /**
    * Save the TrexCPVirtual folder as attachments on the work item
    * @param session Documentum session
    * @param workitem Work item to save to
    * @param folder The folder to save
    * @throws DfException
    */
   public void saveTrexCPVirtualFolderAsAttachments(IDfSession session, IDfWorkitemEx workitem, TrexCPVirtualFolder folder) throws DfException {
      if(session == null) { throw new RuntimeException("Session is required for call to saveTrexCPVirtualFolderAsAttachments"); }
      if(workitem == null) { throw new RuntimeException("Work item cannot be null in call to saveTrexCPVirtualFolderAsAttachments"); }
      if(folder == null) { throw new RuntimeException("Folder cannot be null in call to saveTrexCPVirtualFolderAsAttachments"); }
      log.debug("Saving TrexCP Virtual Folder as attachments list on work item");
      
      Set<IDfId> attachmentIds = new HashSet<IDfId>();
      IDfCollection attachments = workitem.getAttachments();
      while(attachments != null && attachments.next()) {
         IDfTypedObject obj = attachments.getTypedObject();
         attachmentIds.add(obj.getId(FIELD_ATTACHMENT_OBJECT_IDS));
      }
      
      for(IDfDocument document : folder.getAllDocuments(session)) {
         IDfId docId = document.getObjectId();
         if(!attachmentIds.contains(docId)) {
            log.debug("Adding attachment to workf item for document with ID '{}'", docId);
            workitem.addAttachment("dm_document", docId);
         } else {
            log.debug("Workflow already has document attachment");
         }
      }
      workitem.save();
      workitem.fetch(null);
   }

   /**
    * Convenience method to get a single document from a package.  won't work for many documents or many packages. Useful if you're working
    * with a workflow where you know there will only ever be 1 package with 1 document
    * @param workItem Work item with packages
    * @return The first object package found (non-deterministic)
    * @throws DfException
    */
   public IDfSysObject getObjectFromPackage(IDfWorkitem workItem) throws DfException {
      IDfCollection pkgColl = workItem.getAllPackages(null);
      try {
         if (pkgColl != null && pkgColl.next()) {
            return (IDfSysObject) this.session.getObject(pkgColl.getRepeatingId(FIELD_ATTACHMENT_OBJECT_IDS, 0));
         }
      } finally {
         if (pkgColl != null)
            pkgColl.close();
      }
      return null;
   }
   
   /**
    * Retrieve all package objects associated with a work item as a collection of IDfSysObjects
    * @param workItem Work item with packages
    * @return All objects associated with the work item as packages
    * @throws DfException
    */
   public List<? extends IDfSysObject> getAllPackageObjects(IDfWorkitem workItem) throws DfException  {
      if(workItem == null) { throw new RuntimeException("Work item cannot be null in call to getAllPackageObjects"); }
      log.debug("Retrieving package objects for work item {}", workItem.getObjectId());
      List<IDfSysObject> packageDocuments = new ArrayList<IDfSysObject>();
      IDfCollection pkgColl = workItem.getAllPackages(null);
      try {
         while(pkgColl != null && pkgColl.next()) {
            int docCount = pkgColl.getValueCount(FIELD_ATTACHMENT_OBJECT_IDS);
            for (int i=0; i < docCount; i++) {
               packageDocuments.add((IDfSysObject) this.session.getObject(pkgColl.getRepeatingId(FIELD_ATTACHMENT_OBJECT_IDS, i)));
            }
         }
      } finally {
         if (pkgColl != null)
            pkgColl.close();
      }
      
      return packageDocuments;
   }
   
   /**
    * Convenience method to retrieve the package objects as a virtual folder
    * @param workItem Work item with packages
    * @param byVersion If True, add documents by version (Chronicle ID), otherwise just add the document identifier directly
    * @return
    * @throws DfException
    */
   @SuppressWarnings("unchecked")
   public TrexCPVirtualFolder getAllPackageObjectsAsVirtualFolder(IDfWorkitem workItem, boolean byVersion) throws DfException  {
      if(workItem == null) { throw new RuntimeException("Work item cannot be null in call to getAllPackageObjectsAsVirtualFolder"); }
      log.debug("Retrieving package objects as virtual folder for work item {}", workItem.getObjectId());
      List<IDfDocument> packageDocuments = (List<IDfDocument>)this.getAllPackageObjects(workItem);
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      for(IDfDocument document : packageDocuments) {
         if(byVersion) {
            folder.addDocument(new VersionIdentifier(document));
         } else {
            folder.addDocument(new DocumentIdentifier(document));
         }
      }
      return folder;
   }
   
   /**
    * Retrieve all attachment objects associated with a work item as a collection of IDfSysObjects
    * @param workItem Work item with attachment
    * @return All objects associated with the work item as attachment
    * @throws DfException
    */
   public List<? extends IDfSysObject> getAllAttachmentObjects(IDfWorkitem workItem) throws DfException  {
      if(workItem == null) { throw new RuntimeException("Work item cannot be null in call to getAllAttachmentObjects"); }
      log.debug("Retrieving attachment objects for work item {}", workItem.getObjectId());
      List<IDfSysObject> attachmentDocuments = new ArrayList<IDfSysObject>();
      IDfCollection attachmentColl = workItem.getAttachments();
      try {
         while(attachmentColl != null && attachmentColl.next()) {
            IDfTypedObject obj = attachmentColl.getTypedObject();
            IDfId docId = obj.getId(FIELD_ATTACHMENT_OBJECT_IDS);
            attachmentDocuments.add((IDfSysObject) this.session.getObject(docId));
         }
      } finally {
         if (attachmentColl != null)
            attachmentColl.close();
      }
      
      return attachmentDocuments;
   }
   
   /**
    * Convenience method to retrieve the attachment objects as a virtual folder
    * @param workItem Work item with attachment
    * @param byVersion If True, add documents by version (Chronicle ID), otherwise just add the document identifier directly
    * @return
    * @throws DfException
    */
   @SuppressWarnings("unchecked")
   public TrexCPVirtualFolder getAllAttachmentObjectsAsVirtualFolder(IDfWorkitem workItem, boolean byVersion) throws DfException  {
      if(workItem == null) { throw new RuntimeException("Work item cannot be null in call to getAllAttachmentObjectsAsVirtualFolder"); }
      log.debug("Retrieving attachment objects as virtual folder for work item {}", workItem.getObjectId());
      List<IDfDocument> packageDocuments = (List<IDfDocument>)this.getAllAttachmentObjects(workItem);
      TrexCPVirtualFolder folder = new TrexCPVirtualFolder();
      for(IDfDocument document : packageDocuments) {
         if(byVersion) {
            folder.addDocument(new VersionIdentifier(document));
         } else {
            folder.addDocument(new DocumentIdentifier(document));
         }
      }
      return folder;
   }

   /**
    * Remove all attachments from a work item
    * @param workitem the work item to remove attachments from
    * @throws DfException
    */
   public void removeAllAttachments(IDfWorkitem workitem) throws DfException {
      if(workitem == null) { throw new RuntimeException("Work item cannot be null in call to removeAllAttachments"); }
      log.debug("Removing attachments from work item {}", workitem.getObjectId());
      IDfCollection attachmentColl = workitem.getAttachments();
      try {
         while(attachmentColl != null && attachmentColl.next()) {
            IDfTypedObject obj = attachmentColl.getTypedObject();
            IDfId attachmentId = obj.getId(FIELD_ATTACHMENT_ID);
            workitem.removeAttachment(attachmentId);
         }
      } finally {
         if (attachmentColl != null)
            attachmentColl.close();
      }
   }
   
}
