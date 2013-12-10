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


from setuptools import setup, find_packages

setup(
    name='converter',
    version='1.0',
    description='Converter Service',
    author='',
    author_email='',
    url='',
    namespace_packages=['converter'],
    install_requires=[
      # For web.
        "Pylons==1.0.1",
        "WebTest==1.3.1",
        "WebOb==1.1.1",
        "Spawning==0.9.7",
      # For server.
        "SQLAlchemy==0.6.4",
        "path.py>=5.0",
        "pypng==0.0.12",
      # For both.
        "eventlet==0.9.17",
        "PyPubSub==3.1.2",
        "Inject==1.0.1",
        "pyparsing==1.5.5",
        "psycopg2>=2.4.2",
      # Other TAF stuff
        "syscfg==1.0",
        "cifsmount==1.0",
    ],
    # For web.
    setup_requires=["PasteScript>=1.6.3"],
    packages=find_packages(),
    include_package_data=True,
    package_data={'converter.web': ['i18n/*/LC_MESSAGES/*.mo']},
    #message_extractors={'converter': [
    #        ('**.py', 'python', None),
    #        ('templates/**.mako', 'mako', {'input_encoding': 'utf-8'}),
    #        ('public/**', 'ignore', None)]},
    zip_safe=False,
    paster_plugins=['PasteScript', 'Pylons'],
    entry_points="""
    [paste.app_factory]
    main = converter.web.config.middleware:make_app

    [paste.app_install]
    main = pylons.util:PylonsInstaller
    """,
)
