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
 * This class should be incorporated into the DOMImplSafari class.
 * 
 * TODO: incorporate into DOMImplSafari
 */
class OverrideDOMImplSafari extends OverrideDOMImpl {
  @Override
  public Element createTD() {
    Element td = super.createTD();
    DOM.setElementPropertyInt(td, "colSpan", 1);
    DOM.setElementPropertyInt(td, "rowSpan", 1);
    return td;
  }

  @Override
  public Element createTH() {
    Element th = super.createTH();
    DOM.setElementPropertyInt(th, "colSpan", 1);
    DOM.setElementPropertyInt(th, "rowSpan", 1);
    return th;
  }

  /**
   * Gets the cell index of a cell within a table row.
   * 
   * The cellIndex property is not defined in Safari, so we must calculate the
   * cell index manually.
   * 
   * @param td the cell element
   * @return the cell index
   */
  @Override
  public int getCellIndex(Element td) {
    return DOM.getChildIndex(DOM.getParent(td), td);
  }
}
