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

package com.vmware.thinapp.manualmode.server;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.manualmode.ThreadLocalFileAppender;
import com.vmware.thinapp.manualmode.util.DownloadResult;
import com.vmware.thinapp.workpool.ProcessMonitorService;
import com.vmware.thinapp.workpool.Workpool;

/**
 * Represents each individual manual mode request
 */
@Component
@Scope("prototype")
public class Request {
   Logger log = LoggerFactory.getLogger(Request.class);

   @Autowired
   public ProcessMonitorService processMonitor;

   @Autowired
   private ExecutorService executor;

   @Autowired
   private Workpool workpool;

   /*
    * There are two phases to each request: an acquire phase where we obtain a
    * lease for a VM and a running phase where the leased VM is prepared and the
    * user is given access to it until it is given back.
    */
   private Future<Lease> acquiringVmPhase;
   private Future<Void> runningPhase;
   private final com.vmware.thinapp.common.workpool.dto.Workpool workpoolInstance;
   private final Capturer capturer;
   private final Status status;
   private final Project project;
   private final String logFile;

   private class RequestRunner implements Callable<Void> {
      @Override
      public Void call() throws Exception {
         Lease lease = null;
         try {
            ThreadLocalFileAppender.set(logFile);
            status.setProjectId(project.getId());
            log.info("Acquiring a lease for {}.", workpoolInstance);
            status.setCurrentState(State.acquiringVm);
            acquiringVmPhase = workpool.acquire(workpoolInstance);
            lease = acquiringVmPhase.get();
            log.info("Lease acquired: {}.", lease);

            workpool.withSnapshot(capturer, lease);

            status.setCurrentState(State.refreshingProject);
            log.info("Refreshing project {}.", project);
            project.refresh();
            status.setCurrentState(State.refreshingProjectDone);
            status.setCurrentState(State.success);
         } catch (CaptureRequestCanceled e) {
            log.info("Capture request was canceled.", e);
            status.setCurrentState(State.cancelled);
         } catch (Exception e) {
            // Logging this here because the exception will get lost if somebody
            // doesn't .get() the runningPhase future and they'll be really confused as to
            // what happened.
            log.error("Request failed:", e);
            status.setCurrentState(State.failure);
            throw e;
         } catch (Error e) {
            try {
               log.error("Request failed:", e);
            } catch (Throwable ignored) {
               // Ignore anything that comes our way from attempting to log the
               // error.  We don't want to lose the original exception.
            }

            status.setCurrentState(State.failure);
            throw e;
         } finally {
            if (lease != null) {
               log.info("Releasing lease.");
               workpool.release(lease);
               status.setCurrentState(State.vmReleased);
            }
            status.setCurrentState(State.finished);

            if (status.getStates().contains(State.success)) {
               log.info("Request finished successfully.");
            } else if (status.getStates().contains(State.failure)) {
               log.info("Request failed.");
            }

            ThreadLocalFileAppender.remove();

            // Carefully remove any and all temporary download directories: fixes PR 848083
            cleanupDownloadDir();
         }

         // void, actually.
         return null;
      }

      private void cleanupDownloadDir() {
         log.debug("Cleaning up downloaded files...");
         try {
            // Check the download results list for download directories
            if (!CollectionUtils.isEmpty(capturer.downloadResults)) {
               for (DownloadResult result : capturer.downloadResults) {
                  deleteDir(result.getDownloadDir());
               }
            }

            // Check the download result futures in case they were not processed by the capturer
            if (!CollectionUtils.isEmpty(capturer.downloadResultFutures)) {
               for (Future<DownloadResult> downloadResultFuture : capturer.downloadResultFutures) {
                  // TODO: Handle cancelling pending downloads as well, this is noted in PR 748393
                  if (downloadResultFuture.isDone()) {
                     try {
                        deleteDir(downloadResultFuture.get().getDownloadDir());
                     } catch (Exception e) {
                        log.debug("Unable to get download result.", e);
                     }
                  }
               }
            }
         } catch (Exception e) {
            log.error("Unable to cleanup download directories.", e);
         }
      }

      private void deleteDir(String dirStr) {
         File dir = new File(dirStr);
         if (dir.exists()) {
            // Delete quietly never throws, so no need to wrap in an exception handler
            boolean deleted = FileUtils.deleteQuietly(dir);
            if (deleted) {
               log.debug("Deleted directory: {}", dirStr);
            } else {
               log.error("Unable to delete directory: {}", dirStr);
            }
         }
      }
   }

   public Request(com.vmware.thinapp.common.workpool.dto.Workpool workpool, Project project, Capturer capturer, Status status, String logFile) {
      this.workpoolInstance = workpool;
      this.project = project;
      this.capturer = capturer;
      this.status = status;
      this.logFile = logFile;
   }

   public void start() {
      runningPhase = executor.submit(new RequestRunner());
   }

   public Status getStatus() {
      // XXX: We might should have a value object that we return to callers and
      // have Status as private-only due to various synchronization issues.
      return status;
   }

   public void next(State state) {
      // XXX: Need to make sure this can't make backwards progress.
      switch (state) {
         case preCaptureWait:
            status.setCurrentState(State.preCaptureDone);
            break;

         case installationWait:
            status.setCurrentState(State.installationDone);
            break;

         case needsLoginWait:
            status.setCurrentState(State.needsLoginDone);
            break;

         case created:
         case acquiringVm:
         case vmAcquired:
         case poweringOnVm:
         case waitingForTools:
         case needsLoginDone:
         case installingThinApp:
         case downloading:
         case mountingFileSharesToGuest:
         case preCaptureDone:
         case takingPreCaptureSnapshot:
         case preInstallationWait:
         case preInstallationDone:
         case installationDone:
         case postInstallationWait:
         case postInstallationDone:
         case takingPostCaptureSnapshot:
         case generatingProject:
         case preProjectBuildWait:
         case preProjectBuildDone:
         case buildingProject:
         case vmReleased:
         case refreshingProject:
         case cancelling:
         case refreshingProjectDone:
         case failure:
         case success:
         case cancelled:
         case installerDownloadFailed:
         case finished:
            throw new RuntimeException(
                  String.format("State %s is not nextable.", state));
      }
   }

   public void cancel() {
      log.info("A cancelation request was made.");
      status.setCurrentState(State.cancelling);

      if ( status.getStates().contains(State.vmAcquired) &&
          !status.getStates().contains(State.finished)) {
         /**
          *  Stop other running contexts (thread and processes)
          *  to avoid them blocked on I/O indefinitely.
          *  We should only do this under both of the conditions:
          *  1. AFTER the lease (VM) has been acquied. This is to
          *  ensure the lease reference is been received and
          *  can be used in the "finally" block of the
          *  RequestRunnder.call() method to release it.
          *  2. BEFORE the task becomes finished. This is to
          *  ensure that the cleanup procedure is not interrupted.
          */
         // Cancel the thread
         runningPhase.cancel(true);

         // Kill any processes that might be blocking on IO
         processMonitor.cancel(project.getId());
      }
   }
}
