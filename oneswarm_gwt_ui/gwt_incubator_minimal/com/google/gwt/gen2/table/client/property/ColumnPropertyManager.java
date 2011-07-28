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

import com.google.gwt.gen2.table.client.property.ColumnProperty.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager responsible for adding, retrieving, and removing
 * {@link ColumnProperty}.
 */
public class ColumnPropertyManager {
  /**
   * A map of property types to their associated properties.
   */
  private Map<Type<?>, ColumnProperty> properties = new HashMap<Type<?>, ColumnProperty>();

  /**
   * <p>
   * Get the {@link ColumnProperty} associated with the specified
   * {@link ColumnProperty.Type}. If the property is not defined, the default
   * value will be returned.
   * </p>
   * <p>
   * This method should never return null. Instead, it should return the default
   * property from {@link ColumnProperty.Type#getDefault()}.
   * </p>
   * 
   * @param <P> the column property type
   * @param type the {@link ColumnProperty} type
   * @return the property, or the default value if the property is not defined
   */
  public <P extends ColumnProperty> P getColumnProperty(
      ColumnProperty.Type<P> type) {
    return getColumnProperty(type, true);
  }

  /**
   * Get the {@link ColumnProperty} associated with the specified
   * {@link ColumnProperty.Type}.
   * 
   * @param <P> the column property type
   * @param type the {@link ColumnProperty} type
   * @param useDefault if true, return the default property instead of null
   * @return the property, or the default value if the property is not defined
   */
  @SuppressWarnings("unchecked")
  public <P extends ColumnProperty> P getColumnProperty(
      ColumnProperty.Type<P> type, boolean useDefault) {
    Object property = properties.get(type);
    if (property == null && useDefault) {
      return type.getDefault();
    }
    return (P) property;
  }

  /**
   * Remove an existing {@link ColumnProperty} if it has already been added.
   * 
   * @param type the type of the property to remove
   * @return the removed property, or null if one was never set
   */
  @SuppressWarnings("unchecked")
  public <P extends ColumnProperty> P removeColumnProperty(
      ColumnProperty.Type<P> type) {
    Object property = properties.remove(type);
    if (property == null) {
      return null;
    }
    return (P) property;
  }

  /**
   * Set a {@link ColumnProperty}.
   * 
   * @param <P> the column property type
   * @param type the {@link ColumnProperty} type
   * @param property the property to set
   */
  public <P extends ColumnProperty> void setColumnProperty(
      ColumnProperty.Type<P> type, P property) {
    assert type != null : "Cannot add a property with a null type";
    assert property != null : "Cannot add a null property";
    properties.put(type, property);
  }
}
