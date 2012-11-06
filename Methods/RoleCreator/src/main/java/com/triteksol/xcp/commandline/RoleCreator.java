/**
 * Copyright(c) 2008-2011, TriTek Solutions, Inc.
 * 
 * Created Jun 19, 2012
 * @author wmoore
 * 
 * $Id$
 */
package com.triteksol.xcp.commandline;

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.triteksol.xcp.commandline.base.DocumentumCommandLineBase;

/**
 * @author wmoore
 *
 */
public class RoleCreator extends DocumentumCommandLineBase {
   private static final Logger log = LoggerFactory.getLogger(RoleCreator.class);
   
   public static final String OPT_ROLE = "role";
   public static final String OPT_ROLE_SHORT = "r";
   public static final String OPT_ROLE_PARENT = "parent";
   public static final String OPT_ROLE_PARENT_SHORT = "a";
   
   /**
    * The main method - execute the program
    * @param args
    */
   public static void main(String[] args) {
      log.info("Starting the cleaner utility with arguments {}", new Object[] { args });
      try {
         RoleCreator runner = new RoleCreator();
         System.exit(runner.run(args) ? 1 : 0);
      } catch(RuntimeException ex) {
         log.error("A runtime exception occurred during the program's execution", ex);
         System.err.println("The application failed to execute successfully and returned the following exception: " + ex.getMessage());
         System.exit(1);
      }
   }

   /* (non-Javadoc)
    * @see com.triteksol.xcp.commandline.base.DocumentumCommandLineBase#addOptions(org.apache.commons.cli.Options)
    */
   @Override
   @SuppressWarnings("static-access")
   public Options addOptions(Options opts) {
      Option roles = OptionBuilder.withArgName(OPT_ROLE).hasArgs()
            .withDescription("The role to create in the system").withLongOpt(OPT_ROLE)
            .isRequired().create(OPT_ROLE_SHORT);

      Option parents = OptionBuilder.withArgName(OPT_ROLE_PARENT).hasArgs()
            .withDescription("The role to create in the system").withLongOpt(OPT_ROLE_PARENT)
            .withValueSeparator(',').isRequired().create(OPT_ROLE_PARENT_SHORT);
      
      opts.addOption(roles);
      opts.addOption(parents);
      
      return opts;
   }

   /* (non-Javadoc)
    * @see com.triteksol.xcp.commandline.base.DocumentumCommandLineBase#doWork(com.documentum.fc.client.IDfSession, org.apache.commons.cli.CommandLine)
    */
   @Override
   public boolean doWork(IDfSession session, CommandLine line) {
      String role = line.getOptionValue(OPT_ROLE);
      String[] parents = line.getOptionValues(OPT_ROLE_PARENT);
      String administrator = line.getOptionValue(OPT_USER);

      log.info("Executing the role creator utility");
      if(role != null) {
         log.info("Creating role specified in input parameter: {}", role.toString());
         return createRoleWithParent(role, parents, administrator, session);
      }
      
      return true;
   }

   /**
    * Create the specified role with the named parent
    * @param roleName
    * @param parents
    * @param administrator
    * @param session
    * @return
    */
   public boolean createRoleWithParent(String roleName, String[] parents, String administrator, IDfSession session) {
      log.info("Creating role {} with administrator {} and parents{}", new Object[] { roleName, parents, administrator });
      RoleCreator.createRole(roleName, administrator, session);
      
      log.info("Role successfully created, adding to parent roles");
      RoleCreator.addRoleToGroups(roleName, parents, session);
      
      return true;
   }
   
   /**
    * Create the role with the specified name
    * @param roleName
    * @param administrator
    * @param session
    * @return
    * @throws DfException
    */
   public static IDfGroup createRole(String roleName, String administrator, IDfSession session) {
      log.debug("Creating role {}", roleName);
      try {
         IDfGroup role = RoleCreator.getRole(roleName, session);
         if(role != null) {
            log.warn("Attempted to create role with name '{}', but a role with that name already exists", roleName);
            return role;
         }
         role = (IDfGroup) session.newObject("dm_group");
         role.setGroupName(roleName);
         role.setGroupClass("role");
         role.setDescription("Application Role");
         role.setGroupAdmin(administrator);
         role.addUser(administrator);
         role.save();
         log.debug("role created, ID: {}", role.getObjectId());
         return role;
      }
      catch (DfException e) {
         throw new RuntimeException(String.format("Exception occurred while trying to create the role specified by the name '%s'", roleName), e);
      }
   }
   
   /**
    * Get the role specified in the input parameters
    * @param roleName
    * @param session
    * @return
    */
   public static IDfGroup getRole(String roleName, IDfSession session) {
      log.debug("Retrieving role: {}", roleName);
      try {
         IDfGroup role = (IDfGroup)session.getObjectByQualification(String.format("dm_group where group_name = '%s'", roleName));
         log.debug("Retrieved role, ID: {}", role == null ? "<Role Does Not Exist>" : role.getObjectId());
         return role;
      }
      catch(DfException e) {
         throw new RuntimeException(String.format("Exception occurred trying to retrieve role with name '%s'", roleName), e);
      }
   }
   
   /**
    * Add role specified by name in input parameter to groups specified below
    * @param roleName
    * @param parents
    * @param session
    */
   public static void addRoleToGroups(String roleName, String[] parents, IDfSession session) {
      log.debug("Adding role '{}' to groups specified by '{}'", roleName, parents);
      try {
         for(String parent : parents) {
            log.debug("Adding role to parent '{}'", parent);
            //IDfGroup parentRole = getRole(parent, session);
            
            IDfQuery dql = new DfQuery();
            dql.setDQL(String.format("ALTER GROUP '%s' ADD '%s'", parent, roleName));
            dql.execute(session, IDfQuery.DF_QUERY);
            log.debug("Added role to parent");
         }
      }
      catch(DfException e) {
         throw new RuntimeException(String.format("Exception occurred while adding role specified by '%s' to parents in array '%s'", roleName, Arrays.toString(parents)), e);
      }
   }
}
