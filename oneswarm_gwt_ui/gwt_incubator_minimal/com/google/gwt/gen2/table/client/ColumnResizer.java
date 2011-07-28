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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * A helper class that distributes available width across a set of columns.
 * </p>
 * 
 * <h3>The following algorithm is used to distribute the available width:</h3>
 * <ol>
 * <li>Calculate the percent difference between the current and preferred width
 * of each column</li>
 * <li>Order the columns in descending order according to their percent
 * differences from preferred width</li>
 * <li>For each iteration, distribute width into the first n columns such that
 * their percent difference now equals that of the n+1 column</li>
 * <li>If a column hits its minimum or maximum size, remove it from the list of
 * columns</li>
 * <li>If there are no more columns that can accept more/less width (they are
 * all at their boundaries), return the undistributed width.</li>
 * </ol>
 */
class ColumnResizer {
  /**
   * The resolution of the width of columns. The true target width of a column
   * is usually a decimal, but column widths can only be represented as integers
   * (ie. pixels). The resolution determines the maximum number of pixels that
   * the calculations can be off by.
   * 
   * Increasing the resolution will increase the speed of the algorithm and
   * reduce the accuracy of the calculations. The resolution must be at least 1
   * or errors will occur (because we cannot get perfect "0" accuracy).
   */
  private static final int RESOLUTION = 1;

  /**
   * A class that contains the current and desired width of a column.
   */
  static class ColumnWidthInfo {
    private int minWidth;
    private int maxWidth;
    private int preferredWidth;
    private int curWidth;

    /**
     * The new column width.
     */
    private int newWidth = 0;

    /**
     * The required width to achieve the next level.
     */
    private int requiredWidth;

    /**
     * Construct a new {@link ColumnWidthInfo}.
     * 
     * @param minWidth the minimum width of the column
     * @param maxWidth the maximum width of the column
     * @param preferredWidth the preferred width of the column
     * @param curWidth the current width of the column
     */
    public ColumnWidthInfo(int minWidth, int maxWidth, int preferredWidth,
        int curWidth) {
      this.minWidth = minWidth;
      this.maxWidth = maxWidth;
      this.preferredWidth = preferredWidth;
      this.curWidth = curWidth;
    }

    public int getCurrentWidth() {
      return curWidth;
    }

    public int getMaximumWidth() {
      // For calculation purposes, ensure maxWidth >= minWidth
      if (hasMaximumWidth()) {
        return Math.max(maxWidth, minWidth);
      }
      return maxWidth;
    }

    public int getMinimumWidth() {
      return minWidth;
    }

    public int getNewWidth() {
      return newWidth;
    }

    public int getPreferredWidth() {
      return preferredWidth;
    }

    public boolean hasMaximumWidth() {
      return maxWidth >= 0;
    }

    public boolean hasMinimumWidth() {
      return minWidth >= 0;
    }

    public void setCurrentWidth(int curWidth) {
      this.curWidth = curWidth;
    }

    public void setMaximumWidth(int maxWidth) {
      this.maxWidth = maxWidth;
    }

    public void setMinimumWidth(int minWidth) {
      this.minWidth = minWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
      this.preferredWidth = preferredWidth;
    }

    /**
     * Get the percentage difference between the current column width and the
     * preferred column width. A negative value indicates that the current width
     * is less than the preferred width, while a positive value indicates that
     * the current width is above the preferred width.
     * 
     * @return the percentage difference
     */
    double getPercentageDifference() {
      return (newWidth - preferredWidth) / (double) preferredWidth;
    }

    int getRequiredWidth() {
      return requiredWidth;
    }

    void setNewWidth(int newWidth) {
      this.newWidth = newWidth;
    }

    void setRequiredWidth(int requiredWidth) {
      this.requiredWidth = requiredWidth;
    }
  }

  /**
   * Distribute some width across a list of columns, respecting the minimum and
   * maximum widths of the columns. The return value is the remaining width that
   * could not be distributed due to constraints. If the return value is 0, all
   * of the width has been distributed.
   * 
   * @param columns the list of column width info
   * @param width the width to distribute
   * @return the width that could not be distributed
   */
  public int distributeWidth(List<ColumnWidthInfo> columns, int width) {
    // The new width defaults to the current width, within min/max range
    for (ColumnWidthInfo info : columns) {
      int curWidth = info.getCurrentWidth();
      if (info.hasMinimumWidth() && curWidth < info.getMinimumWidth()) {
        curWidth = info.getMinimumWidth();
      } else if (info.hasMaximumWidth() && curWidth > info.getMaximumWidth()) {
        curWidth = info.getMaximumWidth();
      }
      width -= (curWidth - info.getCurrentWidth());
      info.setNewWidth(curWidth);
    }

    // Do not modify widths if there is nothing to distribute
    if (width == 0) {
      return 0;
    }

    // Copy the list of columns
    List<ColumnWidthInfo> orderedColumns = new ArrayList<ColumnWidthInfo>(
        columns);

    // Sort the list of columns
    if (width > 0) {
      // Enlarge columns
      Comparator<ColumnWidthInfo> comparator = new Comparator<ColumnWidthInfo>() {
        public int compare(ColumnWidthInfo o1, ColumnWidthInfo o2) {
          double diff1 = o1.getPercentageDifference();
          double diff2 = o2.getPercentageDifference();
          if (diff1 < diff2) {
            return -1;
          } else if (diff1 == diff2) {
            return 0;
          } else {
            return 1;
          }
        }
      };
      Collections.sort(orderedColumns, comparator);
    } else if (width < 0) {
      // Shrink columns
      Comparator<ColumnWidthInfo> comparator = new Comparator<ColumnWidthInfo>() {
        public int compare(ColumnWidthInfo o1, ColumnWidthInfo o2) {
          double diff1 = o1.getPercentageDifference();
          double diff2 = o2.getPercentageDifference();
          if (diff1 > diff2) {
            return -1;
          } else if (diff1 == diff2) {
            return 0;
          } else {
            return 1;
          }
        }
      };
      Collections.sort(orderedColumns, comparator);
    }

    // Distribute the width
    return distributeWidthImpl(orderedColumns, width);
  }

  private int distributeWidthImpl(List<ColumnWidthInfo> columns, int width) {
    // Iterate until width can not longer be distributed
    boolean growing = (width > 0);
    boolean fullySynced = false;
    int syncedColumns = 1;
    while (columns.size() > 0 && width != 0) {
      // Calculate the target difference at the next level
      double targetDiff = getTargetDiff(columns, syncedColumns, width);

      // Calculate the total required width to achieve the target difference
      int totalRequired = 0;
      for (int curIndex = 0; curIndex < syncedColumns; curIndex++) {
        // Calculate the new width at the target diff
        ColumnWidthInfo curInfo = columns.get(curIndex);
        int preferredWidth = curInfo.getPreferredWidth();
        int newWidth = (int) (targetDiff * preferredWidth) + preferredWidth;

        // Compare the boundaries
        if (growing) {
          newWidth = Math.max(newWidth, curInfo.getCurrentWidth());
          if (curInfo.hasMaximumWidth()) {
            newWidth = Math.min(newWidth, curInfo.getMaximumWidth());
          }
        } else {
          newWidth = Math.min(newWidth, curInfo.getCurrentWidth());
          if (curInfo.hasMinimumWidth()) {
            newWidth = Math.max(newWidth, curInfo.getMinimumWidth());
          }
        }

        // Calculate the width required to achieve the new width
        curInfo.setRequiredWidth(newWidth - curInfo.getNewWidth());
        totalRequired += curInfo.getRequiredWidth();
      }

      // Calculate the percent of the required width that is available
      double percentAvailable = 1.0;
      if (totalRequired != 0) {
        percentAvailable = Math.min(1.0, width / (double) totalRequired);
      }
      for (int curIndex = 0; curIndex < syncedColumns; curIndex++) {
        // Determine the true width to add to the column
        ColumnWidthInfo curInfo = columns.get(curIndex);
        int required = (int) (percentAvailable * curInfo.getRequiredWidth());

        // Make sure we get out of the loop by distributing at least 1
        if (fullySynced) {
          if (growing) {
            required = Math.max(RESOLUTION, required);
          } else {
            required = Math.min(-RESOLUTION, required);
          }
        }

        // Don't distribute more than the available width
        if (growing && required > width) {
          required = width;
        } else if (!growing && required < width) {
          required = width;
        }

        // Set the new width of the column
        curInfo.setNewWidth(curInfo.getNewWidth() + required);
        width -= required;

        // Remove the column if it has reached its maximum/minimum width
        boolean maxedOut = false;
        if (growing && curInfo.hasMaximumWidth()) {
          maxedOut = (curInfo.getNewWidth() >= curInfo.getMaximumWidth());
        } else if (!growing && curInfo.hasMinimumWidth()) {
          maxedOut = (curInfo.getNewWidth() <= curInfo.getMinimumWidth());
        }
        if (maxedOut) {
          columns.remove(curIndex);
          curIndex--;
          syncedColumns--;
        }
      }

      // Increment the number of synced column
      if (!fullySynced && syncedColumns < columns.size()) {
        syncedColumns++;
      } else {
        fullySynced = true;
      }
    }

    // Return the undistributed width
    return width;
  }

  /**
   * Calculate the target percentage difference of the next level.
   * 
   * @param columns the column width info
   * @param syncedColumns the number of synced columns
   * @param width the width to distribute
   */
  private double getTargetDiff(List<ColumnWidthInfo> columns,
      int syncedColumns, int width) {
    if (syncedColumns < columns.size()) {
      // Use the diff of the next un-synced column as the target
      return columns.get(syncedColumns).getPercentageDifference();
    } else {
      // Calculate the total diff after all width has been distributed
      int totalNewWidth = width;
      int totalPreferredWidth = 0;
      for (ColumnWidthInfo info : columns) {
        totalNewWidth += info.getNewWidth();
        totalPreferredWidth += info.getPreferredWidth();
      }
      return (totalNewWidth - totalPreferredWidth)
          / (double) totalPreferredWidth;
    }
  }
}
