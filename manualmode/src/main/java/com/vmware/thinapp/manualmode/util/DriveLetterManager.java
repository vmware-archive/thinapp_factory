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

package com.vmware.thinapp.manualmode.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.vmware.thinapp.common.converter.exception.ConverterException;

/**
 * Utility class for managing Windows drive letters.  Once a manager is
 * created, one can reserve drive letters one by one.
 */
public class DriveLetterManager<T> {
   public enum DriveLetter {
      A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z;

      public static Set<DriveLetter> asSet() {
         return new HashSet<DriveLetter>(Arrays.asList(DriveLetter.values()));
      }

      public String driveString() {
         return this.toString() + ":";
      }
   }

   /**
    * Drive letters that are reserved by default.
    */
   public static final Set<DriveLetter> DEFAULT_LETTERS = ImmutableSet.<DriveLetter>of(
      DriveLetter.A,
      DriveLetter.B,
      DriveLetter.C,
      DriveLetter.D);

   private final Set<DriveLetter> reservedLetters;
   private final Map<T, DriveLetter> keyToDriveLetter;
   private final DriveLetter tryFirst;

   /**
    * Construct a new manager that uses the given drive letter as the first
    * letter to attempt to reserve.
    *
    * @param tryFirst
    */
   private DriveLetterManager(DriveLetter tryFirst) {
      this.reservedLetters = new HashSet<DriveLetter>();
      this.keyToDriveLetter = new HashMap<T, DriveLetter>();
      this.tryFirst = tryFirst;

      // Reserve A, B, C, and D by default
      for (DriveLetter letter : DEFAULT_LETTERS) {
         this.reserve(letter);
      }
   }

   /**
    * Creates a default drive letter manager.
    *
    * @param tryFirst A single drive letter to attempt to reserve first
    * @return a default drive letter manager with drive letter C already
    *         reserved
    */
   public static <T> DriveLetterManager<T> getDefault(DriveLetter tryFirst) {
      return new DriveLetterManager<T>(tryFirst);
   }

   /**
    * Attempt to reserve the given drive letter.
    *
    * @param driveLetter drive letter to attempt to reserve
    * @throws ConverterException if the given letter
    */
   public void reserve(DriveLetter driveLetter) {
      if (isReserved(driveLetter)) {
         throw new ConverterException("Given drive letter is already reserved");
      }

      this.reservedLetters.add(driveLetter);
   }

   /**
    * Attempt to reserve any available drive letter.
    *
    * @return the reserved drive letter
    * @throws ConverterException if no drive letters are available
    */
   public DriveLetter reserve() {
      Set<DriveLetter> remaining = DriveLetter.asSet();
      remaining.removeAll(reservedLetters);
      if (!remaining.isEmpty()) {
         DriveLetter reserve;
         if (!isReserved(tryFirst)) {
            reserve = tryFirst;
         } else {
            reserve = remaining.iterator().next();
         }
         reservedLetters.add(reserve);
         return reserve;
      }

      throw new ConverterException("No drive letters left to reserve");
   }

   /**
    * Attempt to reserve any available drive letter.  If a drive letter has
    * already been reserved with the given key, that drive letter is returned
    * without reserving a new one.
    *
    * @param key the key to associate the drive letter with
    * @return the reserved drive letter
    * @throws ConverterException if no drive letters are available
    */
   public DriveLetter reserveWithKey(T key) {
      // Check for the key before reserving a new drive letter
      if (containsKey(key)) {
         return keyToDriveLetter.get(key);
      } else {
         DriveLetter reserve = this.reserve();
         keyToDriveLetter.put(key, reserve);
         return reserve;
      }
   }

   /**
    * @param key the key to check for
    * @return true if the given key already has a drive letter associated with
    *         it, false otherwise
    */
   public boolean containsKey(T key) {
      return keyToDriveLetter.containsKey(key);
   }

   /**
    * @return true if the given drive letter is reserved
    */
   public boolean isReserved(DriveLetter letter) {
      return reservedLetters.contains(letter);
   }
}
