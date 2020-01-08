# What is VMware ThinApp Factory (TAF)?
# VMware has ended active development of this project, this repository will no longer be updated.

TAF is a tool that simplifies generation of VMware ThinApps to an extent
that it can be configured to keep generating ThinApps by picking up
installers from multiple sources, apply recipes to customize ThinApp
creation and across different Operating Systems.

# What's in ThinApp Factory?

TAF comprises of multiple components and some of the functional units are:

1. ThinApp Converter: REST service that manages the lifecycle of ThinApp
   projects and their settings to modify, rebuild, etc. System configuration
   backend also lives here.
2. VMware Workpool manager (Legacy)
   Library to automatically install Windows templates from ISO, clone, and
   lease out VMs needed for conversion.
3. Datastore manager
   Handles management of internal and external datastores.
4. Workpool & ManualMode Manager
   Workpool service with a REST API that manages the lifecycle of conversion VMs
5. Manual mode (in manualmode/)
   REST service that, despite its name, does both automatic as well as manual
   captures.
6. Admin UI (webui)
   The main web interface to ThinApp Factory
7. ThinApp Store and client.
   Client application and browser plugin used to download and thinreg
   applications from the ThinApp Store
8. TAF Appliance and initial setup.

# Build TAF

## TAF Prerequisites

On a Ubuntu system (we used 12.04 LTS), you need these prerequisites:

	build-essential
	libsqlite3-0
	curl
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

Running TAF requires the VIX SDK to be installed. You can download VIX from [here](https://my.vmware.com/group/vmware/details?productId=352&downloadGroup=VIXAPI112).

You need a **My VMware** account to download and a EULA must be accepted. Once the installer is copied to your server, simply run it with:

	sh VMware-VIX-1.12.0-812388.x86_64.bundle

(Note two things: your browser may download it as a text file, and that's OK -- you can still run sh on it. Second, VIX version 1.13.0 does not appear to work well with TAF.)

## Building TAF

Assuming you have all the prerequisites, go into the source tree, type make and everything will be built for you. After editing `setup.cfg`, you should then be able to `make setup` as root. To refresh the Java webapps and Python eggs after the initial deploy, run `make deploy` (also as root.)

The setup process deletes Tomcat's ROOT webapp and replaces it with TAF. It may be possible to run TAF in the future as a non-ROOT webapp, but we haven't had time to try that out yet. Keep this in mind if you have something important as a ROOT webapp.

Once you run the setup procedure, you should be able to hit Tomcat and get a TAF login screen. The default admin password is blank.

After deploying you are responsible for staging a ThinApp runtime here. Here
are some tested versions we have used:

* 4.5.0-238809
* 4.6.0-287958
* 4.6.1-361923
* 4.6.2-467908
* 4.7.0-519532
* 4.7.1-677178
* 4.7.2-753608
* 4.7.3-891762

In the runtimes directory of your $install_dir (usually /var/lib/taf), create a
directory named as the above (thinapp version-build number.) It should have the
following files:

	AppSync.exe
	boot_loader.exe
	Branding.dll
	CustomMsi.dll
	license.thinapp.4.0.e1.200805
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

Once done, restart tomcat, then you should be able to see the runtime appear in TAF settings.

## Known bugs

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

## Is opensourcing complete?

The only part that is not currently open sourced is the ability to
automatically provision a Windows work VM given an ISO file. Additionally,
we require the 'vmrun' utility (part of the freeware VIX SDK) to interact
with worker VMs.

# Developer tips

## IDE Setup

You can use Eclipse / SpringSource Tool Suite(STS) / IntelliJ Idea. I used STS and here are some useful tips (The same can be used for Eclipse):

* I was using STS Version: 2.6.1.RELEASE, (JUNO eclipse worked too).
Configure eclipse: network, proxy, etc.
Configure jvm params appropriately. (min-max: 0.25 - 1 GB)

* (optional) Generate eclipse artifacts for TAF java projects:
	`mvn eclipse:eclipse`, clean with `mvn eclipse:clean`

* Plugins for working on scala, velocity, import maven projects, etc.
	* [m2eclipse](http://download.eclipse.org/technology/m2e/releases)
	* [Scala IDE](http://scala-ide.org/download/current.html)
	* [Scala IDE integration for M2Eclipse](https://github.com/sonatype/m2eclipse-scala)
	* [veloeclipse](http://veloeclipse.googlecode.com/svn/trunk/update/)
	* [Spring IDE](http://springide.org/updatesite)
* Remove some warnings on Eclipse by doing the following:
Under *Preferences > Maven > Warnings*, check 2 checkboxes for
    * "GroupId is duplicate of parent group",
    * "Version is duplicate of parent version"

## Import TAF maven projects into Eclipse

1. In Eclipse, select "Import projects", and under maven, select "Maven generated projects"
2. Browse to the thinapp_factory folder and select all projects.
3. Click submit to finish.
4. With `m2eclipse`, the classpath will point to the maven repo automatically.
5. However, if `mvn eclipse:eclipse` is used, the `.classpath` file contains references to `M2_REPO`, and this will have to be set manually. See details [here](http://maven.apache.org/guides/mini/guide-ide-eclipse.html). Edit this in *Preferences > Java > Build Path > Classpath Variables* or: `mvn -Declipse.workspace=<path-to-eclipse-workspace> eclipse:add-maven-repo`

## Test TAF WebUI in isolation

The webui Java module can be tested without the entire thinapp factory dependencies. The webui communicates with a backend service for datastores, workpools, conversion, etc. This can be simulated so the UI part of it can work
in isolation. To do this, follow these steps:

1. Add `appfactory\webui\src\resources\webui-local.properties`. This file can be used to overwrite all the values in webui.properties.
2. Overwrite the following entries:
    * **Hibernate/datasource settings** - Point webui to a local Postgres database:

            my.datasource.username=taf-user
            my.datasource.password=taf-password
            my.datasource.url=jdbc:postgresql://localhost/appfactory
            my.hibernate.hbm2ddl=update
   * Set to debug mode:

            webui.devmode.enable=true
   * Replace ThinApp factory service urls with local simulator url

	        cws.conversions_service_url = http://localhost:8080/webui/cws
	        cws.service_url = http://localhost:8080/webui/cws
	        datastore.service_url = http://localhost:8080/webui/ds
	        workpool.service_url = http://localhost:8080/mm/workpool

    * You can find all configuration values from the source file: `com/vmware/appfactory/config/ConfigRegistry.java`


## Test TAF WebUI with existing instance

1. You'll need to enable to CWS to accept foreign connections. Edit `appliance.ini` in the converter source and change `host = 127.0.0.1` to `host = 0.0.0.0`. Rebuild the egg and deploy using `make deploy`.

2. On your local Admin UI, go to the *Settings > Configuration tab*, and set
the service URLs to:
	* Converter: `http://<appliance_ip_address>:5000`
	* Conversions: `http://<appliance_ip_address>:5000/mm`
	* Datastore: `http://<appliance_ip_address>:5000`
	* Workpool: `http://<appliance_ip_address>:8080/mm/workpool`

NOTE: This step can be done on webui-local.properties as well.

## SONAR for TAF

You can also hookup sonar to these java projects by using sonar-pom.xml. Just run `mvn sonar:sonar`

NOTE: Some of this config needs work before we can get this up and running.


## Proxy for tomcat setup

If you use ninite.com feed or download from other external source, and you
have a proxy, ensure the following properties are set.

	proxySet=true
	proxyHost=proxy.your.company.com
	proxyPort=port

A TODO is to make this settable in setup.cfg.
