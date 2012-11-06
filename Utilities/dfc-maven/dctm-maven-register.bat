echo installing DFC maven dependencies

rem build dependencies for krbutil.jar
call mvn install:install-file -DgroupId=com.documentum -DartifactId=jcifs-krb5 -Dpackaging=jar -Dversion=1.3.1 -Dfile="c:/Program Files/Documentum/Shared/jcifs-krb5-1.3.1.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=krbutil -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/krbutil.jar" -DpomFile=krbutil-6.6.0.039.pom

rem build dependencies for dfc.jar
call mvn install:install-file -DgroupId=com.documentum -DartifactId=dfc -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/dfc.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=all-mb -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/All-MB.jar" -DgeneratePom=true
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId= -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/.jar" -DgeneratePom=true //activation.jar, 1.1, from Apache
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId= -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/.jar" -DgeneratePom=true //aspectjrt.jar, 1.5.2a, from Apache
call mvn install:install-file -DgroupId=com.rsa -DartifactId=certjfips -Dpackaging=jar -Dversion=2.2.0.0 -Dfile="c:/Program Files/Documentum/Shared/certjfips.jar" -DgeneratePom=true
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId= -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/.jar" -DgeneratePom=true //commons-codec, 1.3 apache
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId= -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/.jar" -DgeneratePom=true //commons-lang, 2.4 apache
call mvn install:install-file -DgroupId=com.documentum -DartifactId=configservice-api -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/configservice-api.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=configservice-impl -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/configservice-impl.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=dms-client-api -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/dms-client-api.jar" -DgeneratePom=true
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId= -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/.jar" -DgeneratePom=true //jaxb-api, 2.1, apache (javax.xml.jaxb-api)
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId= -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/.jar" -DgeneratePom=true //jaxb-impl, 2.1.4, apache (javax.xml.jaxb-impl)
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId=jcifs-krb5 -Dpackaging=jar -Dversion=1.3.1 -Dfile="c:/Program Files/Documentum/Shared/jcifs-krb5-1.3.1.jar" -DgeneratePom=true //jcifs-krb5 already registered
call mvn install:install-file -DgroupId=com.rsa -DartifactId=jsafefips -Dpackaging=jar -Dversion=3.6 -Dfile="c:/Program Files/Documentum/Shared/jsafeFIPS.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=jsr173_api -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/jsr173_api.jar" -DgeneratePom=true
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId=krbutil -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/krbutil.jar" -DpomFile=krbutil-6.6.0.039.pom //krbutil already registered
rem call mvn install:install-file -DgroupId=com.documentum -DartifactId= -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/.jar" -DgeneratePom=true //log4j, 1.2.13, apache
call mvn install:install-file -DgroupId=com.documentum -DartifactId=xtrim-api -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/xtrim-api.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=xtrim-server -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/Shared/xtrim-server.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=documentum-dfc -Dpackaging=pom -Dversion=6.6.0.039 -Dfile=documentum-dfc-6.6.0.039.pom

rem build dependencies for bpm-infra.jar
call mvn install:install-file -DgroupId=com.documentum -DartifactId=bpmutil -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/bpm/classes/lib/bpmutil.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=castor-xml -Dpackaging=jar -Dversion=1.1 -Dfile="c:/Program Files/Documentum/bpm/classes/lib/castor-1.1-xml.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=bpm_infra -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/bpm/classes/lib/bpm_infra.jar" -DpomFile=bpm_infra-6.6.0.039.pom
call mvn install:install-file -DgroupId=com.documentum -DartifactId=workflow -Dpackaging=jar -Dversion=6.6.0.039 -Dfile="c:/Program Files/Documentum/bpm/classes/lib/workflow.jar" -DgeneratePom=true
call mvn install:install-file -DgroupId=com.documentum -DartifactId=documentum-bpm -Dpackaging=pom -Dversion=6.6.0.039 -Dfile=documentum-bpm-6.6.0.039.pom

pause