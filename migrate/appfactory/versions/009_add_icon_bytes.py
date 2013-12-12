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

appicon = Table('appicon', meta)
appicon_localurl = Column('_localurl', String(255), nullable=True)
appicon_bytes = Column('_iconbytes', LargeBinary, nullable=True)
appicon_hash = Column('_iconhash', String(32), nullable=True)

buildicon = Table('buildicon', meta)
buildicon_localurl = Column('_localurl', String(255), nullable=True)
buildicon_bytes = Column('_iconbytes', LargeBinary, nullable=True)
buildicon_hash = Column('_iconhash', String(32), nullable=True)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   appicon_localurl.create(appicon)
   appicon_bytes.create(appicon)
   appicon_hash.create(appicon)
   buildicon_localurl.create(buildicon)
   buildicon_bytes.create(buildicon)
   buildicon_hash.create(buildicon)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   appicon_localurl.drop(appicon)
   appicon_bytes.drop(appicon)
   appicon_hash.drop(appicon)
   buildicon_localurl.drop(buildicon)
   buildicon_bytes.drop(buildicon)
   buildicon_hash.drop(buildicon)
