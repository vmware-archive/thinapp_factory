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


"""Routes configuration

The more specific and detailed routes should be defined first so they
may take precedent over the more generic routes. For more information
refer to the routes manual at http://routes.groovie.org/docs/
"""
from routes import Mapper

def make_map(config):
   """Create, configure and return the routes Mapper"""
   map = Mapper(directory=config['pylons.paths']['controllers'],
                always_scan=config['debug'])
   map.minimization = False
   map.explicit = False

   # The ErrorController route (handles 404/500 error pages); it should
   # likely stay at the top, ensuring it can always be resolved
   map.connect('/error/{action}', controller='error')
   map.connect('/error/{action}/{id}', controller='error')

   with map.submapper(controller='projects') as m:
      ### Core project-related operations
      m.connect(r'/projects', action='create', conditions={'method': ['POST']})
      m.connect(r'/projects/{id:\d+}', action='update', conditions={'method': ['PUT']})
      m.connect(r'/projects/{id:\d+}', action='show', conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/icon', action='get_icon', conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/logs', action='logs', conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/refresh', action='refresh', conditions={'method': ['POST']})
      m.connect(r'/projects/{id:\d+}', action='delete', conditions={'method': ['DELETE']})
      m.connect(r'/projects/{id:\d+}/rebuild', action='rebuild', conditions={'method': ['POST']})

      ### Project directory operations
      m.connect(r'/projects/{id:\d+}/directory', action='show_directory', did= -1, conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/directory/{did:\d+}', action='show_directory', conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/directory/{did:\d+}', action='update_directory', conditions={'method': ['PUT']})
      m.connect(r'/projects/{id:\d+}/directory/{fid:\d+}', action='delete_file', conditions={'method': ['DELETE']}, directory=1)

      ### Project directory creation functions
      m.connect(r'/projects/{id:\d+}/directory/new', action='create_directory', conditions={'method': ['POST']})
      m.connect(r'/projects/{id:\d+}/directory/{did:\d+}/new_file', action='create_file', conditions={'method': ['POST']})

      ### Project (single) file operations
      m.connect(r'/projects/{id:\d+}/file/{fid:\d+}', action='show_file', conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/file/{fid:\d+}', action='update_file', conditions={'method': ['PUT']})
      m.connect(r'/projects/{id:\d+}/file/{fid:\d+}', action='delete_file', conditions={'method': ['DELETE']}, directory=0)

      ### Project package.ini operations
      m.connect(r'/projects/{id:\d+}/packageini', action='show_packageini', conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/packageini', action='update_packageini', conditions={'method': ['PUT']})

      ### Project registry operations
      m.connect(r'/projects/{id:\d+}/registry', action='show_registry', rid= -1, conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/registry/{rid:\d+}', action='show_registry', conditions={'method': ['GET']})
      m.connect(r'/projects/{id:\d+}/registry/{rid:\d+}', action='update_registry', conditions={'method': ['PUT']})
      m.connect(r'/projects/{id:\d+}/registry/{rid:\d+}', action='delete_registry', conditions={'method': ['DELETE']})
      m.connect(r'/projects/{id:\d+}/registry/new', action='create_registry', conditions={'method': ['POST']})

      ## Importing projects from a datastore
      ### Core project-related operations
      m.connect(r'/projects/import', action='import_projects', conditions={'method': ['POST']})

   with map.submapper(controller='storage') as m:
      m.connect(r'/storage', action='list', conditions={'method': ['GET']})
      m.connect(r'/storage', action='create', conditions={'method': ['POST']})
      m.connect(r'/storage/{id:\d+}', action='update', conditions={'method': ['PUT']})
      m.connect(r'/storage/{id:\d+}', action='show', conditions={'method': ['GET']})
      m.connect(r'/storage/{id:\d+}/online', action='online', conditions={'method': ['POST']})
      m.connect(r'/storage/{id:\d+}/offline', action='offline', conditions={'method': ['POST']})
      m.connect(r'/storage/{id:\d+}', action='delete', conditions={'method': ['DELETE']})
      m.connect(r'/storage/{id:\d+}/_acquire', action='acquire', conditions={'method': ['POST']})
      m.connect(r'/storage/{id:\d+}/_release/{releaseid:\d+}', action='release', conditions={'method': ['POST']})

   with map.submapper(controller='config') as m:
      m.connect(r'/config/reboot', action='reboot', conditions={'method': ['POST']})
      m.connect(r'/config/logs', action='logs', conditions={'method': ['GET']})
      m.connect(r'/config/timesync', action='getTimeSync', conditions={'method': ['GET']})
      m.connect(r'/config/timesync/enable', action='enableTimeSync', conditions={'method': ['POST']})
      m.connect(r'/config/timesync/disable', action='disableTimeSync', conditions={'method': ['POST']})
      m.connect(r'/config/info', action='getInfo', conditions={'method': ['GET']})
      m.connect(r'/config/resetpwd', action='clearAdminPwd', conditions={'method': ['PUT']})

   # Make things like GET /storage/ work.
   map.redirect('/*(url)/', '/{url}', _redirect_code='301 Moved Permanently')

   return map
