#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()

appicon = Table('appicon', meta)
appicon_localurl = Column('_localurl', String(255), nullable=True)
appicon_bytes = Column('_iconbytes', LargeBinary, nullable=True)
appicon_hash = Column('_iconhash', String(32), nullable=True)

buildicon = Table('buildicon', meta)
buildicon_localurl = Column('_localurl', String(255), nullable=True)
buildicon_bytes = Column('_iconbytes', LargeBinary, nullable=True)
buildicon_hash = Column('_iconhash', String(32), nullable=True)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   appicon_localurl.create(appicon)
   appicon_bytes.create(appicon)
   appicon_hash.create(appicon)
   buildicon_localurl.create(buildicon)
   buildicon_bytes.create(buildicon)
   buildicon_hash.create(buildicon)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   appicon_localurl.drop(appicon)
   appicon_bytes.drop(appicon)
   appicon_hash.drop(appicon)
   buildicon_localurl.drop(buildicon)
   buildicon_bytes.drop(buildicon)
   buildicon_hash.drop(buildicon)
