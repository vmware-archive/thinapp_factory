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

import sys
from setuptools import find_packages, setup

if sys.platform.startswith('win'):
   raise Exception('only builds on Linux')

install_requires = ['pySdk>=4.1.0', 'path.py>=5.0']

setup(name='afdeploy',
      version='1.0',
      install_requires=install_requires,
      packages=find_packages(),
      zip_safe=True,
      entry_points={
         'console_scripts': [
            'clone-vm = afdeploy.scripts:CloneVm',
         ],
      })
