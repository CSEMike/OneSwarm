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

import com.google.gwt.core.client.GWT;
import com.google.gwt.libideas.logging.shared.impl.LogImpl;

import java.util.Iterator;

/**
 * This class is very loosely based Java's Logger class. It is static to allow
 * differed binding to remove logging information in production code.
 * 
 * By default the logging level is set to {@link Level.CONFIG}.
 * 
 * If you wish to use this class in code that may not be run in a GWT
 * environment, you must instantiate on of the system logging adaptors available
 * from {@link com.google.gwt.libideas.logging.server.SystemLogSystemLog}.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class Log {
  public static final String CATEGORY = "gwt.logging";
  private static LogImpl impl;

  static {
    try {
      impl = GWT.create(LogImpl.class);
    } catch (UnsupportedOperationException ex) {
      // You must be in server mode;
    }
  }

  /**
   * Add a log Handler to receive logging messages.
   * <p>
   * If no handlers are supplied and a message should be logged, it will be
   * logging to the GWT console in hosted mode and the JavaScript console in web
   * mode.
   * 
   * @param handler the new handler
   * @return whether the handler was added.
   * 
   */
  public static boolean addLogHandler(LogHandler handler) {
    return impl.addLogHandler(handler);
  }

  /**
   * Clear all handlers.
   */

  public static void clearLogHandlers() {
    impl.clearLogHandlers();
  }

  /**
   * Log a CONFIG message.
   * 
   */
  public static void config(String msg) {
    config(msg, null);
  }

  /**
   * Log a CONFIG message.
   */
  public static void config(String msg, String category) {
    impl.config(msg, category);
  }

  /**
   * Log a FINE message.
   */
  public static void fine(String msg) {
    fine(msg, null);
  }

  /**
   * Log a FINE message.
   */
  public static void fine(String msg, String category) {
    impl.fine(msg, category);
  }

  /**
   * Log a FINER message.
   */
  public static void finer(String msg) {
    finer(msg, null);
  }

  /**
   * Log a FINER message.
   */
  public static void finer(String msg, String category) {
    impl.finer(msg, category);
  }

  /**
   * Log a FINEST message.
   */
  public static void finest(String msg) {
    finest(msg, null);
  }

  /**
   * Log a FINEST message.
   */
  public static void finest(String msg, String category) {
    impl.finest(msg, category);
  }

  /**
   * Get the default log level needed to publish messages. This can be
   * overridden by the settings per category.
   * 
   * @return this Logger's level
   */
  public static Level getDefaultLevel() {
    return impl.getDefaultLevel();
  }

  /**
   * Log an INFO message.
   */
  public static void info(String msg) {
    info(msg, null);
  }

  /**
   * Log an INFO message.
   */
  public static void info(String msg, String category) {
    impl.info(msg, category);
  }

  /**
   * Check if a message of the given level would actually be logged by this
   * logger. This check is based on the Loggers effective level, which may be
   * inherited from its parent.
   * 
   * @param level a message logging level
   * @return true if the given message level is currently being logged.
   */
  public static boolean isLoggable(Level level) {
    return impl.isLoggable(level);
  }

  /**
   * Is the logging system in minimal mode? i.e. only severe logging messages
   * are logged and
   */
  public static boolean isLoggingMinimal() {
    return impl.isLoggingMinimal();
  }

  /**
   * Is Logging supported?
   */
  public static boolean isLoggingSupported() {
    return impl.isLoggingSupported();
  }

  /**
   * Returns an iterator of all currently defined levels.
   */
  public static Iterator levelIterator() {
    return impl.levelIterator();
  }

  /**
   * Logs a message using a given level. This method cannot be removed in
   * minimal logging mode, so use it only if you cannot use one of the static
   * logging messages.
   */

  public static void log(String msg, Level level, String category, Throwable e) {
    impl.log(msg, level, category, e);
  }

  /**
   * Return the correct level based on name.
   * 
   * @throws IllegalArgumentException for unknown levels.
   */

  public static Level parseLevel(String levelName) {
    return impl.parse(levelName);
  }

  /**
   * Remove a log Handler.
   * <P>
   * Returns silently if the given Handler is not found or is null
   * 
   * @param handler a logging Handler
   * @exception SecurityException if a security manager exists and if the caller
   *              does not have LoggingPermission("control").
   */
  public static void removeLogHandler(LogHandler handler) {
    impl.removeLogHandler(handler);
  }

  /**
   * Set the default log level specifying which message levels will be logged by
   * this logger.
   */
  public static void setDefaultLevel(Level newLevel) {
    impl.setDefaultLevel(newLevel);
  }

  /**
   * Sets the level of a given logging category. This level overrides the
   * default logging level.
   * 
   * @param category category
   * @param level level
   */
  public static void setLevel(String category, Level level) {
    impl.setLevel(category, level);
  }

  /**
   * Log a SEVERE message.
   * <p>
   * If the logger is currently enabled for the SEVERE message level then the
   * given message is forwarded to all the registered output Handler objects.
   * <p>
   * 
   * @param msg The string message
   */
  public static void severe(String msg) {
    severe(msg, null);
  }

  /**
   * Log a SEVERE message.
   */
  public static void severe(String msg, String category) {
    severe(msg, category, null);
  }

  /**
   * Log a SEVERE message.
   */
  public static void severe(String msg, String category, Throwable t) {
    impl.severe(msg, category, t);
  }

  /**
   * Splits the category into its component parts, separated by the "."
   * character.
   */
  public static String[] splitCategory(String category) {
    return impl.splitCategory(category);
  }

  /**
   * Log a WARNING message.
   * <p>
   * If the logger is currently enabled for the WARNING message level then the
   * given message is forwarded to all the registered output Handler objects.
   * <p>
   * 
   * @param msg The string message
   */
  public static void warning(String msg) {
    warning(msg, null);
  }

  /**
   * Log a WARNING message.
   */
  public static void warning(String msg, String category) {
    warning(msg, category, null);
  }

  /**
   * Log a WARNING message.
   */
  public static void warning(String msg, String category, Throwable e) {
    impl.warning(msg, category, e);
  }

  /**
   * Gets the current LogImpl class. Should only be used by classes directly
   * extending the logging system.
   */
  protected static LogImpl getLogImpl() {
    return impl;
  }

  protected static void initLogImpl(LogImpl impl) {
    Log.impl = impl;
  }

  static void registerLevel(Level level) {
    impl.registerLevel(level);
  }

  /**
   * Installs an uncaught exception handler that logs messages under the
   * "uncaught" category.
   */
  public final void installUncaughtExceptionHandler() {
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        Log.severe(e.getMessage(), "uncaught", e);
      }
    });
  }

}