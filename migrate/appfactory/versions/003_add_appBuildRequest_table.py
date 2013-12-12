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

# This script adds a new table "appbuildrequest".
#

from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()

def commonFields(*args, **kwargs):
   return (Column('_id', BigInteger(), primary_key=True, nullable=False),
           Column('_created', BigInteger(), nullable=False),
           Column('_modified', BigInteger(), nullable=False))

application = Table('application', meta, Column('_id', BigInteger(), nullable=False))

appbuildrequest_cols = commonFields() + (
   Column('_ostype', String(255), nullable=False),
   Column('_osvariant', String(255), nullable=False),
   Column('_runtime', String(255), nullable=False),
   Column('_requeststage', String(255), nullable=False),
   Column('_packageformat', String(255)),
   Column('_buildid', BigInteger()),
   Column('_application__id', BigInteger(), ForeignKey('application._id')),
   Column('_datastoreid', BigInteger()),
   Column('_recipeid', BigInteger()),
   Column('_ismanualmode', Boolean(), nullable=False)
)

appbuildrequest_tab = Table('appbuildrequest', meta, *appbuildrequest_cols)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   appbuildrequest_tab.create()

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   appbuildrequest_tab.drop()
