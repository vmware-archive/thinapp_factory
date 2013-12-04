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


import contextlib
import logging
import os
import re

from datetime import datetime
from sqlalchemy import create_engine, Boolean, Column, DateTime, ForeignKey, LargeBinary, Integer, String, Unicode, Enum, LargeBinary, BigInteger
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import backref, relationship, scoped_session, sessionmaker, column_property

Base = declarative_base()

log = logging.getLogger('converter.server.model')


class SMBShare(object):
   """
      For now a simple data object, but other objects may implement this
      interface later and come up with a different way to generate UNC and
      local paths.
   """
   def __init__(self, name, localPath, uncPath, domain, username, password, baseUrl=None):
      # TODO: Perhaps validate uncPath to make sure it's valid (it's
      # easy to use '\\server\share' instead of r'\\server\share').

      # Ensure we don't get a path in form //server/share.
      if uncPath is not None and '/' in uncPath:
         raise ValueError("UNC path should not contain a '/'.")

      self.name = name
      self.localPath = localPath
      self.uncPath = uncPath
      self.domain = domain
      self.username = username
      self.password = password
      self.baseUrl = baseUrl

   def GetUncForLinux(self):
      return self.uncPath.replace('/', '\\')

   def GetLocalPath(self):
      return self.localPath

   def GetUncPath(self):
      return self.uncPath

   # Returns domain\username if the domain is set, otherwise username.
   def GetFullUsername(self):
      if self.domain:
         return '%s\\%s' % (self.domain, self.username)
      else:
         return self.username

   def GetUsername(self):
      return self.username

   def GetPassword(self):
      return self.password

   def GetName(self):
      return self.name

   def IsValid(self):
      # XXX: No SMB validation yet, just local path.
      return os.path.exists(self.localPath)

   def __repr__(self):
      return 'SMBShare(name="%s",localPath="%s",uncPath="%s",domain="%s",username="%s",' \
             'password="%s",baseUrl="%s"' % (self.name, self.localPath, self.uncPath, self.domain,
                                             self.username, self.password, self.baseUrl)

class File(Base):
   __tablename__ = 'project_files'

   project_id = Column(Integer, ForeignKey('projects.id'), primary_key=True)

   # Relative file path to the job output directory.
   path = Column(Unicode, nullable=False, primary_key=True)

   # Size in bytes.
   size = Column(BigInteger, nullable=False)

   def __init__(self, path, size):
      self.path = path
      self.size = size

   def __repr__(self):
      return 'File(path="%s", size=%d)' % (self.path, self.size)

   def __eq__(self, rhs):
      # We only consider the file path and not file sizes when testing
      # for equality.
      return self.path == rhs.path

   def __neq__(self, rhs):
      return not self.__eq__(rhs)

class RegistryValue(Base):
   """
      Represents the zero or more values that a registry key (RegistryKey)
      can have.
   """
   __tablename__ = 'registry_values'

   id = Column(Integer, primary_key=True)
   key_id = Column(Integer, ForeignKey('registry_keys.id'), nullable=False)
   name = Column(String) # null ok for default value
   regType = Column(Enum('REG_NONE',
                         'REG_SZ',
                         'REG_EXPAND_SZ',
                         'REG_BINARY',
                         'REG_DWORD',
                         'REG_DWORD_BIG_ENDIAN',
                         'REG_LINK',
                         'REG_MULTI_SZ',
                         'REG_RESOURCE_LIST',
                         'REG_FULL_RESOURCE_DESCRIPTOR',
                         'REG_RESOURCE_REQUIREMENTS_LIST',
                         'REG_QWORD',
                         name='RegistryType',
                         nullable=False))

   data = Column(String, nullable=False)

   # Metadata about variable expansion
   nameExpand = Column(Boolean)
   dataExpand = Column(Boolean)

class RegistryKey(Base):
   __tablename__ = 'registry_keys'

   id = Column(Integer, primary_key=True)
   parent_id = Column(Integer, ForeignKey('registry_keys.id'))

   path = Column(String, nullable=False)
   isolation = Column(Enum('full', 'merged', 'writecopy', 'sb_only',
                           name='IsolationType'))

   # Whether this key was created "on the way" to another key.
   # In other words, unless this key gets modified, it should not be
   # written back to the file.
   intermediate = Column(Boolean)

   values = relationship(RegistryValue, backref='key', cascade='all, delete-orphan')
   subkeys = relationship('RegistryKey', backref=backref('parent', remote_side=id), cascade='all')

class ThinAppFile(Base):
   """
      Represents files and directories in the sandbox of a ThinApp project.
   """
   __tablename__ = 'thinapp_files'

   id = Column(Integer, primary_key=True)
   parent_id = Column(Integer, ForeignKey('thinapp_files.id'))
   root_id = Column(Integer, ForeignKey('thinapp_files.id'))
   path = Column(Unicode, nullable=False)

   # Attributes
   isDirectory = Column(Boolean, default=False)
   hidden = Column(Boolean, default=False)

   # Relationships to other files
   root = relationship('ThinAppFile',
                       primaryjoin=id == root_id,
                       remote_side=id)

   children = relationship('ThinAppFile',
                           primaryjoin=id == parent_id,
                           backref=backref('parent', remote_side=id),
                           cascade='all')

class Project(Base):
   __tablename__ = 'projects'

   id = Column(Integer, primary_key=True)

   # id of the ThinApp runtime to build the project against.
   runtime_id = Column(Integer, nullable=False)
   subdir = Column(String, nullable=False)
   state = Column(Enum('created', 'available', 'dirty', 'rebuilding',
                       'deleting', 'deleted', name='ProjectState'),
                  nullable=False)
   icon = Column(LargeBinary, nullable=True)

   datastore_id = Column(Integer, ForeignKey('datastores.id'), nullable=False)
   # Both registry_id and directory_id must be nullable for for when
   # an empty project is created.  The project will later have a
   # project structure added to it and refreshed where the registry
   # and directory entries will be populated.
   registry_id = Column(Integer, ForeignKey('registry_keys.id'), nullable=True)
   directory_id = Column(Integer, ForeignKey('thinapp_files.id'), nullable=True)

   # When a project is deleted all its files, registry captures, and
   # hierarchy traversals will be deleted as well.
   files = relationship(File, backref='project', cascade='all, delete, delete-orphan')
   registry = relationship(RegistryKey, backref=backref('projects', uselist=False), cascade='all')
   directory = relationship(ThinAppFile, backref=backref('projects', uselist=False), cascade='all')

class DataStore(Base):
   __tablename__ = 'datastores'

   SERVER_RE = re.compile(r'^\\\\([^\\]+)\\(.*)')

   id = Column(Integer, primary_key=True)
   # XXX: ASCII for now until unicode values are tested.
   name = Column(String, nullable=False, unique=True)
   domain = Column(String, nullable=True)
   # XXX: NULL = guest?  Need to interpret.
   # Have to allow nullable right now for internal shares.
   username = Column(String, nullable=True)
   password = Column(String, nullable=True)
   # Current local filesystem path.
   localPath = Column(String, nullable=True)
   server = Column(String, nullable=True)
   share = Column(String, nullable=True)
   status = Column(Enum('online', 'offline', name='StorageState'), nullable=False, default='offline')
   baseUrl = Column(String, nullable=True)

   # When a datastore is deleted all projects from it are deleted.
   projects = relationship(Project, backref='datastore', cascade='all')

   @classmethod
   def FromShare(cls, share):
      # The internal share doesn't have a uncPath so it's optional.
      server = None
      shareName = None

      if share.uncPath:
         server, shareName = cls.SERVER_RE.match(share.uncPath).groups()

      ds = cls()
      ds.name = share.name
      ds.domain = share.domain
      ds.username = share.username
      ds.password = share.password
      ds.localPath = share.localPath
      ds.server = server
      ds.share = shareName
      ds.baseUrl = share.baseUrl

      return ds

   def __str__(self):
      return ' '.join([str(i) for i in (self.id, self.name, self.username, self.password,
                                        self.localPath, self.server, self.share, self.baseUrl)])

   def GetAsShare(self):
      uncPath = r'\\%s\%s' % (self.server, self.share)

      return SMBShare(self.name, self.localPath, uncPath, self.domain, self.username, self.password)

class Database(object):
   def __init__(self, url, echo=False):
      self.engine = create_engine(url, echo=echo)

      # Session is now created per-(green)thread.  Perhaps Sessions
      # should be retrieved from a Pool.  Unsure what the overhead of
      # creating one per-(green)thread is.
      self.CreateSession = sessionmaker(bind=self.engine)

   def Create(self):
      Base.metadata.create_all(self.engine)

   def Destroy(self):
      Base.metadata.drop_all(self.engine)

@contextlib.contextmanager
def AutoSession(session, close=True):
   """ Either flushes and commits changes on __exit__ or rolls back on
   exception """
   if hasattr(session, '_cws_autosession'):
      # The outer autosession will deal with any exceptions that arise.
      yield
   else:
      try:
         # Set a hidden flag in the session that indicates it's inside
         # an AutoSession.
         setattr(session, '_cws_autosession', True)

         yield
         session.commit()
      except:
         if close:
            log.exception('Exception caught - attempting to close session')
         session.rollback()
         raise
      finally:
         delattr(session, '_cws_autosession')

         if close:
            session.close()
