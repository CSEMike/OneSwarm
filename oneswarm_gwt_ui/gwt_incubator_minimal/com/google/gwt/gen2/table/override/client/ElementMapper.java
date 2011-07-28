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

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Creates a mapping from elements to their associated ui objects.
 * 
 * @param <MappedType> the type that the element is mapped to
 */
public class ElementMapper<MappedType extends UIObject> {

  private static class FreeNode {
    int index;
    ElementMapper.FreeNode next;

    public FreeNode(int index, ElementMapper.FreeNode next) {
      this.index = index;
      this.next = next;
    }
  }

  private static native void clearIndex(Element elem) /*-{
    elem["__uiObjectID"] = null;
   }-*/;

  private static native int getIndex(Element elem) /*-{
    var index = elem["__uiObjectID"];
    return (index == null) ? -1 : index;
  }-*/;

  private static native void setIndex(Element elem, int index) /*-{
     elem["__uiObjectID"] = index;
  }-*/;

  private ElementMapper.FreeNode freeList = null;

  private final ArrayList<MappedType> uiObjectList = new ArrayList<MappedType>();

  /**
   * Returns the uiObject associated with the given element.
   * 
   * @param elem uiObject's element
   * @return the uiObject
   */
  public MappedType get(Element elem) {
    int index = getIndex(elem);
    if (index < 0) {
      return null;
    }
    return uiObjectList.get(index);
  }

  /**
   * Adds the MappedType.
   * 
   * @param uiObject uiObject to add
   */
  public void put(MappedType uiObject) {
    int index;
    if (freeList == null) {
      index = uiObjectList.size();
      uiObjectList.add(uiObject);
    } else {
      index = freeList.index;
      uiObjectList.set(index, uiObject);
      freeList = freeList.next;
    }
    setIndex(uiObject.getElement(), index);
  }

  /**
   * Remove the uiObject associated with the given element.
   * 
   * @param elem the uiObject's element
   */
  public void removeByElement(Element elem) {
    int index = getIndex(elem);
    removeImpl(elem, index);
  }

  /**
   * Creates an iterator of uiObjects.
   * 
   * @return the iterator
   */
  public Iterator<MappedType> iterator() {

    return new Iterator() {
      int lastIndex = -1;
      int nextIndex = -1;
      {
        findNext();
      }

      public boolean hasNext() {
        return nextIndex < uiObjectList.size();
      }

      public Object next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Object result = uiObjectList.get(nextIndex);
        lastIndex = nextIndex;
        findNext();
        return result;
      }

      public void remove() {
        if (lastIndex < 0) {
          throw new IllegalStateException();
        }
        Widget w = (Widget) uiObjectList.get(lastIndex);
        assert (w.getParent() instanceof HTMLTable);
        w.removeFromParent();
        lastIndex = -1;
      }

      private void findNext() {
        while (++nextIndex < uiObjectList.size()) {
          if (uiObjectList.get(nextIndex) != null) {
            return;
          }
        }
      }
    };
  }

  private void removeImpl(Element elem, int index) {
    clearIndex(elem);
    uiObjectList.set(index, null);
    freeList = new FreeNode(index, freeList);
  }
}