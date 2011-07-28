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
 * A {@link ColumnProperty} that describes whether or not the contents of the
 * column can be sorted.
 */
public class SortableProperty extends ColumnProperty {
  /**
   * Property type.
   */
  public static final Type<SortableProperty> TYPE = new Type<SortableProperty>() {
    private SortableProperty instance;

    @Override
    public SortableProperty getDefault() {
      if (instance == null) {
        instance = new SortableProperty(true);
      }
      return instance;
    }
  };

  private boolean isSortable;

  /**
   * Construct a new {@link SortableProperty}.
   * 
   * @param isSortable true if the column is sortable, false if not
   */
  public SortableProperty(boolean isSortable) {
    this.isSortable = isSortable;
  }

  /**
   * Returns true if the column is sortable, false if it is not.
   * 
   * @return true if the column is sortable, false if it is not sortable
   */
  public boolean isColumnSortable() {
    return isSortable;
  }
}
