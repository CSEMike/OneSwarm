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
package com.google.gwt.libideas.resources.client;

import com.google.gwt.libideas.resources.ext.ResourceGeneratorType;
import com.google.gwt.libideas.resources.rg.ImageResourceGenerator;
import com.google.gwt.user.client.ui.Image;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Provides access to image resources at runtime.
 * <p>
 * <b>Note on deprecated methods:</b> In order to allow ImageResource to be used
 * with UI toolkits that are not derived from the standard GWT UI library, the
 * methods relating to {@link Image} have been deprecated in favor of exposing
 * the ImageResource's metadata in the public API.
 * 
 * @deprecated Superseded by
 *             {@link com.google.gwt.resources.client.ImageResource}
 */
@Deprecated
@ResourceGeneratorType(ImageResourceGenerator.class)
public interface ImageResource extends ResourcePrototype {

  /**
   * Specifies additional options to control how an image is bundled.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface ImageOptions {
    /**
     * This option affects the image bundling optimization to allow the image to
     * be used with the {@link CssResource} {@code @sprite} rule where
     * repetition of the image is desired.
     * 
     * @see "CssResource documentation"
     */
    RepeatStyle repeatStyle() default RepeatStyle.None;

    /**
     * If <code>true</code>, the image will be flipped along the y-axis when
     * {@link com.google.gwt.i18n.client.LocaleInfo#isRTL()} returns
     * <code>true</code>. This is intended to be used by graphics that are
     * sensitive to layout direction, such as arrows and disclosure indicators.
     */
    boolean flipRtl() default false;
  }

  /**
   * Indicates that an ImageResource should be bundled in such a way as to
   * support horizontal or vertical repetition.
   */
  public enum RepeatStyle {
    /**
     * The image is not intended to be tiled.
     */
    None,

    /**
     * The image is intended to be tiled horizontally.
     */
    Horizontal,

    /**
     * The image is intended to be tiled vertically.
     */
    Vertical,

    /**
     * The image is intended to be tiled both horizontally and vertically. Note
     * that this will prevent compositing of the particular image in most cases.
     */
    Both
  }

  /**
   * Transforms an existing {@link Image} into the image represented by this
   * prototype.
   * 
   * @param image the instance to be transformed to match this prototype
   * @deprecated Use
   *             {@link Image#setUrlAndVisibleRect(String, int, int, int, int)}
   *             instead.
   */
  @Deprecated
  void applyTo(Image image);

  /**
   * Creates a new {@link Image} instance based on the image represented by this
   * prototype.
   * 
   * @return a new <code>Image</code> based on this prototype
   * @deprecated Use
   *             {@link Image#setUrlAndVisibleRect(String, int, int, int, int)}
   *             instead.
   */
  @Deprecated
  Image createImage();

  /**
   * Returns the height of the image.
   */
  int getHeight();

  /**
   * Gets an HTML fragment that displays the image represented by this
   * prototype. The HTML returned is not necessarily a simple
   * <code>&lt;img&gt;</code> element. It may be a more complex structure that
   * should be treated opaquely.
   * 
   * @return the HTML representation of this prototype
   * @deprecated See {@link CssResource.Sprite} instead.
   */
  @Deprecated
  String getHTML();

  /**
   * Returns the horizontal position of the image within the composite image.
   */
  int getLeft();

  /**
   * Returns the vertical position of the image within the composite image.
   */
  int getTop();

  /**
   * Returns the URL for the composite image that contains the ImageResource.
   */
  String getURL();

  /**
   * Returns the width of the image.
   */
  int getWidth();
}
