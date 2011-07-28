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
package com.google.gwt.gen2.table.override.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * This class should be incorporated into the DOM class.
 * 
 * TODO: incorporate into DOM
 */
public class OverrideDOM extends DOM {
  private static OverrideDOMImpl impl2;

  static {
    impl2 = GWT.create(OverrideDOMImpl.class);
  }
  
  /**
   * Creates an HTML TD element.
   *
   * @return the newly-created element
   */
  public static Element createTD() {
    return impl2.createTD();
  }
  
  /**
   * Creates an HTML TH element.
   *
   * @return the newly-created element
   */
  public static Element createTH() {
    return impl2.createTH();
  }

  /**
   * Gets the cell index of a cell within a table row.
   *
   * @param td the cell element
   * @return the cell index
   */
  public static int getCellIndex(Element td) {
    return impl2.getCellIndex(td);
  }
  
  /**
   * Gets the row index of a row element.
   *
   * @param tr the row element
   * @return the row index
   */
  public static int getRowIndex(Element tr) {
    return DOM.getElementPropertyInt(tr, "rowIndex");
  }
}
