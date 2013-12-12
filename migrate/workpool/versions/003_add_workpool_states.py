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
workpool = Table('workpoolmodel', meta)
instance = Table('instance', meta)
vmimage = Table('vmimage', meta)
workpoolState = Column('state', String, default='created')
instanceState = Column('state', String, default='created')
vmImageState = Column('state', String, default='unknown')

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolState.create(workpool, populate_default=True)
   instanceState.create(instance, populate_default=True)
   # Can't set nullable until after data updated with default value.
   workpoolState.alter(table=workpool, nullable=False)
   instanceState.alter(table=instance, nullable=False)

   # This was supposed to be false before, but was wrong.
   vmImageState.alter(table=vmimage, nullable=False)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolState.drop(workpool)
   instanceState.drop(instance)
   vmImageState.alter(table=vmimage, nullable=True)
