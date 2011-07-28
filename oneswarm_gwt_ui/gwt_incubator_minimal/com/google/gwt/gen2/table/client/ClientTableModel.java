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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A {@link MutableTableModel} used when the data source can be accessed
 * synchronously.
 */
public abstract class ClientTableModel extends MutableTableModel<List<Object>> {
  /**
   * Convenience class to support custom iterators.
   */
  private abstract static class StubIterator<E> implements Iterator<E> {
    int index;
    E next;
    boolean done = false;

    protected abstract E computeNext();

    public boolean hasNext() {
      if (done) {
        return false;
      }
      if (next == null) {
        next = computeNext();
        if (next == null) {
          done = true;
          return false;
        }
      }
      return true;
    }

    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      } else {
        E accum = next;
        next = null;
        return accum;
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private class ColumnIterator extends StubIterator<Object> {
    private int row = 0;

    @Override
    public Object computeNext() {
      return getCell(row, index++);
    }
  }

  /**
   * An iterator over the rows of of client data.
   */
  private class RowIterator extends StubIterator<List<Object>> {
    private int max;

    public RowIterator(Request request) {
      index = request.getStartRow();
      if (request.getNumRows() == -1) {
        max = Integer.MAX_VALUE;
      } else {
        max = request.getNumRows() + index;
      }
    }

    @Override
    protected List<Object> computeNext() {
      // Reset column iterator rather than creating new one.
      final ColumnIterator colIt = new ColumnIterator();
      colIt.index = 0;
      colIt.row = index++;
      colIt.done = false;
      colIt.next = null;

      // Now check for next.
      if (colIt.hasNext() && colIt.row < max) {
        List<Object> next = new ArrayList<Object>();
        while (colIt.hasNext()) {
          next.add(colIt.next());
        }
        return next;
      } else {
        return null;
      }
    }
  }

  /**
   * Get the value for a given cell. Return null if no more values are
   * available.
   * 
   * @param rowNum the row index
   * @param colNum the column index
   * @return the value at the given row and column
   */
  public abstract Object getCell(int rowNum, int colNum);

  @Override
  public void requestRows(Request request, Callback<List<Object>> callback) {
    final RowIterator rowIter = new RowIterator(request);
    Response<List<Object>> response = new Response<List<Object>>() {
      @Override
      public Iterator<List<Object>> getRowValues() {
        return rowIter;
      }
    };
    callback.onRowsReady(request, response);
  }
}
