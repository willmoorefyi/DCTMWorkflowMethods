Documentum Workflow Methods
===========================
This is a project that contains several Eclipse projects, all useful for building automated workflow processes using xCP.
The platform contains a rich set of functionality out of the box, but many common line-of-business functions are not represented 
in the default set of activities.  These actions, such as merging two separate workflows together or converting attachments 
from an application-specific format to a Documentum-generic format, are cumbersome to integrate as external methods.  A 
simpler approach is to build reusable BOF modules and create custom activity configurations for each. 

Usage
-----
Each folder in this project is an Eclipse library.  Simply import hte projects into your version of Eclipse.  Note that these
projects were all built using M2E, the Maven2Eclipse plugin.  You should convert these to maven projects after installation.

Every project can be built as a JAR file from the provided source. You can use Composer to install these JAR files as Jar 
definitions, then simply create a Module and Method for the Jar definition as you normally would. You can use the included 
'activity.xml' files in the resources folder to create a new custom activity for your method.

### Notes
* The projects were built using Eclipse Indigo (3.7)
* If you do not have access to the TriTek nexus repository (on Kansas.tritek.com), you may need to install the necessary 
Documentum dependencies into your local Nexus repository.  These were all built using `DFC 6.61 with an internal version 
number of `DFC 6.6.0.39`.  This has been tested on Documentum platforms 6.5 SP3 and 6.6 (patch versions unknown). You can
find the Maven registration script in the 'Utilities` folder
* Some of the activities depend on the definition of a global library to function correctly as workflow methods, which you
would also define using Composer
