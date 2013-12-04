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


"""The base Controller API

Provides the BaseController class for subclassing.
"""
import httplib
import inject
import json
import logging
import os

from pylons import config, request, response
from pylons.controllers import WSGIController
from pylons.controllers.util import redirect, abort
from pylons.templating import render_mako as render
from paste.httpexceptions import HTTPError
from paste.fileapp import FileApp

# Not using functools.wraps because whatever it does makes Pylons
# unable to map actions to their functions.
from decorator import decorator

def ExceptionToCode(excType, code):
   def ExceptionToCode(f, *args, **kwargs):
      try:
         f(*args, **kwargs)
      except excType, e:
         response.status = code

   return decorator(ExceptionToCode)

@decorator
def ExceptionToError(f, *args, **kwargs):
   try:
      return f(*args, **kwargs)
   except HTTPError, e:
      # Let explicit HTTP error codes that we raise pass through as-is.
      response.status = e.code
   except Exception, e:
      # So that it shows up under the caller's logger.
      log = logging.getLogger(f.__module__)
      log.exception('Error')

      error = { 'type': e.__class__.__name__, 'message': str(e) }
      response.write(json.dumps(error))

      response.status = httplib.BAD_REQUEST

class BaseController(WSGIController):
   def _getClient(self, environ):
      client = environ.get('converter.client')

      # Can't put this in environ.get() call because the
      # config['converter.client'] isn't set for testing.
      if client is None:
         client = config['converter.client']

      return client

   def _sendFileResponse(self, filePath, fileName):
      """
         Helper method to stream a file's data over HTTP instead of buffering
         the whole file in memory.  Idea for this approach from this SO post:

         http://stackoverflow.com/questions/2413707/stream-a-file-to-the-http-response-in-pylons
      """

      fileLen = os.path.getsize(filePath)

      # Don't bother setting Content-Type here, FileApp docs say it will be
      # guessed with mimetypes.guess_type() instead.  Looks like it works
      # well for zip file data.
      headers = [
         ('Content-Disposition', 'attachment; filename="%s"' % (fileName,)),
         ('Content-Length', str(fileLen)),
      ]

      f = FileApp(filePath, headers=headers)
      return f(request.environ, self.start_response)

   def __call__(self, environ, start_response):
      """Invoke the Controller"""
      # WSGIController.__call__ dispatches to the Controller method
      # the request is routed to. This routing information is
      # available in environ['pylons.routes_dict']
      return WSGIController.__call__(self, environ, start_response)
