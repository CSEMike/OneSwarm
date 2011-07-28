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
 * A {@link ColumnProperty} that provides the preferred width of a column.
 */
public class PreferredWidthProperty extends ColumnProperty {
  /**
   * Property type.
   */
  public static final Type<PreferredWidthProperty> TYPE = new Type<PreferredWidthProperty>() {
    private PreferredWidthProperty instance;

    @Override
    public PreferredWidthProperty getDefault() {
      if (instance == null) {
        instance = new PreferredWidthProperty(80);
      }
      return instance;
    }
  };

  private int preferredWidth;

  /**
   * Construct a new {@link PreferredWidthProperty}.
   * 
   * @param preferredWidth the preferred column width
   */
  public PreferredWidthProperty(int preferredWidth) {
    this.preferredWidth = preferredWidth;
  }
  
  /**
   * Returns the preferred width of the column in pixels. Views should respect
   * the preferred column width and attempt to size the column to its preferred
   * width. If the column must be resized, the preferred width should serve as a
   * weight relative to the preferred widths of other ColumnDefinitions.
   * 
   * @return the preferred width of the column
   */
  public int getPreferredColumnWidth() {
    return preferredWidth;
  }
}
