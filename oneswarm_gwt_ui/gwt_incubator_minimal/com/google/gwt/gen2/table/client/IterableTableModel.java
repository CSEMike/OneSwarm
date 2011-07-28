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
package com.google.gwt.gen2.table.client;

import com.google.gwt.gen2.table.client.TableModelHelper.Request;
import com.google.gwt.gen2.table.client.TableModelHelper.Response;

import java.util.Iterator;

/**
 * A class to retrieve row data to be used in a table.
 * 
 * @param <RowType> the data type of the row values
 */
public class IterableTableModel<RowType> extends TableModel<RowType> {
  /**
   * The values associated with each row.
   */
  private Iterable<RowType> rows;

  /**
   * Create a new {@link IterableTableModel}.
   * 
   * @param rows the values associated with each row.
   */
  public IterableTableModel(Iterable<RowType> rows) {
    this.rows = rows;
  }

  @Override
  public void requestRows(Request request, TableModel.Callback<RowType> callback) {

    callback.onRowsReady(request, new Response<RowType>() {
      @Override
      public Iterator<RowType> getRowValues() {
        return rows.iterator();
      }
    });
  }
}
