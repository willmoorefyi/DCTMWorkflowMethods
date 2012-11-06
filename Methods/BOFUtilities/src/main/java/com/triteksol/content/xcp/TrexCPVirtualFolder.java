/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 4, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.content.xcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.triteksol.bof.Utils;

/**
 * Helper class to handle virtual folders, how TrexCP stores documents attached to a workflow.
 * Note this class is not the same as DocumentumVirtualFolder.
 * 
 * @author wmoore
 *
 */
public class TrexCPVirtualFolder {
   private static final Logger logger = LoggerFactory.getLogger(TrexCPVirtualFolder.class);
   
   static final String SEPARATOR_CHAR = ";";
   
   private final List<DocumentumIdentifier> documents;
   
   public TrexCPVirtualFolder() {
      this((String)null);
   }
   
   /**
    * Create a new DocumentumVirtual Folder
    * @param folderID
    */
   public TrexCPVirtualFolder(final List<DocumentumIdentifier> documents) {
      logger.debug("Retrieving virtual folder {}", documents);

      if (documents == null || documents.isEmpty()) {
         // If there are no doc id's, then this folder is empty
         this.documents = new ArrayList<DocumentumIdentifier>();
      }
      else {
         this.documents = new ArrayList<DocumentumIdentifier>();
         this.documents.addAll(documents);
      }
   }
   
   /**
    * Create a new DocumentumVirtual Folder
    * @param folderID
    */
   public TrexCPVirtualFolder(final String folderID) {
      logger.debug("Retrieving virtual folder {}", folderID);

      if (folderID == null || folderID.length() == 0) {
         // If there are no doc id's, then this folder is empty
         this.documents = new ArrayList<DocumentumIdentifier>();
      }
      else {
         final String[] asDocIds = folderID.split(";");
         final int iSize = asDocIds.length;
   
         this.documents = new ArrayList<DocumentumIdentifier>(iSize);
         for(String currentDocId : asDocIds) {
            logger.debug(String.format("Processing doc id %s", currentDocId));
            DocumentumIdentifier oDocId;
            oDocId = createContentIdentifier(currentDocId);
            //prefill with empty documents
            this.documents.add(oDocId);
         }
      }
   }
   
   /**
    * Create a document identifier, to use in the folder
    * @param oid
    * @return
    */
   protected DocumentumIdentifier createContentIdentifier(String oid) {
      if (oid == null || oid.length() == 0) {
         throw new RuntimeException("A blank string can not be a content identifier.");
      }

      final char prefix = oid.charAt(0);
      if (prefix == DocumentIdentifier.PREFIX) {
         return new DocumentIdentifier(oid);
      } else if (prefix == VersionIdentifier.PREFIX) {
         return new VersionIdentifier(oid);
      } else {
         throw new RuntimeException(String.format("Content identifier prefix is of an unsupported type: %s", prefix));
      }
   }
   
   /**
    * Add a document to the folder.  If the document is already contained in the folder, return false
    * @param id   The document or chronicle ID to add to the folder
    * @param byVersion True to add the document by version identifier, false to add this specific version only.
    * @return
    */
   public boolean addDocument(IDfDocument document, boolean byVersion) throws DfException {
      IDfId id = byVersion ? document.getChronicleId() : document.getObjectId();
      logger.debug("Adding document with ID '{}' to folder", id);
      return addDocument(id.toString(), byVersion);
   }

   /**
    * Add a document to the folder.  If the document is already contained in the folder, return false
    * @param id   The document or chronicle ID to add to the folder
    * @param byVersion True to add the document by version identifier, false to add this specific version only.  Note that if you specify byVersion, the passed-in ID must be the chronicle ID.
    *    If you specify false, the passed-in ID must be the document identifier
    * @return
    */
   public boolean addDocument(String id, boolean byVersion) {
      DocumentumIdentifier docId = byVersion ? new VersionIdentifier(id) : new DocumentIdentifier(id);
      return addDocument(docId);
   }

   /**
    * Add a document to the folder.  If the document is already contained in the folder, return false
    * @param id   The DocumentumIdentifier that specifies the document to add
    * @return
    */
   public boolean addDocument(DocumentumIdentifier id) {
      if(documents.contains(id)) {
         logger.debug("Folder already contains document identifier {}", id);
         return false;
      }
      else {
         logger.debug("Adding document identifier {}", id);
         documents.add(id);
         return true;
      }
   }
   
   /**
    * Returns True if the folder contains this document, False if it does not
    * @param id The DocumentumIdentifier to verify
    * @return true if the folder contains the document, false otherwise
    */
   public boolean containsDocument(DocumentumIdentifier id) {
     return documents.contains(id);
   }
  
   /**
    * Returns True if the folder contains this document, False if it does not.  Will attempt to find the document both by ID and by version
    * @param doc The document to verify
    * @return true if the folder contains the document, false otherwise
    */
   public boolean containsDocument(IDfDocument doc) throws DfException {
      return documents.contains(new DocumentIdentifier(doc)) || documents.contains(new VersionIdentifier(doc));
   }
   
   /**
    * Remove the document identifier specified from the folder.
    * @param id The documentum object to remove
    * @return True if the folder contained the document and removed it, false otherwise
    */
   public boolean removeDocument(DocumentumIdentifier id) {
      return this.documents.remove(id);
   }
   
   /**
    * Remove the document specified from the folder.  Will attempt to remove the document both by ID or by version
    * @param doc The document to remove
    * @return False if a document was found and removed, false otherwise
    * @throws DfException
    */
   public boolean removeDocument(IDfDocument doc) throws DfException {
      if(doc == null) { throw new RuntimeException("Document is required when calling remove Document from virtual folder"); }
      logger.debug("Removing document '{}' from virtual folder", doc.getObjectId());
      return this.documents.remove(new DocumentIdentifier(doc)) || this.documents.remove(new VersionIdentifier(doc));
   }
   
   /**
    * Retrieve all the documents from the virtual folder
    * @param session The session used to retrieve the folder documents
    * @return  A list of all documents in the folder
    * @throws DfException
    */
   public List<IDfDocument> getAllDocuments(IDfSession session) throws DfException {
      List<IDfDocument> returnVal = new ArrayList<IDfDocument>();
      logger.debug("Retrieving all documents from the virtual folder");
      for(DocumentumIdentifier ident : this.documents) {
         returnVal.add((IDfDocument)session.getObjectByQualification(ident.getQualification()));
      }
      return returnVal;
   }
   
   /**
    * Return a read-only list of all the document identifiers fro this virtual folder
    * @return A read-only list of all {@link DocumentIdentifier} objects in this virtual folder
    * @throws DfException
    */
   public List<DocumentumIdentifier> getAllDocumentumIdentifiers() throws DfException {
      logger.debug("Retrieving all document identifiers from the virtual folder");
      return Collections.unmodifiableList(this.documents);
   }
   
   /**
    * Return the complete folder ID
    * @return
    */
   public String getFolderID() {
      return Utils.joinCollection(this.documents, SEPARATOR_CHAR);
   }
}
