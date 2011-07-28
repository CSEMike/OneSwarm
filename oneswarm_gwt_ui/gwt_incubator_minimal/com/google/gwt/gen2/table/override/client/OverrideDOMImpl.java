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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * This class should be incorporated into the DOMImpl class.
 * 
 * TODO: Incorporate into DOMImpl.
 */
public class OverrideDOMImpl {
  public Element createTD() {
    return DOM.createElement("td");
  }
     
  public Element createTH() {
    return DOM.createElement("th");
  }
  
  /**
   * Gets the cell index of a cell within a table row.
   *
   * @param td the cell element
   * @return the cell index
   */
  public int getCellIndex(Element td) {
    return DOM.getElementPropertyInt(td, "cellIndex");
  }
}
