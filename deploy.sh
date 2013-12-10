#!/bin/sh
# Helper script that deploys the updated webapps to $tomcat_root and runs
# easy_install on all the generated egg files.

set -e

if [ $(id -u) != 0 ]; then
   echo "should be run as root!" >&2
   exit 1
fi

. `readlink -f "$1"`

if [ ! -d "$tomcat_root/webapps" ]; then
   echo "$tomcat_root does not have a webapps dir. Highly unusual. Exiting"
   exit 1
fi

# The order matters here as converter depends on syscfg and cifsmount
easy_install build/dist/syscfg-*.egg
easy_install build/dist/cifsmount-*.egg
easy_install build/dist/afdeploy-*.egg
easy_install build/dist/converter-*.egg

cp ./webui/target/webui-*.war "$tomcat_root/webapps/ROOT.war"
cp ./manualmode/web/target/manualmode-web-*.war "$tomcat_root/webapps/mm.war"

/etc/init.d/taf restart
/etc/init.d/tomcat6 restart
