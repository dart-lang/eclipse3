#!/usr/bin/python

"""Copyright 2012 the Dart project authors. All rights reserved.

Python file to test gsutil.py.
"""
import os
import platform
import tempfile
import unittest
import gsutil


class TestGsutil(unittest.TestCase):
  """Class to test the gsutil.py class."""
  test_prefix = 'gs://'
  test_bucket = 'dart-editor-archive-testing'
  test_folder = 'unit-testing'
  build_count = 3

  def setUp(self):
    self._iswindows = False
    operating_system = platform.system()
    if operating_system == 'Windows' or operating_system == 'Microsoft':
    # On Windows Vista platform.system() can return "Microsoft" with some
    # versions of Python, see http://bugs.python.org/issue1082 for details.
      self._iswindows = True
    username = os.environ.get('USER')
    if username is None:
      username = os.environ.get('USERNAME')

    if username is None:
      self.fail('could not find the user name tried environment variables'
                ' USER and USERNAME')
    if username.startswith('chrome'):
      running_on_buildbot = True
    else:
      running_on_buildbot = False

    self._gsu = gsutil.GsUtil(running_on_buildbot=running_on_buildbot)
    self._CleanFolder(self.test_folder)
    self._SetupFolder(self.test_bucket, self.test_folder, self.build_count)

  def tearDown(self):
    self._gsu = None

  def test_initialization(self):
    """test the initialization of the GsUtil class."""
    #find out if gsutil is on the path
    path_gs_util = None
    path = os.environ['PATH']
    if path is not None:
      pathelements = path.split(os.pathsep)
      for pathelement in pathelements:
        gs_util_candidate = os.path.join(pathelement, 'gsutil')
        if os.path.exists(gs_util_candidate):
          path_gs_util = gs_util_candidate
          break

    path_gs_util = None
    # ensure that the creation will find an instance of gsutil
    if path_gs_util is not None:
      self.assertFalse(self._gsu is None)
      self.assertEqual(self._gsu._gsutil, path_gs_util)
    else:
      #should be running on the build server
      if self._iswindows:
        gsutilexe = 'e:\\b\\build\\scripts\\slave\\gsutil'
        self.assertTrue(self._gsu._useshell)
      else:
        gsutilexe = '/b/build/scripts/slave/gsutil'
        self.assertFalse(self._gsu._useshell)
      self.assertEqual(gsutilexe, self._gsu._gsutil)
      self.assertFalse(self._gsu._dryrun)

  def test_readBucket(self):
    """Test GsUtil.ReadBucket to make sure it can read the bucket."""
    objects = self._FindInBucket('/' + self.test_folder + '/')
    self.assertEqual(self.build_count, len(objects))

  def test_copyObject(self):
    """Test GsUtil.CopyObject to make sure it is actually doing the copy."""
    # by setUp running the copy of a file from GoogleStorage works so it
    #  will not be tested here
    test_string = '-2-'
    test_string2 = '-22-'
    objects = self._FindInBucket(test_string)
    self.assertEqual(1, len(objects))
    copy_from = objects[0]
    copy_to = objects[0].replace(test_string, test_string2)
    self._gsu.Copy(copy_from, copy_to, False)
    objects = self._FindInBucket(test_string2)
    self.assertEqual(1, len(objects))
    self.assertTrue(test_string2 in objects[0])

  def test_removeObject(self):
    """Test GsUtil.RemoveObject.

    Make sure it removes only the selected object.
    """
    local_folder = '/{0}/'.format(self.test_folder)
    self._CleanFolder(self.test_folder)
    objects = self._FindInBucket(local_folder)
    self.assertEqual(0, len(objects))

#  @unittest.skip("Not complete yet")
  def test_getAclOnObject(self):
    """Test GsUtil.GetAcl to make sure it returns the correct ACL XML."""
    search_string = '-2-'
    objects = self._FindInBucket(search_string)
    self.assertEqual(1, len(objects))
    acl_xml = self._gsu.GetAcl(objects[0])
    self.assertTrue(acl_xml)

  def _FindInBucket(self, search_string):
    """Find a list of objects that match a given search string.

    Args:
      search_string: the string to match

    Returns:
      a collection (possibly empty) of the objects that match the search string
    """
    test_uri = '{0}{1}/{2}/*'.format(self.test_prefix, self.test_bucket,
                                     self.test_folder)
    gs_objects = self._gsu.ReadBucket(test_uri)
    objects = []
    for obj in gs_objects:
      if search_string in obj:
        objects.append(obj)
    return objects

  def _CleanFolder(self, folder):
    """CLean out a given folder.

    Args:
      folder: the name of the folder to clear
    """
    test_uri = '{0}{1}/{2}/*'.format(self.test_prefix, self.test_bucket,
                                     folder)
    bucket_list = self._gsu.ReadBucket(test_uri)
    for obj in bucket_list:
      self._gsu.Remove(obj)

  def _SetupFolder(self, bucket, folder, items):
    """Setup a folder for testing.

    Args:
      bucket: the name of the bucket the folder is in
      folder: the folder to setup
      items: the number of itest
    """
    test_uri = '{0}{1}/{2}'.format(self.test_prefix, bucket, folder)
    for count in range(1, items + 1):
      self._PopulateBucket(test_uri, count)

  def _PopulateBucket(self, object_uri, object_id):
    """Populate a bucket with dummy files.

    Args:
      object_uri: URI for the folder
      object_id: the unique id for the new object
    """
    prefix = 'gsutilTest-{0}-'.format(object_id)
    upload_file = tempfile.NamedTemporaryFile(suffix='.txt',
                                              prefix=prefix,
                                              delete=False)
    try:
      upload_file.write('test file {0}'.format(object_id))
      upload_file.close()
      file_uri = upload_file.name
      full_gs_uri = '{0}/{1}'.format(object_uri,
                                     os.path.basename(upload_file.name))
      self._gsu.Copy(file_uri, full_gs_uri, False)
    finally:
      os.remove(upload_file.name)

if __name__ == '__main__':
  unittest.main()
