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

package com.vmware.appfactory.common.runner;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.annotation.Resource;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.cws.CwsClientService;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * This class is a Spring bean defined in app-config.xml. It holds the current
 * state of the expiration status.
 *
 * @author saung
 * @since v1.0 6/21/2011
 */
public class LicenseStatus implements Runnable {
   /** Get the logger */
   private final Logger _log = LoggerFactory.getLogger(LicenseStatus.class);

   /** Number of ticks for a slow expiration refresh: 1 hour **/
   private final long SLOW_REFRESH_TICKS = 720;

   /** Number of ticks for a fast expiration refresh: 5 seconds **/
   private final long FAST_REFRESH_TICKS = 1;

   /** Number of calls to run remaining before we take action **/
   private long _remainingTicks = 0;

   /** boolean flag to indicate the license is expired or not. */
   private boolean _isExpired = false;

   /** Number of days until expiration. **/
   private long _numDaysToExpiration = Long.MAX_VALUE;

   /** expiration date from last recent call to CWS. **/
   private Date _expirationDate;

   @Resource
   protected ConfigRegistry _config;

   @Resource
   private CwsClientService _cwsClient;

   /**##### Spring IoC injected instances configured in app-config.xml. #####*/
   /** number of days to show warning message before expired. */
   private long _numDaysToShowWarningBeforeExpired;


   /**
    * Invoke CWS to get a license expiration date, calculate how
    * many days left and set appropriate error message.
    * If the call to CWS failed, it won't refresh any data.
    */
   @Override
   public void run() {
      long remainingTicks = getRemainingTicks();
      if (remainingTicks > 0) {
         setRemainingTicks(remainingTicks - 1);
      }
      else {
         final Date expDate = _cwsClient.getLicenseExpirationDate();
         if (expDate != null) {
            _expirationDate = expDate;
            calculateNumDaysToExpiration();
            _log.info("Refreshed expiration status from CWS: " + expDate);

            /* Slow down refreshes now that one has succeeded */
            slowDownRefresh();
         }
      }
   }


   /**
    * @return the isExpired
    */
   public boolean isExpired()
   {
      return _isExpired;
   }


   /**
    * Find out whether license is expired or not using yesterday midnight.
    * Calculate the number of days until expiration.
    */
   private final void calculateNumDaysToExpiration()
   {
      final GregorianCalendar gCal = new GregorianCalendar(AfCalendar.LOCAL_TZONE);
      gCal.add(Calendar.DATE, -1);
      final Date yesterdayMidnight = DateUtils.round(gCal.getTime(), Calendar.HOUR);
      setNumDaysToExpiration((_expirationDate.getTime() - yesterdayMidnight.getTime()) / DateUtils.MILLIS_PER_DAY);

      if (getNumDaysToExpiration() <= 0) {
         _isExpired = true;
      } else {
         _isExpired = false;
      }
   }


   /**
    * @return the numDaysToShowWarningBeforeExpired
    */
   public long getNumDaysToShowWarningBeforeExpired() {
      return _numDaysToShowWarningBeforeExpired;
   }


   /**
    * @return the number of days until expiration
    */
   public long getNumDaysToExpiration()
   {
      return _numDaysToExpiration;
   }


   /**
    * Set the number of days until expiration
    * @param numDaysToExpiration
    */
   public void setNumDaysToExpiration(long numDaysToExpiration)
   {
      _numDaysToExpiration = numDaysToExpiration;
   }


   /**
    * @return the expirationDate
    */
   public Date getExpirationDate()
   {
      return _expirationDate;
   }


   /**
    * @param numDaysToShowWarningBeforeExpired the numDaysToShowWarningBeforeExpired to set
    */
   public void setNumDaysToShowWarningBeforeExpired(
         long numDaysToShowWarningBeforeExpired)
   {
      _numDaysToShowWarningBeforeExpired = numDaysToShowWarningBeforeExpired;
   }


   /**
    * Get the number of remaining ticks
    */
   private long getRemainingTicks()
   {
      return _remainingTicks;
   }


   /**
    * Set the number of remaining ticks
    */
   private void setRemainingTicks(long numTicks)
   {
      _remainingTicks = numTicks;
   }


   /**
    * Increase the time we poll for the expiration date.
    */
   private void hastenRefresh()
   {
      setRemainingTicks(FAST_REFRESH_TICKS);
   }


   /**
    * Decrease the time we poll for the expiration date.
    */
   private void slowDownRefresh()
   {
      /* XXX: This is not ideal.  It would be best to be scheduled by a timer
       * class.  However, I've attempted to change the rate of scheduling here
       * using ScheduledExecutorTask.setPeriod().  That does in fact modify the
       * value the class has for the period, (confirmed by getPeriod()), but the
       * task is still run at the previous rate.
       */
      setRemainingTicks(SLOW_REFRESH_TICKS);
   }
}
