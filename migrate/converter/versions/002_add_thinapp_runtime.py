#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()
projects = Table('projects', meta)
# default is 4.7.0 GA
runtimeId = Column('runtime_id', Integer, default=519532, nullable=True)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   runtimeId.create(projects)
   runtimeId.alter(nullable=False)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   runtimeId.drop(projects)
