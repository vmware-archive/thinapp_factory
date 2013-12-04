#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()
projects = Table('projects', meta)
project_icon = Column('icon', LargeBinary, nullable=True)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   project_icon.create(projects)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   project_icon.drop(projects)
