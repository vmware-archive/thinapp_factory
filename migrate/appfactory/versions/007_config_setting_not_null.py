#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()

configsetting = Table('configsetting', meta)
configsetting_value = Column('_value', String(255), nullable=False)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   configsetting_value.alter(table=configsetting, nullable=True)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   configsetting_value.alter(table=configsetting, nullable=False)
