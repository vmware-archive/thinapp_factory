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
workpool = Table('workpool', meta)
instance = Table('instance', meta)
vmimage = Table('vmimage', meta)
workpoolLastError = Column('lasterror', String, default='')
instanceLastError = Column('lasterror', String, default='')
vmImageLastError = Column('lasterror', String, default='')

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolLastError.create(workpool, populate_default=True)
   instanceLastError.create(instance, populate_default=True)
   vmImageLastError.create(vmimage, populate_default=True)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolLastError.drop(workpool)
   instanceLastError.drop(instance)
   vmImageLastError.drop(vmimage)
