#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
# This script adds a new table "appbuildrequest".
#

from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()

def commonFields(*args, **kwargs):
   return (Column('_id', BigInteger(), primary_key=True, nullable=False),
           Column('_created', BigInteger(), nullable=False),
           Column('_modified', BigInteger(), nullable=False))

application = Table('application', meta, Column('_id', BigInteger(), nullable=False))

appbuildrequest_cols = commonFields() + (
   Column('_ostype', String(255), nullable=False),
   Column('_osvariant', String(255), nullable=False),
   Column('_runtime', String(255), nullable=False),
   Column('_requeststage', String(255), nullable=False),
   Column('_packageformat', String(255)),
   Column('_buildid', BigInteger()),
   Column('_application__id', BigInteger(), ForeignKey('application._id')),
   Column('_datastoreid', BigInteger()),
   Column('_recipeid', BigInteger()),
   Column('_ismanualmode', Boolean(), nullable=False)
)

appbuildrequest_tab = Table('appbuildrequest', meta, *appbuildrequest_cols)

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   appbuildrequest_tab.create()

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   appbuildrequest_tab.drop()
