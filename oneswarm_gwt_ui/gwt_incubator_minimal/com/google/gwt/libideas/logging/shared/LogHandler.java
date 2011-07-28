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
 * A <tt>Handler</tt> object takes log messages from Logger and publishes them.
 * It is a clone of the Java Logging API. The javadoc below are from the Sun
 * implementation.
 * 
 * All handler setup should be guarded by a call to Logger.isLoggingEnabled()
 * 
 * A <tt>Handler</tt> can be disabled by doing a <tt>setLevel(Level.OFF)</tt>
 * and can be re-enabled by doing a <tt>setLevel</tt> with an appropriate level.
 * <p>
 * 
 * @version 1.17, 01/12/04
 * @since 1.4
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public abstract class LogHandler {
  private Level logLevel;

  /**
   * Clears the handler if clear is supported.
   */
  public void clear() {
  }

  /**
   * 
   * /** Get the log level specifying which messages will be logged by this
   * <tt>Handler</tt>. Message levels lower than this level will be discarded.
   * 
   * @return the level of messages being logged.
   */
  public Level getLevel() {
    if (logLevel == null) {
      return Level.ALL;
    }
    return logLevel;
  }

  /**
   * Hides the handler, if hiding is supported.
   */
  public void hideHandler() {
  }

  /**
   * Is this handler supported in the given compilation configuration? By
   * default the answer is "yes".
   * 
   * @return is the handler supported;
   */
  public boolean isSupported() {
    return true;
  }

  /**
   * publish the message.
   * 
   * @param message the message
   * @param level the message's level
   * @param category optional category
   * @param e optional throwable
   */
  public abstract void publish(String message, Level level, String category,
      Throwable e);

  /**
   * Set the log level specifying which message levels will be logged by this
   * <tt>Handler</tt>. Message levels lower than this value will be discarded.
   * <p>
   * The intention is to allow developers to turn on voluminous logging, but to
   * limit the messages that are sent to certain <tt>Handlers</tt>.
   * 
   * @param newLevel the new value for the log level
   * 
   */
  public void setLevel(Level newLevel) {
    logLevel = newLevel;
  }

  /**
   * A standard way to present category and level information to string output.
   */
  protected String format(String message, Level level, String category,
      Throwable e) {
    StringBuffer accum = new StringBuffer(level.getName());
    if (category != null) {
      accum.append("-" + category);
    }
    accum.append(": " + message);
    if (e != null) {
      accum.append("\n" + e.toString());
    }
    return accum.toString();
  }

}
