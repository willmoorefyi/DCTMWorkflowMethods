/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 5, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.content.xcp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author wmoore
 *
 */
public class DocumentumContentTest {
   private static Logger log = LoggerFactory.getLogger(DocumentumContentTest.class);
   
   public final static String CONTENT_FILE_PROPERTY_NAME = "/dctm-document.properties";
   public final static String DOCUMENTTYPE_PROP = "com.triteksol.content.xcp.documenttype";
   public final static String DOCUMENTNAME_PROP = "com.triteksol.content.xcp.documentname";
   public final static String FILETYPE_PROP = "com.triteksol.content.xcp.filetype";
   public final static String CABINET_PROP = "com.triteksol.content.xcp.cabinet";
   
   private IDfSession session;
   
   private String documentType;
   private String documentName;

   private URL fileLoc;
   private String fileType;
   private String folderPath;
   
   private List<IDfDocument> documents;
   
   public DocumentumContentTest(IDfSession session) throws IOException {
      log.debug("Setting up DocumentumContentTest");
      InputStream is = DocumentumContentTest.class.getResourceAsStream( CONTENT_FILE_PROPERTY_NAME );
      if(is == null) {
         throw new RuntimeException(String.format("Documentum Content test properties file '%s' is required!", CONTENT_FILE_PROPERTY_NAME));
      }
      Properties props = new Properties();
      props.load(is);
      this.documentType = props.getProperty(DOCUMENTTYPE_PROP);
      this.documentName = props.getProperty(DOCUMENTNAME_PROP);
      this.fileType = props.getProperty(FILETYPE_PROP);
      this.folderPath = props.getProperty(CABINET_PROP);
      this.fileLoc = this.getClass().getResource(documentName);
      
      this.documents = new ArrayList<IDfDocument>();
      this.session = session;
      log.debug("DocumentumContentTest setup complete");
   }
   
   public IDfDocument createDocumentWithDefaults() throws DfException {
      log.debug("Creating document for documentum test with default values");
      return this.createDocument(null, null, null, null, null);
   }

   /**
    * Create a document using the passed-in parameters, or using the defaults from the propery file if the passed-in values are blank
    * @param documentType The document type to create, i.e. dm_document
    * @param fileType The Document "type" of the file.  This is similar to MIME type, but is a Documentum-specific string
    * @param documentName The name of the document in Documentum
    * @param fileLoc The location of the file on disk
    * @param folder The folder to file into on Documentum
    * @return
    * @throws DfException
    */
   public IDfDocument createDocument(String documentType, String documentName, String fileLoc, String fileType, String folder) throws DfException {
      log.info("Creating document for document test");
      //map local values to either input values or defaults
      String localDocumentType = documentType == null ? this.documentType : documentType;
      String localDocumentName = documentName == null ? this.documentName : documentName;
      String localFileLoc = fileLoc == null ? this.fileLoc.getPath() : fileLoc;
      String localFileType = fileType == null ? this.fileType : fileType;
      String localFolder = folder == null ? this.folderPath : folder;      
      log.debug("Creating Document of type {}, specified by file {} located on disk at {}, of type {}, and filing in cabinet {}", new String[] {localDocumentType, localDocumentName, localFileLoc, localFileType, localFolder});
      
      IDfDocument doc = null;
      doc = null;
      doc = (IDfDocument)this.session.newObject(localDocumentType);
      doc.setObjectName(localDocumentName);
      doc.setContentType(localFileType);
      doc.setFile(localFileLoc);
      doc.link(localFolder);
      doc.save();
      this.documents.add(doc);
      log.debug("Document creation complete");
      return doc;
   }
   
   /**
    * Destroy the documents created in this test
    */
   public void destroyDocuments() {
      log.debug("Destroying temporary document");
      if(this.documents != null) {
         for(IDfDocument doc : this.documents) {
            try {
               doc.destroy();
            }
            catch(DfException e) {
               log.warn("Unable to delete document, may have been deleted as part of test", e);
            }
         }
         log.debug("Documents successfully destroyed");
      }
   }
   
   
   List<IDfDocument> getDocuments() {
      return this.documents;
   }
}
