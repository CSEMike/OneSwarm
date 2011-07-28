/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.gen2.table.client.property;

/**
 * A {@link ColumnProperty} that provides the maximum width of a column.
 */
public class MaximumWidthProperty extends ColumnProperty {
  /**
   * The return value when no maximum width is specified.
   */
  public static final int NO_MAXIMUM_WIDTH = -1;

  /**
   * Property type.
   */
  public static final Type<MaximumWidthProperty> TYPE = new Type<MaximumWidthProperty>() {
    private MaximumWidthProperty instance;

    @Override
    public MaximumWidthProperty getDefault() {
      if (instance == null) {
        instance = new MaximumWidthProperty(NO_MAXIMUM_WIDTH);
      }
      return instance;
    }
  };

  private int maxWidth;

  /**
   * Construct a new {@link MaximumWidthProperty}.
   * 
   * @param maxWidth the maximum column width
   */
  public MaximumWidthProperty(int maxWidth) {
    this.maxWidth = maxWidth;
  }

  /**
   * Get the maximum width of the column. A return value of
   * {@link #NO_MAXIMUM_WIDTH} indicates that the column has no maximum width,
   * but the consumer of the data may impose one anyway.
   * 
   * @return the maximum allowable width of the column
   */
  public int getMaximumColumnWidth() {
    return maxWidth;
  }
}
