/* ***********************************************************************
 *
 * VMware ThinApp Factory
 *
 * Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ***********************************************************************/


What is VMware ThinApp Factory (TAF)?
-------------------------------------
TAF is a tool that simplifies generation of VMware ThinApps to an extent
that it can be configured to keep generating ThinApps by picking up
installers from multiple sources, apply recipes to customize ThinApp
creation and across different Operating Systems.


What constitutes VMware ThinApp Factory?
----------------------------------------
TAF comprises of multiple components and some of the functional units are:
1. ThinApp Coverter
    REST service that manages the lifecycle of ThinApp projects and their
    settings to modify, rebuild, etc. System configuration backend also
    lives here.
2. VMware Workpool manager (Legacy)
    Library to automatically install Windows templates from ISO, clone, and
    lease out VMs needed for conversion.
3. Datastore manager
    Handles management of internal and external datastores (Storage).
4. Newer workpool Manager (/workpool-java)
    Workpool service with a REST API that manages the lifecycle of conversion VMs
Manualmode (/manualmode)
    REST service that, despite its name, does both automatic as well as manual
    captures.
5. Admin UI (/webui)
    The main web interface to ThinApp Factory
6. ThinApp Store and client.
    Client application and browser plugin used to download and thinreg
    applications from the ThinApp Store
7. TAF Appliance and initial setup.



Are all components open sourced?
--------------------------------
No, but we want to, and are working towards it. However, we will expose
a significant chunk of the code that can be deployed over an existing
TAF appliance and call it Phase 1.

As part of TAF Open sourcing, Phase 1 will contain the following:
1. Workpool and manualmode manager (most of it.)
2. Admin UI
3. ThinApp Store


When will the rest come out?
----------------------------
We are currently working on it and there is no specific code drop date.


How do i build TAF?
-------------------
TAF code is written in various languages and each module can be built
separately and the following section outlines each logical group. The phase 1
components include scala/java projects.

Building scala/java modules
---------------------------

The following show the project hierarchy and the parent pom.xml

ThinApp Factory
   |--> pom.xml     (main pom.xml, that builds all)
   |--> settings.xml.in (necessary settings.xml configs)
   |--> workpool
        |--> pom.xml
        |--> workpool-web
                |--> pom.xml
   |--> manualmode
        |--> pom.xml
    |--> manualmode-web
                |--> pom.xml
   |--> client
        |--> pom.xml
   |--> webui
        |--> pom.xml


Build Setup
-----------
1. Download and install Oracle JDK 1.6.x

2. Download and install Apache Maven 2.2.1

3. Setup the classpath for JAVA_HOME, M2_HOME and update path accordingly.

4. Validate the above by running the following (windows cmd as below):
    C:\Users\keerthi> java -version
        java version "1.6.0_37"
        Java(TM) SE Runtime Environment (build 1.6.0_37-b06)
        ...

    C:\Users\keerthi> mvn -v
        Apache Maven 2.2.1 (r801777; 2009-08-06 12:16:01-0700)
        Java version: 1.6.0_26
        Java home: C:\Program Files\Java\jdk1.6.0_26\jre
        ...

5. Now that you have maven installed, you will have to ensure the necessary
remote repositories are set. The template settings.xml.in file is provided and
you can use this file, or copy the remote repositories needed for building TAF.

NOTE: settings.xml loc: M2_HOME/conf/settings.xml or $HOME/.m2/settings.xml)

6. Build TAF java projects:
    thinapp_factory> mvn clean install -Dmaven.test.skip=true

    NOTE: Skip tests and hence use args -Dmaven.test.skip=true
    If everything goes fine, you should see a “BUILD SUCCESSFUL” message at the end.

7. TAF needs 1 external java library that is not available on maven
repositories. Hence the following steps should be followed.

a. Download vijava from here:
    http://sourceforge.net/projects/vijava/files/vijava/VI%20Java%20API%202.1/

b.Deploy vijava to your local maven repository:
    thinapp_factory> mvn install:install-file -DgroupId=net.sourceforge.vijava -DartifactId=vijava -Dversion=2120100824 -Dpackaging=jar -Dfile=c:\vijava-2120100824.jar


IDE Setup
---------
You can use Eclipse / SpringSource Tool Suite(STS) / IntelliJ Idea. I used STS
and here are some useful tips (The same can be used for Eclipse):

a. I have used STS Version: 2.6.1.RELEASE, (and JUNO eclipse works too).
    Cofigure eclipse: network, proxy, etc.
    Configure jvm params appropriately. (min-max: 0.25 - 1 GB)

b. (optional) Generate eclipse artifacts for TAF java projects:
    thinapp_factory> mvn eclipse:eclipse (generate project artifacts)
    thinapp_factory> mvn eclipse:clean   (clean project artifacts)

c. You can use plugins for working on scala, velocity, import maven projects, etc.

Install Maven Integration for Eclipse - Helps while working with maven IDE tools.
NOTE: Do not use both m2eclipse and mvm eclipse:eclipse at the same time.
m2eclipse:
    http://download.eclipse.org/technology/m2e/releases

Install Scala IDE for Eclipse - Helps while working with Scala files.
    https://github.com/sonatype/m2eclipse-scala

Scala plugin:
    http://scala-ide.org/download/current.html

veloeclipse: Helps with velocity template editing.
    http://veloeclipse.googlecode.com/svn/trunk/update/

Spring IDE:
    http://springide.org/updatesite

d. Remove some warnings on eclipse by doing the following:
Under Preferences —> Maven —> Warnings, check 2 checkboxes for
    “GroupId is duplicate of parent grouped”,
    “Version is duplicate of parent version”


Import TAF maven projects into Eclipse
--------------------------------------
1. On eclipse, Select import projects, and under maven, select
“Maven generated projects”
2. Browse to the appfactory folder and select all projects.
3. Click submit to finish.
4. With the m2eclipse, the classpath will point to maven repo automatically.
4. However, if mvm eclipse:eclipse is used, the .classpath file contains
references to M2_REPO, and this will have to be set manually.
    See details here: http://maven.apache.org/guides/mini/guide-ide-eclipse.html
    Preferences —> Java —> Build Path —> Classpath Variables
    or
    mvn -Declipse.workspace=<path-to-eclipse-workspace> eclipse:add-maven-repo


Testing TAF webui
-----------------
If you plan on testing this locally, you will need the following installed:
1. Postgres SQL 9.1+
    Create AppFactory database.
    Add user with login credentials: postgres / postgres.
2. Tomcat 6.x


Test TAF webui in isolation
---------------------------
The webui java module can be tested without the entire thinapp factory
dependancies. The webui communicates with a backend service for datastores,
workpools, conversion, etc. This can be simulated so the UI part of it can work
in isolation. To do this, follow these steps:

1. Add appfactory\webui\src\resources\webui-local.properties
2. This file can be used to overwrite all the values in webui.properties
3. Overwrite the following entries:
   a. Hibernate/datasource settings - Point webui to a local postgres database:
        my.datasource.username=taf-user
        my.datasource.password=taf-password
        my.datasource.url=jdbc:postgresql://localhost/appfactory
        my.hibernate.hbm2ddl=update
   b. Set to debug mode:
        webui.devmode.enable=true
   c. Replace ThinApp factory service urls with local simulator url
        cws.conversions_service_url = http://localhost:8080/webui/cws
        cws.service_url = http://localhost:8080/webui/cws
        datastore.service_url = http://localhost:8080/webui/ds
        workpool.service_url = http://localhost:8080/mm/workpool
    d. You can find all configuration values from the source file:
        com.vmware.appfactory.config.ConfigRegistry.java


Test TAF webui pointing to an existing TAF Appliance
----------------------------------------------------
1. You'll need to enable to CWS to accept foreign connections. On the appliance, edit:
    /opt/appfactory/env/lib/python2.6/site-packages/converter-0.1dev-py2.6.egg/converter/appliance.ini
    and change host = 127.0.0.1 to host = 0.0.0.0.

    Then restart appfactory with:
    /etc/init.d/appfactory restart

2. You will also need to enable tomcat to accept foreign connections.
On the appliance, edit:

    /var/lib/tomcat6/conf/server.xml and replace listening address=127.0.0.1 to 0.0.0.0
    <Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000"
        URIEncoding="UTF-8" redirectPort="8443"
        address="0.0.0.0" />  <-- Desired.

    Now restart tomcat and workpools can be reached from outside the appliance.
    /etc/init.d/tomcat6 restart
    or
    service tomcat6 restart

3. On Admin UI go to the Settings page --> Configuration tab, and set
the service URLs to:
    Converter: http://<appliance_ip_address>:5000
    Conversions: http://<appliance_ip_address>:5000/mm
    Datastore: http://<appliance_ip_address>:5000
    Workpool: http://<appliance_ip_address>:8080/mm/workpool
NOTE: This step can be done on webui-local.properties as well.


Hacking TAF Appliance
---------------------

1. Accessing TAF console
        Login: user / user (Gain root access: sudo su - )
2. Access packages folder:
        Login: user / user (ThinApp-ed packages)
3. Postgres DB access info location:
        /home/user/appfactory.properties
4. Deployed VC and its configuration
        /home/user/workpool.ini
5. Web Server
        /etc/nginx
6. Tomcat webapps folder:
        /var/lib/tomcat6/webapps
7. Appliance version & build info:
        /var/lib/tomcat6/common/classes/appliance-version.properties
8. Admin UI & ThinApp store war:
        ROOT.war (Customer facing renamed from webui*.war)
9. Manualmode & workpool war:
        mm.war (API access, restricted by Nginx, renamed from manualmode*.war)
10. Log location
        /home/user/logs
        /home/user/nginx/
        /home/user/tomcat6/
11. Runtime location:
        /opt/appfactory/runtimes/
        NOTE: vmw.lic is needed, and can be replicated from other runtimes.
12. To change files in the appliance and have it persist across reboots
you have to put them under folder:
        /home/user/overlay
    Ex: If you wanted to add your own runtime you could add:
    /home/user/overlay/opt/appfactory/runtimes/4.9.0-1234. The files would then
    transparently show up in /opt/appfactory/runtimes/4.9.0-1234 and survive
    reboots.

Deploying into a TAF Appliance
------------------------------
Due to legal limitations, and the limited source code exposed as opensource,
we have to use a workaround for some non-java artifacts. There are a few *.exe
files that are built outside the java projects but deployed within the war. These
files are not opensourced and have to be extracted from the existing appliance.

Follow these steps to extract these files:
Option 1:
---------
1. Copy *.war from folder /var/lib/tomcat6/webapps
2. Extract the contents of these war files. [Ex: mm.war --> mm/]
3. Now, copy mm/WEB-INF/classes/*.exe, mm/WEB-INF/classes/*.vbs to the resource
    folder under: thinapp_factory/manualmode/src/main/resources
4. If you need the client installer that that can install and thinreg ThinApps
    on the client machine, then you need to copy the installer:
    ROOT/WEB-INF/classes/setup.exe to the resource folder under:
    thinapp_factory/webui/src/main/resources

Option 2:
---------
Instead of step #3, #4, you can copy those files mentioned above to the classpath loc:
    /var/lib/tomcat6/common/classes


SONAR for TAF
-------------
You can also hookup sonar to these java projects by using sonar-pom.xml.
> mvn sonar:sonar

NOTE: Some of this config needs work before we can get this up and running.


Proxy for tomcat setup
----------------------
If you use ninite.com feed or download from other external source, and you
have a proxy, ensure the following is set.
    -DproxySet=true -DproxyHost=proxy.your.copmany.com -DproxyPort=port
