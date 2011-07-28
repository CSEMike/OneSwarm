/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.libideas.logging.shared;

/**
 * {@link TimingLog} allows users to quickly annotate their code with timing
 * information.
 * 
 * As an aside, the best first step of profiling most web applications is to run
 * your app using FireFox's firebug profiler. Only when that fails would you
 * need the {@link TimingLog}.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class TimingLog {
  public static final String CATEGORY = "gwt.timing";
  private static long time;
  private static long lastChecked;
  private static boolean currentlyTiming;

  /**
   * Logs the current timing.
   * 
   * @param message the message to print with the timing
   */
  public static void logTiming(String message) {
    if (Log.isLoggingSupported() && !Log.isLoggingMinimal()) {
      if (currentlyTiming) {
        if (lastChecked == 0) {
          Log.info(message + ": time " + (System.currentTimeMillis() - time)
              + "ms", CATEGORY);
          lastChecked = System.currentTimeMillis();
        } else {
          long systemTime = System.currentTimeMillis();
          Log.info(message + ": time " + (systemTime - lastChecked) + "ms of "
              + (systemTime - time) + "ms", CATEGORY);
          lastChecked = System.currentTimeMillis();
        }
      }
    }
  }

  /**
   * Starts the current round of timing.
   */
  public static void startTiming() {
    if (Log.isLoggingSupported() && !Log.isLoggingMinimal()) {
      time = System.currentTimeMillis();
      currentlyTiming = true;
      lastChecked = 0;
    }
  }

  /**
   * Ends the current round of timing.
   */
  public void endTiming() {
    currentlyTiming = false;
  }
}
