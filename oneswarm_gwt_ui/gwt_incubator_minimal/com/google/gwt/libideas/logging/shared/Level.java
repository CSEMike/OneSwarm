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

import java.io.Serializable;

/**
 * This is a stripped-down clone of the java level class. It does not support
 * all the functionality. The JavaDoc is imported directly from Sun. The LogImpl
 * in use sets these fields.
 * 
 * The logging levels are initialized by the Log class, so a method from Log
 * must be called before the level fields are used.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class Level implements Comparable, Serializable {
  /**
   * OFF is a special level that can be used to turn off logging. This level is
   * initialized to <CODE>Integer.MAX_VALUE</CODE>.
   */
  public static Level OFF;

  /**
   * SEVERE is a message level indicating a serious failure.
   * <p>
   * In general SEVERE messages should describe events that are of considerable
   * importance and which will prevent normal program execution. They should be
   * reasonably intelligible to end users and to system administrators. This
   * level is initialized to <CODE>1000</CODE>.
   */
  public static Level SEVERE;

  /**
   * WARNING is a message level indicating a potential problem.
   * <p>
   * In general WARNING messages should describe events that will be of interest
   * to end users or system managers, or which indicate potential problems. This
   * level is initialized to <CODE>900</CODE>.
   */
  public static Level WARNING;

  /**
   * INFO is a message level for informational messages.
   * <p>
   * Typically INFO messages will be written to the console or its equivalent.
   * So the INFO level should only be used for reasonably significant messages
   * that will make sense to end users and system admins. This level is
   * initialized to <CODE>800</CODE>.
   */
  public static Level INFO;

  /**
   * CONFIG is a message level for static configuration messages.
   * <p>
   * CONFIG messages are intended to provide a variety of static configuration
   * information, to assist in debugging problems that may be associated with
   * particular configurations. For example, CONFIG message might include the
   * CPU type, the graphics depth, the GUI look-and-feel, etc. This level is
   * initialized to <CODE>700</CODE>.
   */
  public static Level CONFIG;

  /**
   * FINE is a message level providing tracing information.
   * <p>
   * All of FINE, FINER, and FINEST are intended for relatively detailed
   * tracing. The exact meaning of the three levels will vary between
   * subsystems, but in general, FINEST should be used for the most voluminous
   * detailed output, FINER for somewhat less detailed output, and FINE for the
   * lowest volume (and most important) messages.
   * <p>
   * In general the FINE level should be used for information that will be
   * broadly interesting to developers who do not have a specialized interest in
   * the specific subsystem.
   * <p>
   * FINE messages might include things like minor (recoverable) failures.
   * Issues indicating potential performance problems are also worth logging as
   * FINE. This level is initialized to <CODE>500</CODE>.
   */
  public static Level FINE;

  /**
   * FINER indicates a fairly detailed tracing message. By default logging calls
   * for entering, returning, or throwing an exception are traced at this level.
   * This level is initialized to <CODE>400</CODE>.
   */
  public static Level FINER;

  /**
   * FINEST indicates a highly detailed tracing message. This level is
   * initialized to <CODE>300</CODE>.
   */
  public static Level FINEST;

  /**
   * ALL indicates that all messages should be logged. This level is initialized
   * to <CODE>Integer.MIN_VALUE</CODE>.
   */
  public static Level ALL;

  int value;

  String name;

  boolean isControl;

  /**
   * Constructor used for serialization.
   */
  public Level() {
    name = null;
    value = -1;
  }

  /**
   * Create a named Level with a given integer value.
   * <p>
   * Note you should always use a guarded method to create a new level.
   * 
   * @param name the name of the Level, for example "SEVERE".
   * @param value an integer value for the level.
   * @throws NullPointerException if the name is null
   */
  public Level(String name, int value) {
    this.value = value;
    this.name = name;
    Log.registerLevel(this);
  }

  /**
   * Create a named Level with a given integer value.
   * <p>
   * Note you should always use a guarded method to create a new level.
   * 
   * @param name the name of the Level, for example "SEVERE".
   * @param value an integer value for the level.
   * @param isControl is a control log level, usually "ALL" or "OFF".
   * @throws NullPointerException if the name is null
   */
  public Level(String name, int value, boolean isControl) {
    this(name, value);
    this.isControl = isControl;
  }

  public int compareTo(Object o) {
    Level that = (Level) o;
    if (value < that.value) {
      return -1;
    } else if (value > that.value) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Compare two objects for value equality.
   * 
   * @return true if and only if the two objects have the same level value.
   */
  public boolean equals(Object ox) {
    try {
      Level lx = (Level) ox;
      return (lx.value == this.value);
    } catch (Exception ex) {
      return false;
    }
  }

  public String getName() {
    return name;
  }

  /**
   * Generate a hashcode.
   * 
   * @return a hashcode based on the level value
   */
  public int hashCode() {
    return this.value;
  }

  /**
   * Get the integer value for this level. This integer value can be used for
   * efficient ordering comparisons between Level objects.
   * 
   * @return the integer value for this level.
   */
  public final int intValue() {
    return value;
  }

  public boolean isControl() {
    return isControl;
  }

  public String toString() {
    return getName();
  }

}
