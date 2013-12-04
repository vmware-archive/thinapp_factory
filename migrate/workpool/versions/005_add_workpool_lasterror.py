#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()
workpool = Table('workpool', meta)
instance = Table('instance', meta)
vmimage = Table('vmimage', meta)
workpoolLastError = Column('lasterror', String, default='')
instanceLastError = Column('lasterror', String, default='')
vmImageLastError = Column('lasterror', String, default='')

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolLastError.create(workpool, populate_default=True)
   instanceLastError.create(instance, populate_default=True)
   vmImageLastError.create(vmimage, populate_default=True)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   workpoolLastError.drop(workpool)
   instanceLastError.drop(instance)
   vmImageLastError.drop(vmimage)
