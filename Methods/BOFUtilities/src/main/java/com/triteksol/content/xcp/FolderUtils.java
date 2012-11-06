/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 6, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.content.xcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * @author wmoore
 * 
 */
public class FolderUtils {
   private static final Logger log = LoggerFactory.getLogger(FolderUtils.class);

   private IDfSession session;

   public FolderUtils(IDfSession session) {
      this.session = session;
   }
   
   /**
    * Retrieve a folder by location and name
    * @param folderLocation
    * @param folderName
    * @return
    * @throws DfException
    */
   public IDfFolder retrieveFolder(String folderLocation, String folderName) throws DfException {
      log.debug("Retrieving folder: '{}/{}'", folderLocation, folderName);
      IDfFolder folder = (IDfFolder)session.getObjectByQualification(String.format("dm_folder WHERE object_name = '%1$s' AND FOLDER('%2$s')", folderName, folderLocation));
      return folder;
   }

   /**
    * Create a folder in the given location.  Note that the folder location must already exist, and the folder Name must be new & unique
    * @param folderLocation The root location of the folder
    * @param folderName The name of the folder to create
    * @return
    * @throws DfException
    */
   public IDfFolder createFolderSafe(String folderLocation, String folderName) throws DfException {
      if(folderLocation == null) { throw new RuntimeException("Cannot call setFolder with a null folder location"); }
      if(folderName == null) { throw new RuntimeException("Cannot call setFolder with a null folder name"); }
      
      log.debug("Creating folder: '{}/{}'", folderLocation, folderName);
      IDfFolder folder = (IDfFolder)this.session.newObject("dm_folder");
      folder.setObjectName(folderName);
      folder.link(folderLocation);
      folder.save();
      folder.fetch(null);
      log.debug("Created folder with ID '{}' and path '{}'", folder.getObjectId(), folder.getFolderPath(0));
      return folder;
   }

   /**
    * Create a folder in the given location.  Note that the folder location must already exist.  This method is unsafe because if a folder already exists with the given path, we will return it
    * @param folderLocation The root location of the folder
    * @param folderName The name of the folder to create
    * @return
    * @throws DfException
    */
   public IDfFolder createFolderUnsafe(String folderLocation, String folderName) throws DfException {
      if(folderLocation == null) { throw new RuntimeException("Cannot call setFolder with a null folder location"); }
      if(folderName == null) { throw new RuntimeException("Cannot call setFolder with a null folder name"); }
      
      IDfFolder folder = retrieveFolder(folderLocation, folderName);
      log.debug("Creating folder: '{}/{}'", folderLocation, folderName);
      if(folder == null) {
         folder = (IDfFolder)this.session.newObject("dm_folder");
         folder.setObjectName(folderName);
         folder.link(folderLocation);
         folder.save();
         folder.fetch(null);
         log.debug("Created folder with ID '{}' and path '{}'", folder.getObjectId(), folder.getFolderPath(0));
      }
      else {
         log.debug("existing folder found, returning");
      }
      return folder;
   }
   
   /**
    * Destroy the passed-in folder. Same as {@link #destroyFolder(IDfFolder, boolean)} with false as the destroyContents parameter.
    * @param folder The folder to destroy
    * @throws DfException
    */
   public void destroyFolder(IDfFolder folder) throws DfException {
      destroyFolder(folder, false);
   }

   /**
    * Destroy the passed-in folder
    * 
    * @param folder The folder to destroy
    */
   public void destroyFolder(IDfFolder folder, boolean destroyContents) throws DfException {
      if (folder == null) {
         throw new RuntimeException("Cannot call destroy folder with a null folder object");
      }
      log.debug("Destroying the process folder \"{}\" with name \"{}\"", folder.getObjectId(), folder.getObjectName());
      if (destroyContents) {
         IDfCollection col = folder.getContents(null);
         while (col.next()) {
            String id = col.getString("r_object_id");
            IDfDocument obj = (IDfDocument) session.getObject(new DfId(id));
            // destroy the contained object
            log.debug("Destroying filed object \"{}\" with name \"{}\"", obj.getObjectId(), obj.getObjectName());
            obj.destroy();
         }
      }
      folder.destroy();
   }

}
