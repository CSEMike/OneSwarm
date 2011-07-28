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
 * A simple property of a column.
 */
public abstract class ColumnProperty {
  /**
   * Type class used to register properties.
   */
  public abstract static class Type<P extends ColumnProperty> {
    private static int nextHashCode;
    private final int index;

    /**
     * Construct a new type.
     */
    public Type() {
      index = ++nextHashCode;
    }

    /**
     * Get the default property value of this type. This method should never
     * return null.
     * 
     * @return the default (non null) property value
     */
    public abstract P getDefault();

    @Override
    public final int hashCode() {
      return index;
    }
  }
}
