#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()
instance = Table('instance', meta)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   Table('workpoolmodel', meta).rename('workpool')
   workpoolModelLease = Table('workpoolmodel_lease', meta)
   workpoolModelLease.rename('workpool_lease')
   Column('workpoolmodel_id', BigInteger).alter(table=instance, name='workpool_id')
   Column('workpoolmodel_id', BigInteger).alter(table=workpoolModelLease, name='workpool_id')

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   Table('workpool', meta).rename('workpoolmodel')
   workpoolModelLease = Table('workpool_lease', meta)
   workpoolModelLease.rename('workpoolmodel_lease')
   Column('workpool_id', BigInteger).alter(table=instance, name='workpoolmodel_id')
   Column('workpool_id', BigInteger).alter(table=workpoolModelLease, name='workpoolmodel_id')
