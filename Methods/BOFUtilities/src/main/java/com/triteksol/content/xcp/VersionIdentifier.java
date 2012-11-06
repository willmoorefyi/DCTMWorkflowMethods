/*
 * TreX 4.5
 * Copyright © 2010, TriTek Solutions, Inc.
 *
 * $Id: VersionIdentifier.java 6697 2010-06-01 19:45:45Z tritek\jespinozasokal $
 */
package com.triteksol.content.xcp;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * 
 * @author Julio Espinoza-Sokal
 * @author TriTek Solutions, Inc.
 * 
 */
public class VersionIdentifier extends DocumentumIdentifier {
   /**
    * The character prefix used to identify current version identifiers.
    */
   public static final char PREFIX = 'V';

   /**
    * Instantiates a new instance of the VersionIdentifier class.
    * @param doc The object to construct a version identifier of
    */
   public VersionIdentifier(IDfDocument doc) throws DfException {
      super(PREFIX, doc.getChronicleId());
   }
   
   /**
    * Instantiates a new instance of the VersionIdentifier class.
    * @param objectId The String serialized form of the ID.
    */
   public VersionIdentifier(String objectId) {
      super(PREFIX, objectId);
   }

   /**
    * Instantiates a new instance of the VersionIdentifier class.
    * @param id The IdfId object to wrap.
    */
   public VersionIdentifier(IDfId id) {
      super(PREFIX, id);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.triteksol.content.xcp.DocumentumIdentifier#getQualification(java.lang.String)
    */
   @Override
   public String getQualification() {
      return String.format("dm_document where i_chronicle_id = '%s' and any r_version_label in ('CURRENT')", this
         .getObjectId().getId());
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.triteksol.content.xcp.DocumentumIdentifier#isCurrentVersionIdentifier()
    */
   @Override
   public boolean isCurrentVersionIdentifier() {
      return true;
   }
}
