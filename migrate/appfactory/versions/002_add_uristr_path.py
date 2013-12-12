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

#'_url' is now '_path'
#'_absoluteUrl(255)' is now '_uristr(4096)'

meta = MetaData()
appdownload = Table('appdownload', meta)
recipefile = Table('recipefile', meta)

appdownload_url = Column('_url', String(255))
appdownload_absoluteurl = Column('_absoluteurl', String(255))
appdownload_uristr = Column('_uristr', String(4096))
appdownload_path = Column('_path', String(255))

recipefile_url = Column('_url', String(255))
recipefile_absoluteurl = Column('_absoluteurl', String(255))
recipefile_uristr = Column('_uristr', String(4096))
recipefile_path = Column('_path', String(255))

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   appdownload_url.alter(table=appdownload, name='_path')
   appdownload_absoluteurl.alter(table=appdownload, name='_uristr', type=String(4096))

   recipefile_url.alter(table=recipefile, name='_path')
   recipefile_absoluteurl.alter(table=recipefile, name='_uristr', type=String(4096))

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   appdownload_path.alter(table=appdownload, name='_url')
   appdownload_uristr.alter(table=appdownload, name='_absoluteurl', type=String(255))

   recipefile_path.alter(table=recipefile, name='_url')
   recipefile_uristr.alter(table=recipefile, name='_absoluteurl', type=String(255))
