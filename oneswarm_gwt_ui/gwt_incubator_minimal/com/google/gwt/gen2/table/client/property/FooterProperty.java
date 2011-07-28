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
 * A {@link ColumnProperty} that provides information about the footers below a
 * column. The row indexes start from the top of the footer, such that all
 * footers at the 0th index refer to the row directly below the data table.
 */
public class FooterProperty extends HeaderPropertyBase {
  /**
   * Property type.
   */
  public static final Type<FooterProperty> TYPE = new Type<FooterProperty>() {
    private FooterProperty instance;

    @Override
    public FooterProperty getDefault() {
      if (instance == null) {
        instance = new FooterProperty();
      }
      return instance;
    }
  };

  /**
   * Get the footer at the given row index.
   * 
   * @param row the row index from the top
   * @return the footer for the given row
   */
  public Object getFooter(int row) {
    return super.getHeader(row);
  }

  /**
   * Get the footer at the given row and column index. Override this method if
   * your footer includes dynamic content that depends on the column index.
   * 
   * @param row the row index from the top
   * @param column the column index at runtime
   * @return the footer for the given row
   */
  public Object getFooter(int row, int column) {
    return getFooter(row);
  }

  /**
   * @return get the number of footers below the column
   */
  public int getFooterCount() {
    return super.getHeaderCount();
  }

  /**
   * Remove the footer below this column for the specified row.
   * 
   * @param row the row index from the top
   */
  public void removeFooter(int row) {
    super.removeHeader(row);
  }

  /**
   * Set the footer below this column. The row index starts with the top row,
   * such that index 0 is directly below the data table. The footerCount will
   * automatically be increased to accommodate the row.
   * 
   * @param row the row index from the top
   * @param footer the footer
   */
  public void setFooter(int row, Object footer) {
    super.setHeader(row, footer);
  }

  /**
   * Set the number of footers below the column.
   * 
   * @param footerCount the number of footers
   */
  public void setFooterCount(int footerCount) {
    super.setHeaderCount(footerCount);
  }
}
