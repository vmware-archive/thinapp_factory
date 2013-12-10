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

from setuptools import Extension, find_packages, setup

# Insanity to deal with setuptools not detecting cython instead of
# pyrex.  See also: http://pypi.python.org/pypi/setuptools_cython
if 'setuptools.extension' in sys.modules:
   m = sys.modules['setuptools.extension']
   m.Extension.__dict__ = m._Extension.__dict__

ext_modules = [Extension('cifsmount.mount', ['mount.pyx'])]


setup(
    name='cifsmount',
    version='1.0',
    description='Library to manage CIFS mounts',
    author='',
    author_email='',
    url='',
    install_requires=[
      'path.py>=5.0.0',
    ],
    setup_requires=['setuptools_cython==0.2.1'],
    packages=find_packages(),
    include_package_data=True,
    zip_safe=False,
    ext_modules=ext_modules,
    entry_points={
      'console_scripts': [
         'cifsmount = cifsmount.mounter:mount',
         'cifsumount = cifsmount.mounter:umount',
      ],
    }
)
