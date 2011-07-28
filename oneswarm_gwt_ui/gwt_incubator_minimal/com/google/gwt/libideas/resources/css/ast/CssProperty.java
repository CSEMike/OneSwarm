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
package com.google.gwt.libideas.resources.css.ast;

import com.google.gwt.core.ext.Generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Maps a named property to a Value.
 */
public class CssProperty extends CssNode {

  /**
   * Represents a sequence of no-arg method invocations.
   */
  public static class DotPathValue extends Value {
    private final String path;
    private final String suffix;

    public DotPathValue(String path, String suffix) {
      this.path = path;
      this.suffix = suffix;
    }

    public String getExpression() {
      return path.replace(".", "().") + "() + \"" + Generator.escape(suffix)
          + "\"";
    }

    public String getPath() {
      return path;
    }

    public String getSuffix() {
      return suffix;
    }

    @Override
    public DotPathValue isDotPathValue() {
      return this;
    }

    public String toCss() {
      return "value(\"" + path + "\""
          + (suffix == null ? "" : (", \"" + suffix + "\"")) + ")";
    }
  }

  /**
   * Represents a literal Java expression.
   */
  public static class ExpressionValue extends Value {
    private final String expression;

    public ExpressionValue(String expression) {
      this.expression = expression;
    }

    public String getExpression() {
      return expression;
    }

    @Override
    public ExpressionValue isExpressionValue() {
      return this;
    }

    public String toCss() {
      return "/* Java expression */";
    }

    /**
     * For debugging only.
     */
    @Override
    public String toString() {
      return expression;
    }
  }

  /**
   * Represents an identifier in the CSS source.
   */
  public static class IdentValue extends Value {
    private final String ident;

    public IdentValue(String ident) {
      this.ident = ident;
    }

    public String getExpression() {
      return '"' + Generator.escape(ident) + '"';
    }

    public String getIdent() {
      return ident;
    }

    @Override
    public IdentValue isIdentValue() {
      return this;
    }

    public String toCss() {
      return ident;
    }
  }

  /**
   * Represents a space-separated list of Values.
   */
  public static class ListValue extends Value {
    private final List<Value> values;

    public ListValue(List<Value> values) {
      this.values = Collections.unmodifiableList(new ArrayList<Value>(values));
    }

    public ListValue(Value... values) {
      this(Arrays.asList(values));
    }

    public String getExpression() {
      StringBuilder toReturn = new StringBuilder();
      for (Iterator<Value> i = values.iterator(); i.hasNext();) {
        toReturn.append(i.next().getExpression());
        if (i.hasNext()) {
          toReturn.append("+ \" \" +");
        }
      }
      return toReturn.toString();
    }

    public List<Value> getValues() {
      return values;
    }

    @Override
    public ListValue isListValue() {
      return this;
    }

    public String toCss() {
      StringBuilder sb = new StringBuilder();
      for (Value v : values) {
        sb.append(" ").append(v.toCss());
      }
      return sb.substring(1);
    }
  }

  /**
   * Represents a numeric value, possibly with attached units.
   */
  public static class NumberValue extends Value {
    private final String css;
    private final String expression;
    private final String units;
    private final float value;

    public NumberValue(float value) {
      this(value, null);
    }

    public NumberValue(float value, String units) {
      this.value = value;
      this.units = units;

      String s;
      int i = (int) value;
      if (i == value) {
        s = String.valueOf(i);
      } else {
        s = String.valueOf(value);
      }

      if (units != null && value != 0) {
        css = s + units;
        expression = '"' + s + Generator.escape(units) + '"';
      } else if (value == 0) {
        css = "0";
        expression = "\"0\"";
      } else {
        css = s;
        expression = '"' + s + '"';
      }
    }

    public String getExpression() {
      return expression;
    }

    public String getUnits() {
      return units;
    }

    public float getValue() {
      return value;
    }

    @Override
    public NumberValue isNumberValue() {
      return this;
    }

    public String toCss() {
      return css;
    }
  }

  /**
   * Represents one or more quoted string literals.
   */
  public static class StringValue extends Value {
    private static String escapeValue(String s, boolean inDoubleQuotes) {
      StringBuilder b = new StringBuilder();
      for (char c : s.toCharArray()) {
        if (Character.isISOControl(c)) {
          b.append('\\').append(Integer.toHexString(c).toUpperCase()).append(
              " ");
        } else {
          switch (c) {
            case '\'':
              // Special case a single quote in a pair of double quotes
              if (inDoubleQuotes) {
                b.append(c);
              } else {
                b.append("\\'");
              }
              break;

            case '"':
              // Special case a single quote in a pair of single quotes
              if (inDoubleQuotes) {
                b.append("\\\"");
              } else {
                b.append(c);
              }
              break;

            case '\\':
              b.append("\\\\");
              break;

            default:
              b.append(c);
          }
        }
      }

      return b.toString();
    }

    private final String value;

    public StringValue(String value) {
      this.value = value;
    }

    public String getExpression() {
      // The escaped CSS content has to be escaped to be a valid Java literal
      return "\"" + Generator.escape(toCss()) + "\"";
    }

    public String getValue() {
      return value;
    }

    @Override
    public StringValue isStringValue() {
      return this;
    }

    /**
     * Returns a escaped, quoted representation of the underlying value.
     */
    public String toCss() {
      return '"' + escapeValue(value, true) + '"';
    }
  }

  /**
   * An abstract encapsulation of property values in GWT CSS.
   */
  public abstract static class Value {
    /**
     * Generate a Java expression whose execution results in the value.
     */
    public abstract String getExpression();

    public DotPathValue isDotPathValue() {
      return null;
    }

    public ExpressionValue isExpressionValue() {
      return null;
    }

    public IdentValue isIdentValue() {
      return null;
    }

    public ListValue isListValue() {
      return null;
    }

    public NumberValue isNumberValue() {
      return null;
    }

    public StringValue isStringValue() {
      return null;
    }

    /**
     * Generate a CSS expression that represents the Value.
     */
    public abstract String toCss();

    /**
     * For debugging only.
     */
    @Override
    public String toString() {
      return toCss();
    }
  }

  private boolean important;

  private String name;
  private ListValue value;

  public CssProperty(String name, Value value, boolean important) {
    this.name = name;
    setValue(value);
    this.important = important;
  }

  public String getName() {
    return name;
  }

  public ListValue getValues() {
    return value;
  }

  public boolean isImportant() {
    return important;
  }

  public void setImportant(boolean important) {
    this.important = important;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setValue(Value value) {
    this.value = value.isListValue();
    if (this.value == null) {
      this.value = new ListValue(value);
    }
  }

  public void traverse(CssVisitor visitor, Context context) {
    visitor.visit(this, context);
    visitor.endVisit(this, context);
  }
}
