/*
 * TreX 4.5
 * Copyright Â© 2010, TriTek Solutions, Inc.
 *
 * $Id: DocumentumIdentifier.java 6697 2010-06-01 19:45:45Z tritek\jespinozasokal $
 */
package com.triteksol.content.xcp;

import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

/**
 * 
 * @author Julio Espinoza-Sokal
 * @author TriTek Solutions, Inc.
 * 
 * Notes about Documentum object IDs: Content server generates this ID when you create an object of any type. IDs are 16
 * characters long and alphanumeric. The characters from position 01 to 02 indicate the object type tag (09 = document,
 * 0b = folder, etc.). The characters from position 03 to 08 are the Repository id (Same for every object in a
 * Repository, but different for each Repository) The characters from position 09 -to 16 is unique identifier for this
 * object.
 */
public abstract class DocumentumIdentifier {
   private final char  prefix;
   private final IDfId objectId;

   /**
    * Instantiates a new instance of the DocumentumIdentifier class. Child classes should call with the appropriate parameter values
    * @param prefix The character prefix used by this class of Identifiers.
    * @param objectId The String representation of the object id.
    */
   protected DocumentumIdentifier(final char prefix, final String objectId) {
      assert objectId!=null && objectId.length()>0 : "A blank string can not be a content identifier.";

      this.prefix = prefix;

      if (objectId.charAt(0) == this.prefix) {
         this.objectId = new DfId(objectId.substring(1));
      }
      else {
         // all IDs should start with a digit unless it has a TreX prefix.
         if (!Character.isDigit(objectId.charAt(0))) {
            throw new RuntimeException(String.format("The provided prefix '%s' and object id '%s' conflict.", prefix,
               objectId));
         }
         this.objectId = new DfId(objectId);
      }
   }

   /**
    * Instantiates a new instance of the DocumentumIdentifier class.
    * @param prefix The character prefix used by this class of Identifiers.
    * @param id The IDfId object to be wrapped by this ContentIdentifier instance.
    */
  protected DocumentumIdentifier(char prefix, IDfId id) {
      assert id != null : "Id is required.";

      this.prefix = prefix;
      this.objectId = id;
   }

   /**
    * Generate the qualification used in queries for the identified object.
    * @return The Qualification string to retrieve the requested document.
    */
   public abstract String getQualification();

   /**
    * Get the actual object ID.
    * @return The IDfId object wrapped by this object.
    */
   public IDfId getObjectId() {
      return this.objectId;
   }

   /**
    * Is this identifier for the latest version or a specific version of a document.
    * @return true if this identifier always refers to the latest version.
    */
   public abstract boolean isCurrentVersionIdentifier();

   /*
    * (non-Javadoc)
    * 
    * @see com.triteksol.content.ContentIdentifier#toString()
    */
   @Override
   public String toString() {
      return prefix + this.objectId.getId();
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;

      if (obj == null)
         return false;

      if (getClass() != obj.getClass())
         return false;

      final DocumentumIdentifier other = (DocumentumIdentifier) obj;
      if (this.prefix != other.prefix) {
         return false;
      }

      if (!this.objectId.equals(other.objectId)) {
         return false;
      }

      return true;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return this.prefix ^ this.objectId.hashCode();
   }

}
