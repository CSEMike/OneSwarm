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
import com.google.gwt.libideas.resources.rg.CssResourceGenerator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Aggregates and minifies CSS stylesheets. A CssResource represents a regular
 * CSS file with GWT-specific at-rules.
 * <p>
 * Currently-supported accessor functions:
 * 
 * <ul>
 * <li>{@code String someClassName();} will allow the css class
 * <code>.someClassName</code> to be obfuscated at runtime. The function will
 * return the obfuscated class name.</li>
 * <li>{@code <primitive numeric type> someDefName();} will allow access to the
 * values defined by {@literal @def} rules within the CSS file. The defined
 * value must be a raw number, a CSS length, or a percentage value.
 * </ul>
 * 
 * <p>
 * Currently-supported rules:
 * 
 * <ul>
 * <li>{@code @def NAME replacement-expression; .myClass background: NAME;}
 * Define a static constant. The replacement expression may be any CSS that
 * would be valid in a property value context. A {@code @def} may refer to
 * previously-defined rules, but no forward-references will be honored.</li>
 * <li>{@code @eval NAME Java-expression; .myClass background: NAME;} Define a
 * constant based on a Java expression.</li>
 * <li><code>{@literal @if} [!]property list of values {ruleBlock}</code> Include or
 * exclude CSS rules based on the value of a deferred-binding property. Also
 * {@code @elif} and {@code @else} follow the same pattern.<br/>
 * This might look like {@code @if user.agent ie6 safari ...}.</li>
 * <li><code>{@literal @if} (Java-expression) {ruleBlock}</code> Include or exclude
 * CSS rules based on a boolean Java expression.</li>
 * <li><code>{@literal @noflip} { rules }</code> will suppress the automatic
 * right-to-left transformation applied to the CSS when the module is compiled
 * for an RTL language.</li>
 * <li>
 * <code>{@literal @}sprite .any .selector {gwt-image: "imageResourceFunction";}</code>
 * . The appearance, size, and height of the sprite will be affected by any
 * {@link ImageResource.ImageOptions} annotations present on the related
 * {@link ImageResource} accessor function. Additional properties may be
 * specified in the rule block.</li>
 * <li>{@code @url NAME siblingDataResource; .myClass background: NAME
 * repeat-x;} Use a {@link DataResource} to generate a <code>url('...'}</code> value.</li>
 * </ul>
 * 
 * <p>
 * Currently-supported CSS functions:
 * 
 * <ul>
 * <li>{@code literal("expression")} substitutes a property value that does not
 * conform to CSS2 parsing rules. The escape sequences {@code \"} and {@code \\}
 * will be replaced with {@code "} and {@code \} respectively.
 * <li>{@code value("bundleFunction.someFunction[.other[...]]" [, "suffix"])}
 * substitute the value of a sequence of named zero-arg function invocations. An
 * optional suffix will be appended to the return value of the function. The
 * first name is resolved relative to the bundle interface passed to
 * {@link com.google.gwt.core.client.GWT#create(Class)}.</li>
 * </ul>
 * 
 * @deprecated Superseded by {@link com.google.gwt.resources.client.CssResource}
 * @see <a
 *      href="http://code.google.com/p/google-web-toolkit-incubator/wiki/CssResource"
 *      >CssResource design doc</a>
 */
@Deprecated
@ResourceGeneratorType(CssResourceGenerator.class)
public interface CssResource extends ResourcePrototype {
  /**
   * The original CSS class name specified in the resource. This allows CSS
   * classes that do not correspond to Java identifiers to be mapped onto
   * obfuscated class accessors.
   * 
   * <pre>
   * .some-non-java-ident { background: blue; }
   * 
   * interface MyCssResource extends CssResource { {@literal @}ClassName("some-non-java-ident") String classAccessor();
   * }
   * 
   * </pre>
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface ClassName {
    String value();
  }

  /**
   * Makes class selectors from other CssResource types available in the raw
   * source of a CssResource. String accessor methods can be referred to using
   * the value of the imported type's {@link ImportedWithPrefix} value.
   * <p>
   * This is an example of creating a descendant selector with two unrelated
   * types:
   * 
   * <pre>
   *{@literal @ImportedWithPrefix}("some-prefix")
   * interface ToImport extends CssResource {
   *   String widget();
   * }
   * 
   *{@literal @ImportedWithPrefix}("other-import")
   * interface OtherImport extends CssResource {
   *   String widget();
   * }
   * 
   * interface Resources extends ClientBundle {
   *  {@literal @Import}(value = {ToImport.class, OtherImport.class})
   *  {@literal @Source}("my.css")
   *   CssResource usesImports();
   * }
   * 
   * my.css:
   * .some-prefix-widget .other-import-widget {...}
   * </pre>
   * 
   * If the imported CssResource type is lacking an {@link ImportedWithPrefix}
   * annotation, the simple name of the type will be used instead. In the above
   * example, without the annotation on <code>ToImport</code>, the class
   * selector would have been <code>.ToImport-widget</code>. Notice also that
   * both interfaces defined a method called <code>widget()</code>, which would
   * prevent meaningful composition of the original interfaces.
   * <p>
   * It is an error to import multiple classes with the same prefix into one
   * CssResource.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Import {
    Class<? extends CssResource>[] value();
  }

  /**
   * Specifies the string prefix to use when one CssResource is imported into
   * the scope of another CssResource.
   * 
   * @see Import
   */
  @Documented
  @Target(ElementType.TYPE)
  public @interface ImportedWithPrefix {
    String value();
  }

  /**
   * Indicates that the String accessor methods defined in a CssResource will
   * return the same values across all implementations of that type.
   * <p>
   * This is an example of "stateful" class selectors being used:
   * 
   * <pre>
   *{@literal @Shared}
   * interface FocusCss extends CssResource {
   *   String focused();
   *   String unfocused();
   * }
   * 
   * interface PanelCss extends CssResource, FocusCss {
   *   String widget();
   * }
   * 
   * interface InputCss extends CssResource, FocusCss {
   *   String widget();
   * }
   * 
   * input.css:
   * *.focused .widget {border: thin solid blue;}
   * 
   * Application.java:
   * myPanel.add(myInputWidget);
   * myPanel.addStyleName(instanceOfPanelCss.focused());
   * </pre>
   * 
   * Because the <code>FocusCss</code> interface is tagged with {@code @Shared},
   * the <code>focused()</code> method on the instance of <code>PanelCss</code>
   * will match the <code>.focused</code> parent selector in
   * <code>input.css</code>.
   * <p>
   * The effect of inheriting an {@code Shared} interface can be replicated by
   * use use of the {@link Import} annotation (e.g. {@code .FocusCss-focused
   * .widget}), however the use of state-bearing descendant selectors is common
   * enough to warrant an easier use-case.
   */
  @Documented
  @Target(ElementType.TYPE)
  public @interface Shared {
  }

  /**
   * The presence of this annotation on a CssResource accessor method indicates
   * that any class selectors that do not correspond with a String accessor
   * method in the return type should trigger a compilation error. In the normal
   * case, any unobfuscatable class selectors will be emitted as-is. This
   * annotation ensures that the resource does not contribute any unobfuscated
   * class selectors into the global CSS namespace and is recommended as the
   * default for library-provided CssResource instances.
   * <p>
   * Given these interfaces:
   * 
   * <pre>
   * interface MyCss extends CssResource {
   *   String someClass();
   * }
   * 
   * interface MyBundle extends ClientBundle {
   *  {@literal @Source("my.css")}
   *  {@literal @Strict}
   *   MyCss css();
   * }
   * </pre>
   * 
   * the source CSS will fail to compile if it does not contain exactly the one
   * class selector defined in the MyCss type.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Strict {
  }

  /**
   * Provides the contents of the CssResource.
   */
  String getText();
}
