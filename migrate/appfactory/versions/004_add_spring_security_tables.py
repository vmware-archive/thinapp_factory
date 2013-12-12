# VMware ThinApp Factory
# Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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

