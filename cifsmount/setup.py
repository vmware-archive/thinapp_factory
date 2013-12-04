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
    version='0.1',
    description='Library to manage CIFS mounts',
    author='',
    author_email='',
    url='',
    install_requires=[
      'path.py==2.2.2.vmware',
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
