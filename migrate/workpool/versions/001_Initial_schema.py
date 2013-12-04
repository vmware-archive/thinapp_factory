#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
from sqlalchemy import *
from migrate import *

meta = MetaData()

### Column helpers ###

def idField(*args, **kwargs):
   return Column('id', BigInteger(), Sequence('hibernate_sequence'), *args, primary_key=True, nullable=False, **kwargs)

def BasicString():
   return String(255)

def OsType():
   return (Column('ostype', Integer(), nullable=False),
           Column('variant', BasicString(), nullable=False),)

def OsRegistration():
   return (Column('kmsserver', BasicString()),
           Column('licensekey', BasicString()),
           Column('organization', BasicString()),
           Column('username', BasicString()),)

### Table definitions ###

instance = Table('instance', meta,
   idField(),
   Column('guestpassword', BasicString(), nullable=False),
   Column('guestusername', BasicString(), nullable=False),
   Column('autologon', Boolean(), nullable=False),
   Column('moid', BasicString(), unique=True),
   Column('workpoolmodel_id', BigInteger(), ForeignKey('workpoolmodel.id'), nullable=False),
)

lease = Table('lease', meta,
   idField(),
   Column('instance_id', BigInteger(), ForeignKey('instance.id'), nullable=False),
)

vcconfig = Table('vcconfig', meta,
   Column('name', BasicString(), primary_key=True, nullable=False),
   Column('datacenter', BasicString()),
   Column('host', BasicString()),
   Column('password', BasicString()),
   Column('username', BasicString()),
   Column('computeresource', BasicString()),
   Column('datastorename', BasicString()),
   Column('resourcepool', BasicString()),
)

instanceable = Table('instanceable', meta,
   idField(),
)

vmimage = Table('vmimage', meta,
   idField(),
   Column('moid', BasicString(), nullable=False),
   Column('name', BasicString(), nullable=False),
   Column('vmpattern_id', BigInteger(), ForeignKey('vmpattern.id')),
   UniqueConstraint('name', 'moid', name='vmimage_name_key'),
   *(OsType() + OsRegistration())
)

vmpattern = Table('vmpattern', meta,
   idField(),
   Column('networkname', BasicString(), nullable=False),
   Column('sourceiso', BasicString(), nullable=False),
   *(OsType() + OsRegistration())
)

fullworkpool = Table('fullworkpool', meta,
   idField(),
   Column('instance_id', BigInteger(), ForeignKey('vmpattern.id'), nullable=False),
)

linkedworkpool = Table('linkedworkpool', meta,
   idField(),
   Column('instance_id', BigInteger(), ForeignKey('vmimage.id'), nullable=False),
)

workpoolmodel = Table('workpoolmodel', meta,
   idField(),
   Column('maximuminstances', Integer(), nullable=False),
   Column('name', BasicString(), unique=True),
)

workpoolmodel_lease = Table('workpoolmodel_lease', meta,
   Column('workpoolmodel_id', BigInteger(), ForeignKey('workpoolmodel.id'),
          Sequence('hibernate_sequence'), primary_key=True, nullable=False),
   Column('leases_id', BigInteger(), ForeignKey('lease.id'), Sequence('hibernate_sequence'),
          primary_key=True, nullable=False, unique=True),
)

### Migration ###

def upgrade(migrate_engine):
   meta.create_all(migrate_engine)

def downgrade(migrate_engine):
   # Dangerous!
   meta.drop_all(migrate_engine)
