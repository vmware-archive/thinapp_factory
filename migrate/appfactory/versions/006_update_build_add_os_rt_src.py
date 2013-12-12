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

# Server default only saves it from storing null, but can cause invalid display.
# Nullable _ostype and _osvariant due to imported projects

build = Table('build', meta)
build_osType = Column('_ostype', String(255), nullable=True)
build_osVar = Column('_osvariant', String(255), nullable=True)
build_runtime = Column('_runtime', String(255), nullable=False, server_default='')
build_source = Column('_source', String(255), nullable=False, server_default='AUTO_CAPTURE')

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   build_osType.create(build)
   build_osVar.create(build)
   build_runtime.create(build)
   build_source.create(build)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   build_osType.drop(build)
   build_osVar.drop(build)
   build_runtime.drop(build)
   build_source.drop(build)
