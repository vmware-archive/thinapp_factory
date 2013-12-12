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
from migrate.changeset import *

meta = MetaData()
instance = Table('instance', meta)

def BasicString():
   return String(255)

def idField(*args, **kwargs):
   # Set autoincrement=false to avoid creating a new sequence.  Hibernate will use
   # hibernate_sequence.
   return Column('id', BigInteger(), *args, primary_key=True, nullable=False, autoincrement=False, **kwargs)

def OsType():
   return (Column('ostype', Integer(), nullable=False),
           Column('variant', BasicString(), nullable=False),)

customworkpool = Table('customworkpool', meta,
   idField(),
   *(OsType()))

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   customworkpool.create()

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   customworkpool.drop()
