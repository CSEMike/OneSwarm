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
 * A {@link ColumnProperty} that provides the minimum width of a column.
 */
public class MinimumWidthProperty extends ColumnProperty {
  /**
   * The return value when no minimum width is specified.
   */
  public static final int NO_MINIMUM_WIDTH = -1;

  /**
   * Property type.
   */
  public static final Type<MinimumWidthProperty> TYPE = new Type<MinimumWidthProperty>() {
    private MinimumWidthProperty instance;

    @Override
    public MinimumWidthProperty getDefault() {
      if (instance == null) {
        instance = new MinimumWidthProperty(NO_MINIMUM_WIDTH);
      }
      return instance;
    }
  };

  private int minWidth;

  /**
   * Construct a new {@link MinimumWidthProperty}.
   * 
   * @param minWidth the minimum column width
   */
  public MinimumWidthProperty(int minWidth) {
    this.minWidth = minWidth;
  }

  /**
   * Get the minimum width of the column. A return value of
   * {@link #NO_MINIMUM_WIDTH} indicates that the column has no minimum width,
   * but the consumer of the data may impose one anyway.
   * 
   * @return the minimum allowable width of the column
   */
  public int getMinimumColumnWidth() {
    return minWidth;
  }
}
