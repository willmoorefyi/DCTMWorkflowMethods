/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jun 20, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.common.DfException;
import com.triteksol.junit.DocumentumSetupTest;

/**
 * @author wmoore
 *
 */
public class RoleCreatorTest extends DocumentumSetupTest {
   private static final Logger log = LoggerFactory.getLogger(RoleCreatorTest.class);
   
   public static final String TEST_ROLE_NAME = "test-role";
   public static final String TEST_ROLE_PARENT = "test-role-parent";
   
   @Before public void setupTest() throws DfException {
      IDfGroup existingRole = RoleCreator.createRole(TEST_ROLE_PARENT, getUsername(), getSession());
      Assert.assertNotNull("Created group should not be null", existingRole);
      Assert.assertNotNull("Created group should have an ID", existingRole.getObjectId());
   }
   
   @After public void tearDownTest() throws DfException {
      IDfGroup existingRole = (IDfGroup) getSession().getObjectByQualification(String.format("dm_group where group_name = '%s'", TEST_ROLE_PARENT));
      if(existingRole != null) {
         existingRole.destroy();
      }
   }
   @Test public void test_createRole() throws DfException {
      log.info("Executing test to create a specific role, {}", TEST_ROLE_NAME);
      IDfGroup role = RoleCreator.createRole(TEST_ROLE_NAME, getUsername(), getSession());
      Assert.assertNotNull("Created group should not be null", role);
      Assert.assertNotNull("Created group should have an ID", role.getObjectId());
      role.destroy();

      log.info("Executing test to create a specific role, known to exist: {}", TEST_ROLE_PARENT);
      RoleCreator.createRole(TEST_ROLE_PARENT, getUsername(), getSession());
      //role should be created successfully
      log.info("Testing creating an existing role succeeded");
   }
   
   @Test public void test_retrieveRole() throws DfException {
      log.info("Testing retrieving existing role(s)");
      IDfGroup role;
      log.info("Retrieving role that does not exist");
      role = RoleCreator.getRole(TEST_ROLE_NAME, getSession());
      Assert.assertNull("Retrieval of role that doesn't exist should be null", role);
      
      role = RoleCreator.getRole(TEST_ROLE_PARENT, getSession());
      Assert.assertNotNull("Retrieval of existing group should not be null", role);
      Assert.assertNotNull("Retrieval of existing group should have an ID", role.getObjectId());
   }
   
   @Test public void test_endToend() throws DfException {
      log.info("Testing end-to-end for process");
      IDfGroup role = null;
      try {
         role = RoleCreator.createRole(TEST_ROLE_NAME, getUsername(), getSession());
         
         log.info("Role successfully created, adding to parent roles");
         RoleCreator.addRoleToGroups(TEST_ROLE_NAME, new String[] { TEST_ROLE_PARENT }, getSession());
         
         role.addUser("trexcp_admin");      
         
         Assert.assertNotNull("Query should return the parent role", getSession().getObjectByQualification(String.format("dm_group where group_name = '%s' and ANY groups_names = '%s'", TEST_ROLE_PARENT, TEST_ROLE_NAME)));
      }
      finally {
         if(role != null) {
            role.destroy();
         }
         log.info("Completed test");
      }
   }
}
