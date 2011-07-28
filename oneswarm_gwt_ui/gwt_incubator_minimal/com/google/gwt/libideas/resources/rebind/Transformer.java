/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.libideas.resources.rebind;

import com.google.gwt.core.ext.typeinfo.JMethod;

import java.lang.reflect.Method;

/**
 * Applies user-defined transformations to the contents of a Resource at
 * compile-time. Add one or more <code>gwt.transformer</code> annotations to
 * the resource declaration. The Transformers will be applied in the order in
 * which they are declared. The Transformer does not need to be included in the
 * module's source path, and may therefore take advantage of non-translatable
 * code.
 * 
 * @param <T> The type of value on which the Transformer operates
 * @deprecated with no replacement
 */
@Deprecated
public abstract class Transformer<T> {

  /**
   * Ensures that the Transformer is capable of transforming the requested type.
   * This method allows consumers of Transformer subclasses to not need generic
   * casts.
   * 
   * @param <X> The desired type of object
   * @param type The type of the objects that the caller wishes to transform
   * @return the instance of the Transformer if the Transformer can accept
   *         <code>X</code>
   * @throws ClassCastException if the Transformer cannot be cast to the desired
   *           subtype.
   */
  @SuppressWarnings("unchecked")
  public final <X> Transformer<X> asSubclass(Class<X> type) {
    try {
      Method m = getClass().getMethod("transform", JMethod.class, type);
      if (type.equals(m.getReturnType())) {
        return (Transformer<X>) this;
      }
    } catch (NoSuchMethodException e) {
    }
    
    throw new ClassCastException("No method will accept " + type.getName());
  }

  /**
   * Perform the transformation.
   * 
   * @param method the JMethod for which the TextResource is being generated
   * @param input the original value of the resource.
   * @return the replacement value to use
   */
  public abstract T transform(JMethod method, T input);
}