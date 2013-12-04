#
# Copyright 2011 VMware, Inc.  All rights reserved. -- VMware Confidential
#
# This script adds Spring Security 3.0. related tables:
# 1. users
# 2. authorities
# 3. persistent_logins (Remember-me service)
# @see http://static.springsource.org/spring-security/site/docs/3.0.x/reference/springsecurity-single.html#d0e6992
#
from sqlalchemy import *
from migrate.changeset import *

meta = MetaData()

users_cols = (
   Column('username', String(50), nullable=False, primary_key=True),
   Column('password', String(50), nullable=False),
   Column('enabled', Boolean(), nullable=False, default=True)
)
users_tab = Table('users', meta, *users_cols)

authorities_cols = (
   Column('username', String(50), ForeignKey('users.username'), nullable=False),
   Column('authority', String(50), nullable=False),
   Column('enabled', Boolean(), nullable=False, default=True)
)
authorities_tab = Table('authorities', meta, *authorities_cols)

# place a unique index on username and authority
auth_username_index = Index('ix_auth_username', authorities_tab.c.username, authorities_tab.c.authority, unique=True)

# table for storing persistent token of 'Remember-Me' Service
persistent_logins_cols = (
   Column('username', String(64), nullable=False),
   Column('series', String(64), nullable=False, primary_key=True),
   Column('token', String(64), nullable=False),
   Column('last_used', DateTime, nullable=False)
)
persistent_logins_tab = Table('persistent_logins', meta, *persistent_logins_cols)


def upgrade(migrate_engine):
   meta.bind = migrate_engine
   users_tab.create()
   authorities_tab.create()
   persistent_logins_tab.create()
   # insert seed data
   users_tab.insert().execute(username='admin', password='', enabled=True)
   authorities_tab.insert().execute(username='admin', authority='ROLE_ADMIN', enabled=True)

def downgrade(migrate_engine):
   meta.bind = migrate_engine
   persistent_logins_tab.drop()
   authorities_tab.drop()
   users_tab.drop()

