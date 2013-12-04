# (c) 2010 VMware, Inc. All rights reserved.
# This file defines a helper that does not abstract pyVmomi, but provides
# helpers for long-winded workflows and common tasks.

# Since we use greenthreads directly here, best to do this early if necessary.
import eventlet
eventlet.monkey_patch()

import json
import logging
import socket
import os
import inject
import tempfile
import pkg_resources
import getpass
from datetime import datetime

from path import path

# We must import the subprocess from eventlet.green instead of the standard
# subprocess module so Popen works correctly with eventlet
from eventlet.green import subprocess

log = logging.getLogger(__name__)

# Following functions are in the top-level syscfg namespace as they are static

def reboot():
   """
      Reboot the machine, requires the process to have password-less sudo
      privileges.  Returns None on success and an error message string on
      failure.
   """

   try:
      # Execute the reboot command and wait for it to complete
      retval = subprocess.Popen(['sudo', 'reboot']).wait()
   except OSError as e:
      msg = "Unable to execute the 'sudo reboot' command: %s" % str(e)
      log.exception(msg)
      return msg

   if retval == 0:
      log.info('Restarting appliance...')
      return None
   else:
      # Running the command failed
      msg = 'Reboot command failed, exit code %d' % retval
      log.info(msg)
      return msg

def zipDirectory(pathToDir, dirName, zipName, excludePatterns=None, changeOwner=False):
   """
      Zips up the directory named dirName located at pathToDir and creates a new
      zip file named zipName stored at pathToDir.  Requires the process to have
      password-less sudo privileges.  Passes the given exclusion patterns to the
      zip command to exclude files from the resulting zip file.  Returns the
      path to the resulting zip file.
   """

   #Make excludePatterns an exmpty list if it is None
   excludePatterns = excludePatterns or []

   # Get a temporary ame for the zip file
   with tempfile.NamedTemporaryFile(dir=pathToDir, delete=False) as tmp:
      tmpName = '%s.zip' % tmp.name

   # Build a command that will create the zip file with the temp name
   args = ['sudo', 'zip', '-r', tmpName, dirName]
   for excludePattern in excludePatterns:
      args.extend(['-x', excludePattern])

   try:
      # Create the new zip file using the temp file name
      retval = subprocess.Popen(args,
                                cwd=pathToDir).wait()
   except OSError as e:
      msg = "Unable to execute the 'sudo zip' command: %s" % str(e)
      log.exception(msg)
      raise OSError(msg)

   if retval != 0:
      msg = 'Creating log zip file failed, exit code: %d' % retval
      log.info(msg)
      raise Exception(msg)

   # Calculate the full path to the zip file
   fullPath = path(pathToDir) / zipName

   try:
      # Move the file to the proper location/name
      retval = subprocess.Popen(['sudo', 'mv', tmpName,
                                 fullPath]).wait()
   except OSError as e:
      msg = "Unable to execute the 'sudo mv' command: %s" % str(e)
      log.exception(msg)
      raise OSError(msg)

   if retval != 0:
      msg = 'Moving the log zip file failed, exit code: %d' % retval
      log.info(msg)
      raise Exception(msg)

   log.info('Successfully created log zip file: %s', fullPath)

   return fullPath

@inject.param('config')
def applianceLogsFileName(config):
   """
      Returns the filename of the log zip file created by calling
      syscfg.zipApplianceLogs()
   """

   return config['converter.appliance_logs_name']

@inject.param('config')
def applianceLogsDirName(config):
   """
      Returns the name of the directory containing the appliance log files.
   """

   return config['converter.appliance_logs_dir']

@inject.param('config')
def applianceSystemStoragePath(config):
   """
      Returns the path to the internal appliance persistent storage.
   """

   return config['storage.system.path']

def zipApplianceLogs():
   """
      Zips up the system log files in /home/user/logs and stores the resulting
      appliance_logs.zip in /home/user.  Requires the process to have
      password-less sudo privileges.  Passes the given exclusion patterns to
      the zip command to exclude files from the resulting zip file.  Returns
      (filepath, None) on success and (None, error message) on failure.

      Note: Use syscfg.applianceLogsFileName() to obtain filename in addition
      to the full path that is returned by this function.
   """

   excludePatterns = ('*packager.ini', '*auth.log', '*auth.log.*', '*/nginx/*',)

   return zipDirectory(applianceSystemStoragePath(),
                       applianceLogsDirName(),
                       applianceLogsFileName(),
                       excludePatterns)

@inject.param('config')
def projectLogsFileName(id, config):
   """
      Returns the filename of the log zip file created by calling
      syscfg.projectLogs(id)
   """

   return config['converter.project_logs_name'] % id

@inject.param('config')
def projectLogsDirName(config):
   """
      Returns the name of the directory that contains the project logs
   """

   return config['converter.project_logs_dir']

def zipProjectLogs(projectPath, projectId):
   """
      Zips up the log files for the project with the given ID located at the
      given path.  Returns the full path to resulting zip file.
   """

   return zipDirectory(projectPath,
                       projectLogsDirName(),
                       projectLogsFileName(projectId),
                       changeOwner=True)

def getTimeSync():
   """
      Get the state of host/guest time synchronization using vmware-toolbox-cmd.
      Returns (True, None) if synchronization is enabled, (False, None) if it is not,
      and (None, errorMsg) if the function fails.
   """

   try:
      # Run the command to get the status of host/guest time sync
      p = subprocess.Popen(['vmware-toolbox-cmd', 'timesync', 'status'],
                           stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
      p.wait()
      retval = p.returncode
   except OSError as e:
      msg = "Unable to execute the 'vmware-toolbox-cmd' command: %s" % str(e)
      log.exception(msg)
      return (None, msg)

   if retval != 0:
      msg = 'Failed to get the status of the host/guest time synchronization,'\
            ' exit code: %d %s' % (retval, p.stderr.read().strip())
      log.info(msg)
      return (None, msg)
   else:
      out = p.stdout.read().strip()

      log.info('Host/guest time synchronization status: %s', out)

      if out == 'Enabled':
         return (True, None)
      elif out == 'Disabled':
         return (False, None)
      else:
         msg = 'Unknown host/guest status string returned: %s' % out
         log.info(msg)
         return (None, msg)

def setTimeSync(state):
   """
      Set the state of host/guest time synchronization using vmware-toolbox-cmd.
      Returns None on success and an error message on failure.
   """

   # Translate the boolean to a string for vmware-toolbox-cmd
   if state:
      setStr = 'enable'
   else:
      setStr = 'disable'

   try:
      # Run the command to set the status of host/guest time sync
      retval = subprocess.Popen(['vmware-toolbox-cmd', 'timesync',
                            setStr]).wait()
   except OSError as e:
      msg = "Unable to execute the 'vmware-toolbox-cmd' command: %s" % str(e)
      log.exception(msg)
      return msg

   if retval != 0:
      msg = 'Setting the state of host/guest time synchronization failed, '\
            'exit code: %d' % retval
      log.info(msg)
      return msg
   else:
      log.info("Successfully set status of host/guest time synchronization "\
               "to: '%s'", setStr)
      return None

def getInfo():
   """
      Gather various info about the appliance and return it in a dictionary:
      -'date': UTC date/time string in the format YYYY-MM-DD HH:MM (24 hour time)
      -'uptime': number of seconds the appliance has been running
   """

   # Grab the uptime in seconds from /proc/uptime
   try:
      uptimeSecs = int(float(path('/proc/uptime').bytes().strip().split()[0]))
   except IOError as e:
      uptimeSecs = None

   # Grab the system date/time
   timeStr = datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')
   return {'date': timeStr, 'uptime': uptimeSecs}
