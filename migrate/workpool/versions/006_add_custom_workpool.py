#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()
instance = Table('instance', meta)

def BasicString():
   return String(255)

def idField(*args, **kwargs):
   # Set autoincrement=false to avoid creating a new sequence.  Hibernate will use
   # hibernate_sequence.
   return Column('id', BigInteger(), *args, primary_key=True, nullable=False, autoincrement=False, **kwargs)

def OsType():
   return (Column('ostype', Integer(), nullable=False),
           Column('variant', BasicString(), nullable=False),)

customworkpool = Table('customworkpool', meta,
   idField(),
   *(OsType()))

def upgrade(migrate_engine):
   meta.bind = migrate_engine
   customworkpool.create()

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   customworkpool.drop()
