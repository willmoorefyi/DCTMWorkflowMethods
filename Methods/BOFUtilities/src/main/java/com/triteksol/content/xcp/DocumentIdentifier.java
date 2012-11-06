/*
 * TreX 4.5
 * Copyright © 2010, TriTek Solutions, Inc.
 *
 * $Id: DocumentIdentifier.java 6697 2010-06-01 19:45:45Z tritek\jespinozasokal $
 */
package com.triteksol.content.xcp;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * Identifier for one specific version of a document.
 * 
 * @author Julio Espinoza-Sokal
 * @author TriTek Solutions, Inc.
 * 
 */
public class DocumentIdentifier extends DocumentumIdentifier {
   /**
    * The character prefix used to identify document identifiers.
    */
   public static final char PREFIX = 'D';

   /**
    * Instantiates a new instance of the VersionIdentifier class.
    * @param doc The object to construct a version identifier of
    */
   public DocumentIdentifier(IDfDocument doc) throws DfException {
      super(PREFIX, doc.getObjectId());
   }
   
   /**
    * Instantiates a new instance of the DocumentIdentifier class.
    * @param objectId The String serialized form of the ID.
    */
   public DocumentIdentifier(String objectId) {
      super(PREFIX, objectId);
   }

   /**
    * Instantiates a new instance of the DocumentIdentifier class.
    * @param id The IdfId object to wrap.
    */
   public DocumentIdentifier(IDfId id) {
      super(PREFIX, id);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.triteksol.content.xcp.DocumentumIdentifier#getQualification(java.lang.String)
    */
   @Override
   public String getQualification() {
      return String.format("dm_document (all) where r_object_id = '%s'", this.getObjectId().getId());
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.triteksol.content.xcp.DocumentumIdentifier#isCurrentVersionIdentifier()
    */
   @Override
   public boolean isCurrentVersionIdentifier() {
      return false;
   }
}
