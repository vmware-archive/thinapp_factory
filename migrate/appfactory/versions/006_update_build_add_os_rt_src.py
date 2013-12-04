#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
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
