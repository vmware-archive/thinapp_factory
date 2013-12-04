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


from setuptools import setup

setup(
    name='converter.tests',
    version='0.1',
    description='Converter Service Tests',
    author='',
    author_email='',
    url='',
    namespace_packages=['converter'],
    install_requires=[
       "converter==0.1dev",
       "nose==0.11.4",
       "mock==0.7.0b3",
       # XXX: Might be able to switch to just fixtures by replacing fixture.TempIO with
       # fixtures.TempDir().
       "fixture==1.4",
       "fixtures==0.3.5",
       "URLObject==0.5",
    ],
    packages=['converter.tests', 'converter'],
    include_package_data=True,
    zip_safe=False,
)
