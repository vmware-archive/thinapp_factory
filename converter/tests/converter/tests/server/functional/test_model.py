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


# encoding=utf-8

from converter.server import model
from datetime import datetime

class TestModel(object):
   def setup(self):
      self.i = model.Database('sqlite:///:memory:')
      model.Base.metadata.create_all(self.i.engine)

   def teardown(self):
      model.Base.metadata.drop_all(self.i.engine)

   def testEventsAreOrderedChronologicallyWithSameTime(self):
      s = self.i.CreateSession()

      job = model.Job()

      now  = datetime.now()

      e1 = model.Event(u'one', now)
      e2 = model.Event(u'two', now)
      e3 = model.Event(u'three', now)

      events = [e1, e2, e3]

      job.events.extend(events)

      s.add(job)

      s.commit()

      assert job.events == [e1, e2, e3]

   def testEventsAreOrderedChronologically(self):
      s = self.i.CreateSession()

      job = model.Job()

      t1 = datetime.utcfromtimestamp(764244233.333333)
      t2 = datetime.utcfromtimestamp(864244233.333333)
      t3 = datetime.utcfromtimestamp(964244233.333333)

      e1 = model.Event(u'one', t1)
      e2 = model.Event(u'two', t2)
      e3 = model.Event(u'three', t3)

      events = [e1, e2, e3]

      job.events.extend(events)

      s.add(job)

      s.commit()

      assert job.events == events

   def testAddingFileWithUnicode(self):
      s = self.i.CreateSession()

      job = model.Job()

      f = model.File(u'bin/μTorrent', 1024)

      job.files.append(f)

      s.add(job)
      s.commit()
