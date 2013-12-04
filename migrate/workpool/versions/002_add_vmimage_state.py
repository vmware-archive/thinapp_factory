#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()
vmimage = Table('vmimage', meta)
col = Column('state', String, default='unknown')

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   col.create(vmimage, populate_default=True)
   # Can't set nullable until after data updated with default value.
   col.alter(nullable=True)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   col.drop(vmimage)
