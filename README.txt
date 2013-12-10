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
    Handles management of internal and external datastores.
4. Workpool & ManualMode Manager
    Workpool service with a REST API that manages the lifecycle of conversion VMs
Manual mode (in manualmode/)
    REST service that, despite its name, does both automatic as well as manual
    captures.
5. Admin UI (webui)
    The main web interface to ThinApp Factory
6. ThinApp Store and client.
    Client application and browser plugin used to download and thinreg
    applications from the ThinApp Store
7. TAF Appliance and initial setup.

Are all components open sourced?
--------------------------------

The only part that is not currently open sourced is the ability to
automatically provision a Windows work VM given an ISO file. Additionally,
we require the 'vmrun' utility (part of the freeware VIX SDK) to interact
with worker VMs.

TAF Prerequisites
-----------------

On a Ubuntu system (we used 12.04 LTS), you need these prerequisites:

build-essential
libsqlite3-0
libcurl3
samba
samba-common-bin
cifs-utils
tomcat6
openjdk-6-jdk
postgresql-9.1
python-dev
python-migrate
python-psycopg2
python-sqlalchemy
python-setuptools
postgresql-server-dev-9.1
wine
zip
maven2
mingw-w64
mingw-w64-dev
mingw-w64-tools

Running TAF requires the VIX SDK to be installed. You can download VIX from:

https://my.vmware.com/group/vmware/details?productId=352&downloadGroup=VIXAPI112

You need a My VMware account to download and a EULA must be accepted. Once the
installer is copied to your server, simply run it with:

sh VMware-VIX... (whatever the filename is)

(Note that version 1.13.0 does not appear to work well with TAF.)

Building TAF
------------

Assuming you have all the prerequisites, go into the source tree, type make and
everything will be built for you. After editing setup.cfg, you should then be
able to type make setup. To refresh the Java webapps and Python eggs after the
initial deploy, run make deploy.

The setup process deletes Tomcat's ROOT webapp and replaces it with TAF.
It may be possible to run TAF in the future as a non-ROOT webapp, but we
haven't had time to try that out yet. Keep this in mind if you have something
important as a ROOT webapp.

Once you run the setup procedure, you should be able to hit Tomcat and get
a TAF login screen. The default admin password is blank.

After deploying you are responsible for staging a ThinApp runtime here. Here
are some tested versions we have used:

4.5.0-238809
4.6.0-287958
4.6.1-361923
4.6.2-467908
4.7.0-519532
4.7.1-677178
4.7.2-753608
4.7.3-891762

In the runtimes directory of your $install_dir (usually /var/lib/taf), create a
directory named as the above (thinapp version-build number.) It should have the
following files:

AppSync.exe
boot_loader.exe
Branding.dll
CustomMsi.dll
license.thinapp.4.0.e1.200805 <or similarly named license.* file>
os_exe.dat
QualityAgent.exe
QualityAgentPlugIn.dll
runtime_res.dll
sbmerge.exe
scripting.dll
snapshot.exe
snapshot.ini
template.msi
templates/
thinreg.exe
tlink.exe
vftool.exe
vregtool.exe
logging.dll
nt0_dll.dat
nt0_dll64.dat

Once done, restart tomcat, then you should be able to see the runtime appear in
TAF settings.

Creating a Workpool VM
----------------------

TODO

IDE Setup
---------
You can use Eclipse / SpringSource Tool Suite(STS) / IntelliJ Idea. I used STS
and here are some useful tips (The same can be used for Eclipse):

a. I was using STS Version: 2.6.1.RELEASE, (JUNO eclipse worked too).
    Cofigure eclipse: network, proxy, etc.
    Configure jvm params appropriately. (min-max: 0.25 - 1 GB)

b. (optional) Generate eclipse artifacts for TAF java projects:
    ...\appfactory> mvn eclipse:eclipse (generate project artifacts)
    ...\appfactory> mvn eclipse:clean   (clean project artifacts)

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

Known bugs
----------

1. The 'tasks' view does not work, but conversions are fine.

2. When a conversion fails and you end up restarting Tomcat to unwedge it,
   the job may still appear to be in 'running' state forever. All running
   jobs should be set to 'failed' after the webapp restarts.

3. Incremental builds are dangerously nonfunctional. If you modify Java code
   a clean build is suggested.

4. The 'ThinApp store' does not work because of a missing setup.exe which
   is difficult to build at this time in an OSS environment. It requires
   proper code signing for its browser integration. OSS users will more likely
   be better served by designing a new delivery mechanism.
