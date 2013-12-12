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

from sqlalchemy import *
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import backref, relationship
from migrate import *

# Taken from converter/service/converter/server/model.py
# commit 8b295d9c684d6290aa774ab324c85207156f5341
Base = declarative_base()

class Event(Base):
   __tablename__ = 'events'

   id = Column(Integer, primary_key=True)
   job_id = Column(Integer, ForeignKey('jobs.id'))
   name = Column(String, nullable=False)
   time = Column(DateTime)

class File(Base):
   __tablename__ = 'project_files'

   project_id = Column(Integer, ForeignKey('projects.id'), primary_key=True)
   path = Column(Unicode, nullable=False, primary_key=True)
   size = Column(BigInteger, nullable=False)

class RegistryValue(Base):
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
   nameExpand = Column(Boolean)
   dataExpand = Column(Boolean)

class RegistryKey(Base):
   __tablename__ = 'registry_keys'

   id = Column(Integer, primary_key=True)
   parent_id = Column(Integer, ForeignKey('registry_keys.id'))
   path = Column(String, nullable=False)
   isolation = Column(Enum('full', 'merged', 'writecopy', 'sb_only',
                           name='IsolationType'))
   intermediate = Column(Boolean)
   values = relationship(RegistryValue, backref='key', cascade='all, delete-orphan')
   subkeys = relationship('RegistryKey', backref=backref('parent', remote_side=id), cascade='all')

class ThinAppFile(Base):
   __tablename__ = 'thinapp_files'

   id = Column(Integer, primary_key=True)
   parent_id = Column(Integer, ForeignKey('thinapp_files.id'))
   root_id = Column(Integer, ForeignKey('thinapp_files.id'))
   path = Column(Unicode, nullable=False)
   isDirectory = Column(Boolean, default=False)
   hidden = Column(Boolean, default=False)
   root = relationship('ThinAppFile',
                       primaryjoin=id==root_id,
                       remote_side=id)
   children = relationship('ThinAppFile',
                           primaryjoin=id==parent_id,
                           backref=backref('parent', remote_side=id),
                           cascade='all')

class Project(Base):
   __tablename__ = 'projects'

   id = Column(Integer, primary_key=True)
   subdir = Column(String, nullable=False)
   state = Column(Enum('created', 'available', 'dirty', 'rebuilding',
                       'deleting', 'deleted', name='ProjectState'),
                  nullable=False)
   datastore_id = Column(Integer, ForeignKey('datastores.id'), nullable=False)
   registry_id = Column(Integer, ForeignKey('registry_keys.id'), nullable=True)
   directory_id = Column(Integer, ForeignKey('thinapp_files.id'), nullable=True)
   files = relationship(File, backref='project', cascade='all')
   registry = relationship(RegistryKey, backref=backref('projects', uselist=False), cascade='all')
   directory = relationship(ThinAppFile, backref=backref('projects', uselist=False), cascade='all')

class Job(Base):
   __tablename__ = 'jobs'

   id = Column(Integer, primary_key=True)
   success = Column(Boolean)
   events = relationship(Event, backref='job', order_by=Event.id, lazy='joined')
   percent = Column(Integer, default=0, nullable=False)
   project_id = Column(Integer, ForeignKey('projects.id'))
   project = relationship('Project', backref=backref('job', uselist=False))

class DataStore(Base):
   __tablename__ = 'datastores'

   id = Column(Integer, primary_key=True)
   name = Column(String, nullable=False, unique=True)
   domain = Column(String, nullable=True)
   username = Column(String, nullable=True)
   password = Column(String, nullable=True)
   localPath = Column(String, nullable=True)
   server = Column(String, nullable=True)
   share = Column(String, nullable=True)
   status = Column(Enum('online', 'offline', name='StorageState'), nullable=False, default='offline')
   baseUrl = Column(String, nullable=True)
   projects = relationship(Project, backref='datastore', cascade='all')

class ConversionJobModel(Base):
   __tablename__ = 'conversionjobmodel'

   id = Column(BigInteger, Sequence('hibernate_sequence'), primary_key=True, nullable=False)

CWS_TABLES = [Event, File, RegistryValue, RegistryKey, ThinAppFile,
              Project, Job, DataStore, ConversionJobModel]

def upgrade(migrate_engine):
   Base.metadata.create_all(migrate_engine)

def downgrade(migrate_engine):
   # Dangerous, should we actually code it like this?
   Base.metadata.drop_all(migrate_engine)
