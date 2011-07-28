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
 * column can be truncated.
 */
public class TruncationProperty extends ColumnProperty {
  /**
   * Property type.
   */
  public static final Type<TruncationProperty> TYPE = new Type<TruncationProperty>() {
    private TruncationProperty instance;

    @Override
    public TruncationProperty getDefault() {
      if (instance == null) {
        instance = new TruncationProperty(true);
      }
      return instance;
    }
  };

  private boolean isTruncatable;
  private boolean isFooterTruncatable;
  private boolean isHeaderTruncatable;

  /**
   * Construct a new {@link TruncationProperty}.
   */
  public TruncationProperty() {
    this(true);
  }

  /**
   * Construct a new {@link TruncationProperty}.
   * 
   * @param isTruncatable true if the column is truncatable, false if not
   */
  public TruncationProperty(boolean isTruncatable) {
    this.isTruncatable = isTruncatable;
    this.isHeaderTruncatable = true;
    this.isFooterTruncatable = true;
  }

  /**
   * @return true (default) if the column is truncatable
   */
  public boolean isColumnTruncatable() {
    return isTruncatable;
  }

  /**
   * @return true (default) if column in the footer is truncatable
   */
  public boolean isFooterTruncatable() {
    return isFooterTruncatable;
  }

  /**
   * @return true (default) if column in the header is truncatable
   */
  public boolean isHeaderTruncatable() {
    return isHeaderTruncatable;
  }

  /**
   * Set whether or not column are truncatable.
   * 
   * @param isTruncatable true to make truncatable
   */
  public void setColumnTruncatable(boolean isTruncatable) {
    this.isTruncatable = isTruncatable;
  }

  /**
   * Set whether or not the column in the footer is truncatable.
   * 
   * @param isTruncatable true to make truncatable
   */
  public void setFooterTruncatable(boolean isTruncatable) {
    this.isFooterTruncatable = isTruncatable;
  }

  /**
   * Set whether or not the column in the header is truncatable.
   * 
   * @param isTruncatable true to make truncatable
   */
  public void setHeaderTruncatable(boolean isTruncatable) {
    this.isHeaderTruncatable = isTruncatable;
  }
}
