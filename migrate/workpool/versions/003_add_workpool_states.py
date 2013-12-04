#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()
workpool = Table('workpoolmodel', meta)
instance = Table('instance', meta)
vmimage = Table('vmimage', meta)
workpoolState = Column('state', String, default='created')
instanceState = Column('state', String, default='created')
vmImageState = Column('state', String, default='unknown')

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolState.create(workpool, populate_default=True)
   instanceState.create(instance, populate_default=True)
   # Can't set nullable until after data updated with default value.
   workpoolState.alter(table=workpool, nullable=False)
   instanceState.alter(table=instance, nullable=False)

   # This was supposed to be false before, but was wrong.
   vmImageState.alter(table=vmimage, nullable=False)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolState.drop(workpool)
   instanceState.drop(instance)
   vmImageState.alter(table=vmimage, nullable=True)
