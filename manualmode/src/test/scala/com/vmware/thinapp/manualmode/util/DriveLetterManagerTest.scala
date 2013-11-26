/* ***********************************************************************
 * VMware ThinApp Factory
 * Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ***********************************************************************/

package com.vmware.thinapp.manualmode.util

import org.junit.Test
import org.junit.Assert._
import com.vmware.thinapp.manualmode.util.DriveLetterManager.DriveLetter
import com.vmware.thinapp.manualmode.util.DriveLetterManager.DEFAULT_LETTERS
import com.vmware.thinapp.common.converter.exception.ConverterException

import scala.collection.JavaConversions._

class DriveLetterManagerTest {
   /**
    * Ensure the drive letter enum has ALL the letters.
    */
   @Test
   def testDriveLetters {
      val letters = DriveLetter.values
      assertEquals(26, letters.length)
      val letterSet = DriveLetter.asSet
      assertTrue(letters.forall(letterSet.contains(_)))
      assertEquals("A:", DriveLetter.A.driveString)
   }

   /**
    * Verify that A, B, C, and D are reserved by default.
    */
   @Test
   def testDefaultReservation {
      val manager = DriveLetterManager.getDefault(DriveLetter.E)
      val defaultLetters = asScalaSet(DriveLetterManager.DEFAULT_LETTERS)
      assertTrue(defaultLetters.forall(manager.isReserved(_)))
   }

   /**
    * Verify that the given drive letter is reserved first.
    */
   @Test
   def testTryFirst {
      val manager = DriveLetterManager.getDefault(DriveLetter.E)
      assertEquals(DriveLetter.E, manager.reserve)
   }

   /**
    * Verify that drive letters can be reserved.
    */
   @Test
   def testReserve {
      val manager = DriveLetterManager.getDefault(DriveLetter.E)
      assertEquals(DriveLetter.E, manager.reserve)
      val letter = manager.reserve
      assertTrue(manager.isReserved(letter))
   }

   /**
    * Verify that drive letters can be reserved explicitly.
    */
      @Test
   def testReserveExplicit {
      val manager = DriveLetterManager.getDefault(DriveLetter.E)
      manager.reserve(DriveLetter.F)
      assertTrue(manager.isReserved(DriveLetter.F))
      assertEquals(DriveLetter.E, manager.reserve)
   }

   /**
    * Verify that a drive letter cannot be reserved twice.
    */
   @Test(expected=classOf[ConverterException])
   def testDoubleReserve {
      val manager = DriveLetterManager.getDefault(DriveLetter.E)
      manager.reserve(DriveLetter.F)
      manager.reserve(DriveLetter.F)
   }

   /**
    * Verify that reserved drive letters can be associated with a key.
    */
   @Test
   def testReserveWithKey {
      val manager: DriveLetterManager[String] =
         DriveLetterManager.getDefault(DriveLetter.E)
      val first = manager.reserveWithKey("First")
      assertTrue(manager.isReserved(first))
      assertTrue(manager.containsKey("First"))
      assertEquals(DriveLetter.E, first)

      val second = manager.reserveWithKey("Second")
      assertTrue(manager.isReserved(second))
      assertTrue(manager.containsKey("Second"))

      val third = manager.reserveWithKey("First")
      assertTrue(manager.isReserved(third))
      assertTrue(manager.containsKey("First"))
      assertEquals(first, third)
   }

   /**
    * Verify that reservations fail when no letters are left.
    */
   @Test(expected=classOf[ConverterException])
   def testOutOfLetters {
      val manager = DriveLetterManager.getDefault(DriveLetter.E)
      val defaultLetters = asScalaSet(DriveLetterManager.DEFAULT_LETTERS)
      val unreservedLetters = DriveLetter.values.toSet--defaultLetters
      unreservedLetters.foreach(letter => {
         assertFalse(manager.isReserved(letter))
         manager.reserve(letter)
         assertTrue(manager.isReserved(letter))
      })
      manager.reserve
   }
}