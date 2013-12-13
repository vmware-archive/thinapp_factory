#!/bin/sh

# VMware ThinApp Factory
# Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

. `readlink -f "$1"`

# Check all config strings
if [ "$i_didnt_read_to_the_end" = 1 ]; then
   echo "Please read and revise setup.cfg before running this script."
   exit 1
fi
if [ -z "$install_dir" ]; then
   echo "Missing value in setup.cfg: install_dir"
   exit 1
fi
if [ -z "$easy_install_dir" ]; then
   echo "Missing value in setup.cfg: easy_install_dir"
   exit 1
fi
if [ -z "$network_interface" ]; then
   echo "Missing value in setup.cfg: network_interface"
   exit 1
fi
if [ -z "$postgres_user" ]; then
   echo "Missing value in setup.cfg: postgres_user"
   exit 1
fi
if [ -z "$db_user" ]; then
   echo "Missing value in setup.cfg: db_user"
   exit 1
fi
if [ -z "$db_password" ]; then
   echo "Missing value in setup.cfg: db_password"
   exit 1
fi
if [ -z "$taf_user" ]; then
   echo "Missing value in setup.cfg: taf_user"
   exit 1
fi
if [ ! -d "$tomcat_root" ]; then
   echo "Missing or invalid value in setup.cfg: tomcat_root"
   exit 1
fi
if [ -z "$tomcat_user" ]; then
   echo "Missing value in setup.cfg: tomcat_user"
   exit 1
fi
if [ -z "$vc_host" ]; then
   echo "Missing value in setup.cfg: vc_host"
   exit 1
fi
if [ -z "$vc_user" ]; then
   echo "Missing value in setup.cfg: vc_user"
   exit 1
fi
if [ -z "$vc_password" ]; then
   echo "Missing value in setup.cfg: vc_password"
   exit 1
fi
if [ -z "$vc_ignoreCertificate" ]; then
   echo "Missing value in setup.cfg: vc_ignoreCertificate"
   exit 1
fi
if [ -z "$vc_datacenter" ]; then
   echo "Missing value in setup.cfg: vc_datacenter"
   exit 1
fi
if [ -z "$vc_computeResource" ]; then
   echo "Missing value in setup.cfg: vc_computeResource"
   exit 1
fi
if [ -z "$vc_resourcePool" ]; then
   echo "Missing value in setup.cfg: vc_resourcePool"
   exit 1
fi
if [ -z "$vc_datastore" ]; then
   echo "Missing value in setup.cfg: vc_datastore"
   exit 1
fi
if [ -z "$thinapp_user" ]; then
   echo "Missing value in setup.cfg: thinapp_user"
   exit 1
fi
if [ -z "$thinapp_license" ]; then
   echo "Missing value in setup.cfg: thinapp_license"
   exit 1
fi

if [ $(id -u) != 0 ]; then
   echo "should be run as root!" >&2
   exit 1
fi

echo "*** Adding user $taf_user"

groupadd $taf_user
useradd $taf_user -g $taf_user --system -m -d "$install_dir"
# Add the $tomcat_user user to the $taf_user group so it can write to the
# $taf_user staging dir
usermod -a -G $taf_user $tomcat_user

echo "*** Creating TAF workspace"

package_root="$install_dir"/packages

# Create empty runtimes root. The user is responsible for putting a runtime
# here.
mkdir -p "$install_dir/runtimes"

# Create exported samba directory.
install -o $taf_user -g $taf_user -d "$package_root"
chown $taf_user "$package_root"

# Create a directory to store installers downloaded from a feed.
mkdir -p "$package_root"/installers

# Create a directory to store installers uploaded from webui.
mkdir -p "$package_root"/upload

# Set group-level WRITE access to packages and all directories underneath
chmod -R 775 "$package_root"

# Change ownership to $tomcat_user so that Tomcat can write into /upload and
# /installers
chown -R $tomcat_user "$package_root"/*

# Create a persistent temp directory for CWS.
install -d "$install_dir"/temp -m 1777 -o root -g root

# Postgres MD5 hashes consist of MD5-hashed password concatenated
# with target username. Then when you call CREATE USER, you prepend
# md5 to it.
hashed=$(echo -n "${db_password}${db_user}" | md5sum | cut -d' ' -f1)

echo "*** Creating database"

# Create the database. SQLAlchemy can't handle this.
su "$postgres_user" -c psql >/dev/null << EOF
CREATE USER $db_user WITH ENCRYPTED PASSWORD 'md5$hashed';
CREATE DATABASE appfactory;
CREATE DATABASE converter WITH ENCODING 'UNICODE' TEMPLATE=template0;
CREATE DATABASE workpool;
GRANT ALL PRIVILEGES ON DATABASE appfactory TO $db_user;
GRANT ALL PRIVILEGES ON DATABASE converter TO $db_user;
GRANT ALL PRIVILEGES ON DATABASE workpool TO $db_user;
EOF

tomcat_classes=$tomcat_root/common/classes

cat > "$tomcat_classes"/appfactory.properties <<EOF
my.datasource.username = $db_user
my.datasource.password = $db_password
EOF

cat > "$tomcat_classes"/manualmode.properties <<EOF
vc.host=$vc_host
vc.username=$vc_user
vc.password=$vc_password
vc.ignoreCertificate=$vc_ignoreCertificate
thinapp.licenseUser=$thinapp_user
thinapp.licenseKey=$thinapp_license
runtimesPath=$install_dir/runtimes
EOF

cat > "$tomcat_classes"/workpool.properties <<EOF
iniPath=$install_dir/workpool.ini
clonevm=$easy_install_dir/clone-vm
EOF

if [ -d "$tomcat_root/webapps/ROOT" ]; then
   echo "*** Deleting default ROOT webapp"
   rm -rf "$tomcat_root/webapps/ROOT"
fi

sed -i 's/^JAVA_OPTS=".*"/JAVA_OPTS="-Djava.awt.headless=true -Xmx512m -XX:+UseConcMarkSweepGC"/g' /etc/default/tomcat6

echo "*** Configuring Samba"

marker="### ADDED BY TAF setup.sh ###"

if ! grep -q "$marker" /etc/samba/smb.conf; then
   cat >>/etc/samba/smb.conf <<EOF
$marker
[packages]
comment = Convert packages
path = $install_dir/packages
valid users = $taf_user
writable = yes
browseable = yes
$marker
EOF

   # XXX probably works only on ubuntu
   reload smbd
fi

# Add user for TAF use
echo -e "$db_password\n$db_password" | smbpasswd -s -a $taf_user

default_file=/etc/default/taf

echo "*** Writing $default_file"

cat >$default_file <<EOF
# Used by /etc/init.d/taf
install_dir="$install_dir"
taf_user="$taf_user"
db_username="$db_user"
db_password="$db_password"
network_interface="$network_interface"
easy_install_dir="$easy_install_dir"
EOF

echo "*** Writing workpool.ini"

cat >$install_dir/workpool.ini <<EOF
[VCEnvironment]
Datacenter = $vc_datacenter
ResourcePool = $vc_resourcePool
ComputeResource = $vc_computeResource
HostLoginPassword = $vc_password
VirtualMachineHost = $vc_host
HostLoginUserName = $vc_user
Datastore = $vc_datastore
EOF

echo "*** Installing init script"

cp init.sh /etc/init.d/taf
chmod 755 /etc/init.d/taf
update-rc.d taf defaults

echo "*** Populating database schema"

sqlalchemy_url="postgresql://${db_user}:${db_password}@localhost"
for db in appfactory converter workpool; do
   migrate_dir="migrate/$db"
   migrate version_control "${sqlalchemy_url}/$db" "$migrate_dir"
   migrate upgrade "${sqlalchemy_url}/$db" "$migrate_dir"
done

exit 0
