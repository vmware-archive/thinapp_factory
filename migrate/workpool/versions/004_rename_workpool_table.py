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

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   Table('workpoolmodel', meta).rename('workpool')
   workpoolModelLease = Table('workpoolmodel_lease', meta)
   workpoolModelLease.rename('workpool_lease')
   Column('workpoolmodel_id', BigInteger).alter(table=instance, name='workpool_id')
   Column('workpoolmodel_id', BigInteger).alter(table=workpoolModelLease, name='workpool_id')

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   Table('workpool', meta).rename('workpoolmodel')
   workpoolModelLease = Table('workpool_lease', meta)
   workpoolModelLease.rename('workpoolmodel_lease')
   Column('workpool_id', BigInteger).alter(table=instance, name='workpoolmodel_id')
   Column('workpool_id', BigInteger).alter(table=workpoolModelLease, name='workpoolmodel_id')
