#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()

build = Table('build', meta)
build_runtime2 = Column('_newruntime', String(32) )
build_hz = Column('_hzsupported', Boolean(), nullable=False, server_default='False' )

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   build_runtime2.create(build)
   build_hz.create(build)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   build_runtime2.drop(build)
   build_hz.drop(build)
