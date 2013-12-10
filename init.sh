#!/bin/bash

### BEGIN INIT INFO
# Provides:       taf
# Required-Start: $local_fs postgresql
# Required-Stop:  $local_fs postgresql
# Default-Start:  2 3 4 5
# Default-Stop:   0 1 6
# X-Start-Before: tomcat6
### END INIT INFO

set -e

. /lib/lsb/init-functions

# Locate paster file to use.
config=`python -c 'import pkg_resources; print pkg_resources.resource_filename("converter", "appliance.ini")'`

# XXX: add retry for dhcp b/c it fails with "socket.gaierror -3
# temporary failure in name resolution"?
options="hostname=`python -c \"import socket; print socket.getfqdn()\"`"

# Get all deployment settings
. /etc/default/taf

pid_file=$install_dir/taf.pid

start()
{
   sqlalchemy_url="postgresql://$db_username:$db_password@localhost"
   options="$options sqlalchemy.url=${sqlalchemy_url}/converter storage.system.path=$install_dir smb.user=$taf_user smb.password=$db_password"

   # Determine current IP of $network_interface to build certain CWS URLs
   ip=$(ifconfig "$network_interface" | grep 'inet addr' | cut -d: -f2 | cut -d' ' -f1)
   options="$options ip=$ip"
   options="$options netbios.name=$ip"

   if [ -r /etc/default/locale ]; then
      . /etc/default/locale
      export LANG
   fi

   spawning                             \
      --chuid "$taf_user":$(id -gn "$taf_user")         \
      --processes 1                                     \
      --threads 0                                       \
      --factory spawning.paste_factory.config_factory   \
      --daemonize                                       \
      --pidfile="$pid_file"                             \
      --stdout=/var/log/converter.stdout                \
      --stderr=/var/log/converter.stderr                \
      "$config" $options
}

stop()
{
   if [ -f "$pid_file" ]; then
      kill $(cat ${pid_file})
   fi
}

case "$1" in
   start)
      start
      ;;
   stop)
      stop
      ;;
   restart)
      stop || true
      start
      ;;
   *)
      log_success_msg "Usage: $0 {start|stop|restart}"
      exit 1
      ;;
esac
