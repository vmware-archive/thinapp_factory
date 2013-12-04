#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()

build = Table('build', meta)
build_horizonApplicationId = Column('_horizonapplicationid', String(255), nullable=True)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   build_horizonApplicationId.create(build)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   build_horizonApplicationId.drop(build)
