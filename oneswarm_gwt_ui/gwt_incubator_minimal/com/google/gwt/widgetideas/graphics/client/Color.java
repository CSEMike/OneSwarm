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
package com.google.gwt.widgetideas.graphics.client;

/**
 * Simple Wrapper specifying a color in RGB format.
 * Provides various methods for converting to String representations
 * of the specified color for easy compatibility with various APIs
 */
public class Color {
  /*
   * Some basic color strings that are often used for the web.
   * Compiler should optimize these out if they are not used.
   */
  public static final Color ALPHA_GREY = new Color("rgba(0,0,0,0.3)");
  public static final Color ALPHA_RED = new Color("rgba(255,0,0,0.3)");
  public static final Color BLACK = new Color("#000000");
  public static final Color BLUE = new Color("#318ce0");
  public static final Color BLUEVIOLET = new Color("#8a2be2");
  public static final Color CYAN = new Color("#5fa2e0");
  public static final Color GREEN = new Color("#23ef24");
  public static final Color GREY = new Color("#a9a9a9");
  public static final Color LIGHTGREY = new Color("#eeeeee");
  public static final Color ORANGE = new Color("#f88247");
  public static final Color PEACH = new Color("#ffd393");
  public static final Color PINK = new Color("#ff00ff");
  public static final Color RED = new Color("#ff0000");
  public static final Color SKY_BLUE = new Color("#c6defa");
  public static final Color WHITE = new Color("#ffffff");
  public static final Color YELLOW = new Color("yellow");
  public static final Color DARK_ORANGE = new Color("#c44607");
  public static final Color BRIGHT_ORANGE = new Color("#fb5c0c");  
  public static final Color DARK_BLUE = new Color("#0c6ac1");
  
  private String colorStr = "";
  
  /**
   * Create a new Color object with the specified RGB 
   * values.
   * 
   * @param r red value 0-255
   * @param g green value 0-255
   * @param b blue value 0-255
   */
  public Color(int r, int g, int b) {
    this.colorStr = "rgb(" + r + "," + g + "," + b + ")";
  }
  
  /**
   * Create a new Color object with the specified RGBA 
   * values.
   * 
   * @param r red value 0-255
   * @param g green value 0-255
   * @param b blue value 0-255
   * @param a alpha channel value 0-1
   */
  public Color(int r, int g, int b, float a) {
    this.colorStr = "rgba(" + r + "," + g + "," + b + "," + a + ")";
  }
  
  /**
   * Create a Color using a valid CSSString. 
   * We do not do any validation so be careful!
   */
  public Color(String colorStr) {
    this.colorStr = colorStr;
  }
  
  public String toString() {
    return this.colorStr;
  }
}
