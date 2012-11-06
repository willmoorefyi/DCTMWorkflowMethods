/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Aug 5, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.bof;

import java.util.Collection;

/**
 * @author wmoore
 *
 */
public class Utils {
   
   /**
    * Join a collection of strings into a delimited string, so as not to depend on apache commons, which isn't present at runtime
    * @param col the collection to join
    * @param delimiter the glue character string to separate the values
    * @return a delimited string containing all the values from the collection with the passed-in delimiter
    */
   public static String joinCollection(Collection<? extends Object> col, String delimiter)
   {
      if(col == null || col.isEmpty()) {
         return null;
      }
      StringBuilder out=new StringBuilder();
      boolean firstVal = true;
      for(Object s : col) {
         if(!firstVal) {
            out.append(delimiter);
         }
         out.append(s == null ? null : s.toString());
         firstVal = false;
      }
     return out.toString();
   }
}
