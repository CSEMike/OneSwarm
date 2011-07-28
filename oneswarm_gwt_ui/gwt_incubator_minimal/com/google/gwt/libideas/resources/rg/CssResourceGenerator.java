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
package com.google.gwt.libideas.resources.rg;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dom.client.Element;
import com.google.gwt.libideas.resources.client.CssResource;
import com.google.gwt.libideas.resources.client.DataResource;
import com.google.gwt.libideas.resources.client.ImageResource;
import com.google.gwt.libideas.resources.client.CssResource.ClassName;
import com.google.gwt.libideas.resources.client.CssResource.Import;
import com.google.gwt.libideas.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.libideas.resources.client.CssResource.Shared;
import com.google.gwt.libideas.resources.client.CssResource.Strict;
import com.google.gwt.libideas.resources.client.ImageResource.ImageOptions;
import com.google.gwt.libideas.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.libideas.resources.css.CssGenerationVisitor;
import com.google.gwt.libideas.resources.css.GenerateCssAst;
import com.google.gwt.libideas.resources.css.ast.CollapsedNode;
import com.google.gwt.libideas.resources.css.ast.Context;
import com.google.gwt.libideas.resources.css.ast.CssCompilerException;
import com.google.gwt.libideas.resources.css.ast.CssDef;
import com.google.gwt.libideas.resources.css.ast.CssEval;
import com.google.gwt.libideas.resources.css.ast.CssIf;
import com.google.gwt.libideas.resources.css.ast.CssMediaRule;
import com.google.gwt.libideas.resources.css.ast.CssModVisitor;
import com.google.gwt.libideas.resources.css.ast.CssNoFlip;
import com.google.gwt.libideas.resources.css.ast.CssNode;
import com.google.gwt.libideas.resources.css.ast.CssNodeCloner;
import com.google.gwt.libideas.resources.css.ast.CssProperty;
import com.google.gwt.libideas.resources.css.ast.CssRule;
import com.google.gwt.libideas.resources.css.ast.CssSelector;
import com.google.gwt.libideas.resources.css.ast.CssSprite;
import com.google.gwt.libideas.resources.css.ast.CssStylesheet;
import com.google.gwt.libideas.resources.css.ast.CssUrl;
import com.google.gwt.libideas.resources.css.ast.CssVisitor;
import com.google.gwt.libideas.resources.css.ast.HasNodes;
import com.google.gwt.libideas.resources.css.ast.CssProperty.DotPathValue;
import com.google.gwt.libideas.resources.css.ast.CssProperty.ExpressionValue;
import com.google.gwt.libideas.resources.css.ast.CssProperty.IdentValue;
import com.google.gwt.libideas.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.libideas.resources.css.ast.CssProperty.NumberValue;
import com.google.gwt.libideas.resources.css.ast.CssProperty.Value;
import com.google.gwt.libideas.resources.ext.ResourceBundleRequirements;
import com.google.gwt.libideas.resources.ext.ResourceContext;
import com.google.gwt.libideas.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.libideas.resources.rebind.StringSourceWriter;
import com.google.gwt.user.rebind.SourceWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

/**
 * Provides implementations of CSSResources.
 */
public class CssResourceGenerator extends AbstractResourceGenerator {
  static class ClassRenamer extends CssVisitor {
    private final Map<String, Map<JMethod, String>> classReplacementsWithPrefix;
    private final Pattern classSelectorPattern = Pattern.compile("\\.([^ :>+#.]*)");
    private final TreeLogger logger;
    private final Set<JMethod> missingClasses;
    private final Set<String> replacedClasses = new HashSet<String>();
    private final boolean strict;
    private final Set<String> unknownClasses = new HashSet<String>();

    public ClassRenamer(TreeLogger logger,
        Map<String, Map<JMethod, String>> classReplacementsWithPrefix,
        boolean strict) {
      this.logger = logger.branch(TreeLogger.DEBUG, "Replacing CSS class names");
      this.classReplacementsWithPrefix = classReplacementsWithPrefix;
      this.strict = strict;

      // Require a definition for all classes in the default namespace
      assert classReplacementsWithPrefix.containsKey("");
      missingClasses = new HashSet<JMethod>(
          classReplacementsWithPrefix.get("").keySet());
    }

    @Override
    public void endVisit(CssSelector x, Context ctx) {
      String sel = x.getSelector();

      // TODO This would be simplified by having a class hierarchy for selectors
      for (Map.Entry<String, Map<JMethod, String>> outerEntry : classReplacementsWithPrefix.entrySet()) {
        String prefix = outerEntry.getKey();
        for (Map.Entry<JMethod, String> entry : outerEntry.getValue().entrySet()) {
          String name = entry.getKey().getName();

          ClassName className = entry.getKey().getAnnotation(ClassName.class);
          if (className != null) {
            name = className.value();
          }
          name = prefix + name;

          Pattern p = Pattern.compile("(.*)\\.(" + Pattern.quote(name)
              + ")([ :>+#.].*|$)");
          Matcher m = p.matcher(sel);
          if (m.find()) {
            sel = m.group(1) + "." + entry.getValue() + m.group(3);
            missingClasses.remove(entry.getKey());
            if (strict) {
              replacedClasses.add(entry.getValue());
            }
          }
        }
      }

      sel = sel.trim();

      if (strict) {
        Matcher m = classSelectorPattern.matcher(sel);
        while (m.find()) {
          String classSelector = m.group(1);
          if (!replacedClasses.contains(classSelector)) {
            unknownClasses.add(classSelector);
          }
        }
      }

      x.setSelector(sel);
    }

    @Override
    public void endVisit(CssStylesheet x, Context ctx) {
      boolean stop = false;
      if (!missingClasses.isEmpty()) {
        stop = true;
        TreeLogger errorLogger = logger.branch(TreeLogger.INFO,
            "The following obfuscated style classes were missing from "
                + "the source CSS file:");
        for (JMethod m : missingClasses) {
          String name = m.getName();
          ClassName className = m.getAnnotation(ClassName.class);
          if (className != null) {
            name = className.value();
          }
          errorLogger.log(TreeLogger.ERROR, name + ": Fix by adding ." + name
              + "{}");
        }
      }

      if (strict && !unknownClasses.isEmpty()) {
        stop = true;
        TreeLogger errorLogger = logger.branch(TreeLogger.ERROR,
            "The following unobfuscated classes were present in a strict CssResource:");
        for (String s : unknownClasses) {
          errorLogger.log(TreeLogger.ERROR, s);
        }
      }

      if (stop) {
        throw new CssCompilerException("Missing a CSS replacement");
      }
    }
  }

  /**
   * Statically evaluates {@literal @if} rules.
   */
  static class IfEvaluator extends CssModVisitor {
    private final TreeLogger logger;
    private final PropertyOracle oracle;

    public IfEvaluator(TreeLogger logger, PropertyOracle oracle) {
      this.logger = logger.branch(TreeLogger.DEBUG,
          "Replacing property-based @if blocks");
      this.oracle = oracle;
    }

    @Override
    public void endVisit(CssIf x, Context ctx) {
      if (x.getExpression() != null) {
        // This gets taken care of by the runtime substitution visitor
      } else {
        try {
          String propertyName = x.getPropertyName();
          String propValue = oracle.getPropertyValue(logger, propertyName);

          /*
           * If the deferred binding property's value is in the list of values
           * in the @if rule, move the rules into the @if's context.
           */
          if (Arrays.asList(x.getPropertyValues()).contains(propValue)
              ^ x.isNegated()) {
            for (CssNode n : x.getNodes()) {
              ctx.insertBefore(n);
            }
          } else {
            // Otherwise, move the else block into the if statement's position
            for (CssNode n : x.getElseNodes()) {
              ctx.insertBefore(n);
            }
          }

          // Always delete @if rules that we can statically evaluate
          ctx.removeMe();
        } catch (BadPropertyValueException e) {
          logger.log(TreeLogger.ERROR, "Unable to evaluate @if block", e);
          throw new CssCompilerException("Unable to parse CSS", e);
        }
      }
    }
  }

  static class JClassOrderComparator implements Comparator<JClassType> {
    public int compare(JClassType o1, JClassType o2) {
      return o1.getQualifiedSourceName().compareTo(o2.getQualifiedSourceName());
    }
  }

  /**
   * Merges rules that have matching selectors.
   */
  static class MergeIdenticalSelectorsVisitor extends CssModVisitor {
    private final Map<String, CssRule> canonicalRules = new HashMap<String, CssRule>();
    private final List<CssRule> rulesInOrder = new ArrayList<CssRule>();

    @Override
    public boolean visit(CssIf x, Context ctx) {
      visitInNewContext(x.getNodes());
      visitInNewContext(x.getElseNodes());
      return false;
    }

    @Override
    public boolean visit(CssMediaRule x, Context ctx) {
      visitInNewContext(x.getNodes());
      return false;
    }

    @Override
    public boolean visit(CssRule x, Context ctx) {
      // Assumed to run immediately after SplitRulesVisitor
      assert x.getSelectors().size() == 1;
      CssSelector sel = x.getSelectors().get(0);

      if (canonicalRules.containsKey(sel.getSelector())) {
        CssRule canonical = canonicalRules.get(sel.getSelector());

        // Check everything between the canonical rule and this rule for common
        // properties. If there are common properties, it would be unsafe to
        // promote the rule.
        boolean hasCommon = false;
        int index = rulesInOrder.indexOf(canonical) + 1;
        assert index != 0;

        for (Iterator<CssRule> i = rulesInOrder.listIterator(index); i.hasNext()
            && !hasCommon;) {
          hasCommon = haveCommonProperties(i.next(), x);
        }

        if (!hasCommon) {
          // It's safe to promote the rule
          canonical.getProperties().addAll(x.getProperties());
          ctx.removeMe();
          return false;
        }
      }

      canonicalRules.put(sel.getSelector(), x);
      rulesInOrder.add(x);
      return false;
    }

    private void visitInNewContext(List<CssNode> nodes) {
      MergeIdenticalSelectorsVisitor v = new MergeIdenticalSelectorsVisitor();
      v.acceptWithInsertRemove(nodes);
      rulesInOrder.addAll(v.rulesInOrder);
    }
  }

  /**
   * Merges rules that have identical content.
   */
  static class MergeRulesByContentVisitor extends CssModVisitor {
    private Map<String, CssRule> rulesByContents = new HashMap<String, CssRule>();
    private final List<CssRule> rulesInOrder = new ArrayList<CssRule>();

    @Override
    public boolean visit(CssIf x, Context ctx) {
      visitInNewContext(x.getNodes());
      visitInNewContext(x.getElseNodes());
      return false;
    }

    @Override
    public boolean visit(CssMediaRule x, Context ctx) {
      visitInNewContext(x.getNodes());
      return false;
    }

    @Override
    public boolean visit(CssRule x, Context ctx) {
      StringBuilder b = new StringBuilder();
      for (CssProperty p : x.getProperties()) {
        b.append(p.getName()).append(":").append(p.getValues().getExpression());
      }

      String content = b.toString();
      CssRule canonical = rulesByContents.get(content);

      // Check everything between the canonical rule and this rule for common
      // properties. If there are common properties, it would be unsafe to
      // promote the rule.
      if (canonical != null) {
        boolean hasCommon = false;
        int index = rulesInOrder.indexOf(canonical) + 1;
        assert index != 0;

        for (Iterator<CssRule> i = rulesInOrder.listIterator(index); i.hasNext()
            && !hasCommon;) {
          hasCommon = haveCommonProperties(i.next(), x);
        }

        if (!hasCommon) {
          canonical.getSelectors().addAll(x.getSelectors());
          ctx.removeMe();
          return false;
        }
      }

      rulesByContents.put(content, x);
      rulesInOrder.add(x);
      return false;
    }

    private void visitInNewContext(List<CssNode> nodes) {
      MergeRulesByContentVisitor v = new MergeRulesByContentVisitor();
      v.acceptWithInsertRemove(nodes);
      rulesInOrder.addAll(v.rulesInOrder);
    }
  }

  static class RequirementsCollector extends CssVisitor {
    private final TreeLogger logger;
    private final ResourceBundleRequirements requirements;

    public RequirementsCollector(TreeLogger logger,
        ResourceBundleRequirements requirements) {
      this.logger = logger.branch(TreeLogger.DEBUG,
          "Scanning CSS for requirements");
      this.requirements = requirements;
    }

    @Override
    public void endVisit(CssIf x, Context ctx) {
      String propertyName = x.getPropertyName();
      if (propertyName != null) {
        try {
          requirements.addPermutationAxis(propertyName);
        } catch (BadPropertyValueException e) {
          logger.log(TreeLogger.ERROR, "Unknown deferred-binding property "
              + propertyName, e);
          throw new CssCompilerException("Unknown deferred-binding property", e);
        }
      }
    }
  }

  static class RtlVisitor extends CssModVisitor {
    /**
     * Records if we're currently visiting a CssRule whose only selector is
     * "body".
     */
    private boolean inBodyRule;

    @Override
    public void endVisit(CssProperty x, Context ctx) {
      String name = x.getName();

      if (name.equalsIgnoreCase("left")) {
        x.setName("right");
      } else if (name.equalsIgnoreCase("right")) {
        x.setName("left");
      } else if (name.endsWith("-left")) {
        int len = name.length();
        x.setName(name.substring(0, len - 4) + "right");
      } else if (name.endsWith("-right")) {
        int len = name.length();
        x.setName(name.substring(0, len - 5) + "left");
      } else if (name.contains("-right-")) {
        x.setName(name.replace("-right-", "-left-"));
      } else if (name.contains("-left-")) {
        x.setName(name.replace("-left-", "-right-"));
      } else {
        List<Value> values = new ArrayList<Value>(x.getValues().getValues());
        invokePropertyHandler(x.getName(), values);
        x.setValue(new CssProperty.ListValue(values));
      }
    }

    @Override
    public boolean visit(CssNoFlip x, Context ctx) {
      return false;
    }

    @Override
    public boolean visit(CssRule x, Context ctx) {
      inBodyRule = x.getSelectors().size() == 1
          && x.getSelectors().get(0).getSelector().equals("body");
      return true;
    }

    void propertyHandlerBackground(List<Value> values) {
      /*
       * The first numeric value will be treated as the left position only if we
       * havn't seen any value that could potentially be the left value.
       */
      boolean seenLeft = false;

      for (ListIterator<Value> it = values.listIterator(); it.hasNext();) {
        Value v = it.next();
        Value maybeFlipped = flipLeftRightIdentValue(v);
        NumberValue nv = v.isNumberValue();
        if (v != maybeFlipped) {
          it.set(maybeFlipped);
          seenLeft = true;

        } else if (isIdent(v, "center")) {
          seenLeft = true;

        } else if (!seenLeft && (nv != null)) {
          seenLeft = true;
          if ("%".equals(nv.getUnits())) {
            float position = 100f - nv.getValue();
            it.set(new NumberValue(position, "%"));
            break;
          }
        }
      }
    }

    void propertyHandlerBackgroundPosition(List<Value> values) {
      propertyHandlerBackground(values);
    }

    Value propertyHandlerBackgroundPositionX(Value v) {
      ArrayList<Value> list = new ArrayList<Value>(1);
      list.add(v);
      propertyHandlerBackground(list);
      return list.get(0);
    }

    /**
     * Note there should be no propertyHandlerBorder(). The CSS spec states that
     * the border property must set all values at once.
     */
    void propertyHandlerBorderColor(List<Value> values) {
      swapFour(values);
    }

    void propertyHandlerBorderStyle(List<Value> values) {
      swapFour(values);
    }

    void propertyHandlerBorderWidth(List<Value> values) {
      swapFour(values);
    }

    Value propertyHandlerClear(Value v) {
      return propertyHandlerFloat(v);
    }

    Value propertyHandlerCursor(Value v) {
      IdentValue identValue = v.isIdentValue();
      if (identValue == null) {
        return v;
      }

      String ident = identValue.getIdent().toLowerCase();
      if (!ident.endsWith("-resize")) {
        return v;
      }

      StringBuffer newIdent = new StringBuffer();

      if (ident.length() == 9) {
        if (ident.charAt(0) == 'n') {
          newIdent.append('n');
          ident = ident.substring(1);
        } else if (ident.charAt(0) == 's') {
          newIdent.append('s');
          ident = ident.substring(1);
        } else {
          return v;
        }
      }

      if (ident.length() == 8) {
        if (ident.charAt(0) == 'e') {
          newIdent.append("w-resize");
        } else if (ident.charAt(0) == 'w') {
          newIdent.append("e-resize");
        } else {
          return v;
        }
        return new IdentValue(newIdent.toString());
      } else {
        return v;
      }
    }

    Value propertyHandlerDirection(Value v) {
      if (inBodyRule) {
        if (isIdent(v, "ltr")) {
          return new IdentValue("rtl");
        } else if (isIdent(v, "rtl")) {
          return new IdentValue("ltr");
        }
      }
      return v;
    }

    Value propertyHandlerFloat(Value v) {
      return flipLeftRightIdentValue(v);
    }

    void propertyHandlerMargin(List<Value> values) {
      swapFour(values);
    }

    void propertyHandlerPadding(List<Value> values) {
      swapFour(values);
    }

    Value propertyHandlerPageBreakAfter(Value v) {
      return flipLeftRightIdentValue(v);
    }

    Value propertyHandlerPageBreakBefore(Value v) {
      return flipLeftRightIdentValue(v);
    }

    Value propertyHandlerTextAlign(Value v) {
      return flipLeftRightIdentValue(v);
    }

    private Value flipLeftRightIdentValue(Value v) {
      if (isIdent(v, "right")) {
        return new IdentValue("left");

      } else if (isIdent(v, "left")) {
        return new IdentValue("right");
      }
      return v;
    }

    /**
     * Reflectively invokes a propertyHandler method for the named property.
     * Dashed names are transformed into camel-case names; only letters
     * following a dash will be capitalized when looking for a method to prevent
     * <code>fooBar<code> and <code>foo-bar</code> from colliding.
     */
    private void invokePropertyHandler(String name, List<Value> values) {
      // See if we have a property-handler function
      try {
        String[] parts = name.toLowerCase().split("-");
        StringBuffer methodName = new StringBuffer("propertyHandler");
        for (String part : parts) {
          methodName.append(Character.toUpperCase(part.charAt(0)));
          methodName.append(part, 1, part.length());
        }

        try {
          // Single-arg for simplicity
          Method m = getClass().getDeclaredMethod(methodName.toString(),
              Value.class);
          assert Value.class.isAssignableFrom(m.getReturnType());
          Value newValue = (Value) m.invoke(this, values.get(0));
          values.set(0, newValue);
        } catch (NoSuchMethodException e) {
          // OK
        }

        try {
          // Or the whole List for completeness
          Method m = getClass().getDeclaredMethod(methodName.toString(),
              List.class);
          m.invoke(this, values);
        } catch (NoSuchMethodException e) {
          // OK
        }

      } catch (SecurityException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      } catch (IllegalArgumentException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      } catch (IllegalAccessException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      } catch (InvocationTargetException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      }
    }

    private boolean isIdent(Value value, String query) {
      IdentValue v = value.isIdentValue();
      return v != null && v.getIdent().equalsIgnoreCase(query);
    }

    /**
     * Swaps the second and fourth values in a list of four values.
     */
    private void swapFour(List<Value> values) {
      if (values.size() == 4) {
        Collections.swap(values, 1, 3);
      }
    }
  }

  /**
   * Splits rules with compound selectors into multiple rules.
   */
  static class SplitRulesVisitor extends CssModVisitor {
    @Override
    public void endVisit(CssRule x, Context ctx) {
      if (x.getSelectors().size() == 1) {
        return;
      }

      for (CssSelector sel : x.getSelectors()) {
        CssRule newRule = new CssRule();
        newRule.getSelectors().add(sel);
        newRule.getProperties().addAll(
            CssNodeCloner.clone(CssProperty.class, x.getProperties()));
        // newRule.getProperties().addAll(x.getProperties());
        ctx.insertBefore(newRule);
      }
      ctx.removeMe();
      return;
    }
  }

  /**
   * Replaces CssSprite nodes with CssRule nodes that will display the sprited
   * image. The real trick with spriting the images is to reuse the
   * ImageResource processing framework by requiring the sprite to be defined in
   * terms of an ImageResource.
   */
  static class Spriter extends CssModVisitor {
    private final ResourceContext context;
    private final TreeLogger logger;

    public Spriter(TreeLogger logger, ResourceContext context) {
      this.logger = logger.branch(TreeLogger.DEBUG,
          "Creating image sprite classes");
      this.context = context;
    }

    @Override
    public void endVisit(CssSprite x, Context ctx) {
      JClassType bundleType = context.getResourceBundleType();
      String functionName = x.getResourceFunction();

      if (functionName == null) {
        logger.log(TreeLogger.ERROR, "The @sprite rule " + x.getSelectors()
            + " must specify the " + CssSprite.IMAGE_PROPERTY_NAME
            + " property");
        throw new CssCompilerException("No image property specified");
      }

      // Find the image accessor method
      JMethod imageMethod = null;
      JMethod[] allMethods = bundleType.getOverridableMethods();
      for (int i = 0; imageMethod == null && i < allMethods.length; i++) {
        JMethod candidate = allMethods[i];
        // If the function name matches and takes no parameters
        if (candidate.getName().equals(functionName)
            && candidate.getParameters().length == 0) {
          // We have a match
          imageMethod = candidate;
        }
      }

      // Method unable to be located
      if (imageMethod == null) {
        logger.log(TreeLogger.ERROR, "Unable to find ImageResource method "
            + functionName + " in " + bundleType.getQualifiedSourceName());
        throw new CssCompilerException("Cannot find image function");
      }

      JClassType imageResourceType = context.getGeneratorContext().getTypeOracle().findType(
          ImageResource.class.getName());
      assert imageResourceType != null;

      if (!imageResourceType.isAssignableFrom(imageMethod.getReturnType().isClassOrInterface())) {
        logger.log(TreeLogger.ERROR, "The return type of " + functionName
            + " is not assignable to "
            + imageResourceType.getSimpleSourceName());
        throw new CssCompilerException("Incorrect return type for "
            + CssSprite.IMAGE_PROPERTY_NAME + " method");
      }

      ImageOptions options = imageMethod.getAnnotation(ImageOptions.class);
      RepeatStyle repeatStyle;
      if (options != null) {
        repeatStyle = options.repeatStyle();
      } else {
        repeatStyle = RepeatStyle.None;
      }

      String instance = "(" + context.getImplementationSimpleSourceName()
          + ".this." + functionName + "())";

      CssRule replacement = new CssRule();
      replacement.getSelectors().addAll(x.getSelectors());
      List<CssProperty> properties = replacement.getProperties();

      if (repeatStyle == RepeatStyle.None
          || repeatStyle == RepeatStyle.Horizontal) {
        properties.add(new CssProperty("height", new ExpressionValue(instance
            + ".getHeight() + \"px\""), false));
      }

      if (repeatStyle == RepeatStyle.None
          || repeatStyle == RepeatStyle.Vertical) {
        properties.add(new CssProperty("width", new ExpressionValue(instance
            + ".getWidth() + \"px\""), false));
      }
      properties.add(new CssProperty("overflow", new IdentValue("hidden"),
          false));

      String repeatText;
      switch (repeatStyle) {
        case None:
          repeatText = " no-repeat";
          break;
        case Horizontal:
          repeatText = " repeat-x";
          break;
        case Vertical:
          repeatText = " repeat-y";
          break;
        case Both:
          repeatText = " repeat";
          break;
        default:
          throw new RuntimeException("Unknown repeatStyle " + repeatStyle);
      }

      String backgroundExpression = "\"url(\\\"\" + " + instance
          + ".getURL() + \"\\\") -\" + " + instance
          + ".getLeft() + \"px -\" + " + instance + ".getTop() + \"px "
          + repeatText + "\"";
      properties.add(new CssProperty("background", new ExpressionValue(
          backgroundExpression), false));

      // Retain any user-specified properties
      properties.addAll(x.getProperties());

      ctx.replaceMe(replacement);
    }
  }

  static class SubstitutionCollector extends CssVisitor {
    private final Map<String, CssDef> substitutions = new HashMap<String, CssDef>();

    @Override
    public void endVisit(CssDef x, Context ctx) {
      substitutions.put(x.getKey(), x);
    }

    @Override
    public void endVisit(CssEval x, Context ctx) {
      substitutions.put(x.getKey(), x);
    }

    @Override
    public void endVisit(CssUrl x, Context ctx) {
      substitutions.put(x.getKey(), x);
    }
  }

  /**
   * Substitute symbolic replacements into string values.
   */
  static class SubstitutionReplacer extends CssVisitor {
    private final ResourceContext context;
    private final TreeLogger logger;
    private final Map<String, CssDef> substitutions;

    public SubstitutionReplacer(TreeLogger logger, ResourceContext context,
        Map<String, CssDef> substitutions) {
      this.context = context;
      this.logger = logger;
      this.substitutions = substitutions;
    }

    @Override
    public void endVisit(CssProperty x, Context ctx) {
      if (x.getValues() == null) {
        // Nothing to do
        return;
      }

      List<Value> values = new ArrayList<Value>(x.getValues().getValues());

      for (ListIterator<Value> i = values.listIterator(); i.hasNext();) {
        IdentValue v = i.next().isIdentValue();

        if (v == null) {
          // Don't try to substitute into anything other than idents
          continue;
        }

        String value = v.getIdent();
        CssDef def = substitutions.get(value);

        if (def == null) {
          continue;
        } else if (def instanceof CssUrl) {
          assert def.getValues().size() == 1;
          assert def.getValues().get(0).isIdentValue() != null;
          String functionName = def.getValues().get(0).isIdentValue().getIdent();

          // Find the method
          JMethod method = context.getResourceBundleType().findMethod(
              functionName, new JType[0]);

          if (method == null) {
            logger.log(TreeLogger.ERROR, "Unable to find DataResource method "
                + functionName + " in "
                + context.getResourceBundleType().getQualifiedSourceName());
            throw new CssCompilerException("Cannot find data function");
          }

          String instance = "((" + DataResource.class.getName() + ")("
              + context.getImplementationSimpleSourceName() + ".this."
              + functionName + "()))";

          StringBuilder expression = new StringBuilder();
          expression.append("\"url('\" + ");
          expression.append(instance).append(".getUrl()");
          expression.append(" + \"')\"");
          i.set(new ExpressionValue(expression.toString()));

        } else {
          i.remove();
          for (Value defValue : def.getValues()) {
            i.add(defValue);
          }
        }
      }

      x.setValue(new ListValue(values));
    }
  }

  /**
   * A lookup table of base-32 chars we use to encode CSS idents. Because CSS
   * class selectors may be case-insensitive, we don't have enough characters to
   * use a base-64 encoding.
   */
  private static final char[] BASE32_CHARS = new char[] {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
      'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-', '0',
      '1', '2', '3', '4'};

  /**
   * This value is used by {@link #concatOp} to help create a more balanced AST
   * tree by producing parenthetical expressions.
   */
  private static final int CONCAT_EXPRESSION_LIMIT = 20;

  public static void main(String[] args) {
    for (int i = 0; i < 1000; i++) {
      System.out.println(makeIdent(i));
    }
  }

  static boolean haveCommonProperties(CssRule a, CssRule b) {
    if (a.getProperties().size() == 0 || b.getProperties().size() == 0) {
      return false;
    }

    SortedSet<String> aProperties = new TreeSet<String>();
    SortedSet<String> bProperties = new TreeSet<String>();

    for (CssProperty p : a.getProperties()) {
      aProperties.add(p.getName());
    }
    for (CssProperty p : b.getProperties()) {
      bProperties.add(p.getName());
    }

    Iterator<String> ai = aProperties.iterator();
    Iterator<String> bi = bProperties.iterator();

    String aName = ai.next();
    String bName = bi.next();
    for (;;) {
      int comp = aName.compareToIgnoreCase(bName);
      if (comp == 0) {
        return true;
      } else if (comp > 0) {
        if (aName.startsWith(bName + "-")) {
          return true;
        }

        if (!bi.hasNext()) {
          break;
        }
        bName = bi.next();
      } else {
        if (bName.startsWith(aName + "-")) {
          return true;
        }
        if (!ai.hasNext()) {
          break;
        }
        aName = ai.next();
      }
    }

    return false;
  }

  /**
   * Create a Java expression that evaluates to a string representation of the
   * given node. Visible only for testing.
   */
  static <T extends CssNode & HasNodes> String makeExpression(
      TreeLogger logger, ResourceContext context, JClassType cssResourceType,
      T node, boolean prettyOutput) throws UnableToCompleteException {
    // Generate the CSS template
    DefaultTextOutput out = new DefaultTextOutput(!prettyOutput);
    CssGenerationVisitor v = new CssGenerationVisitor(out);
    v.accept(node);

    // Generate the final Java expression
    String template = out.toString();
    StringBuilder b = new StringBuilder();
    int start = 0;

    /*
     * Very large concatenation expressions using '+' cause the GWT compiler to
     * overflow the stack due to deep AST nesting. The workaround for now is to
     * force it to be more balanced using intermediate concatenation groupings.
     * 
     * This variable is used to track the number of subexpressions within the
     * current parenthetical expression.
     */
    int numExpressions = 0;

    b.append('(');
    for (Map.Entry<Integer, List<CssNode>> entry : v.getSubstitutionPositions().entrySet()) {
      // Add the static section between start and the substitution point
      b.append('"');
      b.append(Generator.escape(template.substring(start, entry.getKey())));
      b.append('\"');
      numExpressions = concatOp(numExpressions, b);

      // Add the nodes at the substitution point
      for (CssNode x : entry.getValue()) {
        TreeLogger loopLogger = logger.branch(TreeLogger.DEBUG,
            "Performing substitution in node " + x.toString());

        if (x instanceof CssIf) {
          CssIf asIf = (CssIf) x;

          // Generate the sub-expressions
          String expression = makeExpression(loopLogger, context,
              cssResourceType, new CollapsedNode(asIf), prettyOutput);

          String elseExpression;
          if (asIf.getElseNodes().isEmpty()) {
            // We'll treat an empty else block as an empty string
            elseExpression = "\"\"";
          } else {
            elseExpression = makeExpression(loopLogger, context,
                cssResourceType, new CollapsedNode(asIf.getElseNodes()),
                prettyOutput);
          }

          // ((expr) ? "CSS" : "elseCSS") +
          b.append("((" + asIf.getExpression() + ") ? " + expression + " : "
              + elseExpression + ") ");
          numExpressions = concatOp(numExpressions, b);

        } else if (x instanceof CssProperty) {
          CssProperty property = (CssProperty) x;

          validateValue(loopLogger, context.getResourceBundleType(),
              property.getValues());

          // (expr) +
          b.append("(" + property.getValues().getExpression() + ") ");
          numExpressions = concatOp(numExpressions, b);

        } else {
          // This indicates that some magic node is slipping by our visitors
          loopLogger.log(TreeLogger.ERROR, "Unhandled substitution "
              + x.getClass());
          throw new UnableToCompleteException();
        }
      }
      start = entry.getKey();
    }

    // Add the remaining parts of the template
    b.append('"');
    b.append(Generator.escape(template.substring(start)));
    b.append('"');
    b.append(')');

    return b.toString();
  }

  /**
   * Check if number of concat expressions currently exceeds limit and either
   * append '+' if the limit isn't reached or ') + (' if it is.
   * 
   * @return numExpressions + 1 or 0 if limit was exceeded.
   */
  private static int concatOp(int numExpressions, StringBuilder b) {
    /*
     * TODO: Fix the compiler to better handle arbitrarily long concatenation
     * expressions.
     */
    if (numExpressions >= CONCAT_EXPRESSION_LIMIT) {
      b.append(") + (");
      return 0;
    }

    b.append(" + ");
    return numExpressions + 1;
  }

  private static String makeIdent(long id) {
    assert id >= 0;

    StringBuilder b = new StringBuilder();

    // Use only guaranteed-alpha characters for the first character
    b.append(BASE32_CHARS[(int) (id & 0xf)]);
    id >>= 4;

    while (id != 0) {
      b.append(BASE32_CHARS[(int) (id & 0x1f)]);
      id >>= 5;
    }

    return b.toString();
  }

  /**
   * This function validates any context-sensitive Values.
   */
  private static void validateValue(TreeLogger logger,
      JClassType resourceBundleType, Value value)
      throws UnableToCompleteException {

    ListValue list = value.isListValue();
    if (list != null) {
      for (Value v : list.getValues()) {
        validateValue(logger, resourceBundleType, v);
      }
      return;
    }

    DotPathValue dot = value.isDotPathValue();
    if (dot != null) {
      String[] elements = dot.getPath().split("\\.");
      if (elements.length == 0) {
        logger.log(TreeLogger.ERROR, "value() functions must specify a path");
        throw new UnableToCompleteException();
      }

      JType currentType = resourceBundleType;
      for (Iterator<String> i = Arrays.asList(elements).iterator(); i.hasNext();) {
        String pathElement = i.next();

        JClassType referenceType = currentType.isClassOrInterface();
        if (referenceType == null) {
          logger.log(TreeLogger.ERROR, "Cannot resolve member " + pathElement
              + " on non-reference type "
              + currentType.getQualifiedSourceName());
          throw new UnableToCompleteException();
        }

        try {
          JMethod m = referenceType.getMethod(pathElement, new JType[0]);
          currentType = m.getReturnType();
        } catch (NotFoundException e) {
          logger.log(TreeLogger.ERROR, "Could not find no-arg method named "
              + pathElement + " in type "
              + currentType.getQualifiedSourceName());
          throw new UnableToCompleteException();
        }
      }
      return;
    }
  }

  private String classPrefix;
  private JClassType cssResourceType;
  private JClassType elementType;
  private boolean enableMerge;
  private boolean prettyOutput;
  private Map<JClassType, Map<JMethod, String>> replacementsByClassAndMethod;

  private Map<JMethod, String> replacementsForSharedMethods;

  private Map<JMethod, CssStylesheet> stylesheetMap;

  private JClassType stringType;

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {

    SourceWriter sw = new StringSourceWriter();
    // Write the expression to create the subtype.
    sw.println("new " + method.getReturnType().getQualifiedSourceName()
        + "() {");
    sw.indent();

    JClassType cssResourceSubtype = method.getReturnType().isInterface();
    assert cssResourceSubtype != null;
    Map<String, Map<JMethod, String>> replacementsWithPrefix = new HashMap<String, Map<JMethod, String>>();

    replacementsWithPrefix.put("",
        computeReplacementsForType(cssResourceSubtype));
    Import imp = method.getAnnotation(Import.class);
    if (imp != null) {
      boolean fail = false;
      for (Class<? extends CssResource> clazz : imp.value()) {
        JClassType importType = context.getGeneratorContext().getTypeOracle().findType(
            clazz.getName().replace('$', '.'));
        String prefix = importType.getSimpleSourceName();
        ImportedWithPrefix exp = importType.getAnnotation(ImportedWithPrefix.class);
        if (exp != null) {
          prefix = exp.value();
        }
        assert importType != null;

        if (replacementsWithPrefix.put(prefix + "-",
            computeReplacementsForType(importType)) != null) {
          logger.log(TreeLogger.ERROR,
              "Multiple imports that would use the prefix " + prefix);
          fail = true;
        }
      }
      if (fail) {
        throw new UnableToCompleteException();
      }
    }

    /*
     * getOverridableMethods is used to handle CssResources extending
     * non-CssResource types. See the discussion in computeReplacementsForType.
     */
    for (JMethod toImplement : cssResourceSubtype.getOverridableMethods()) {
      String name = toImplement.getName();
      if ("getName".equals(name) || "getText".equals(name)) {
        continue;
      }

      if (toImplement.getReturnType().equals(stringType)
          && toImplement.getParameters().length == 0) {
        writeClassAssignment(sw, toImplement, replacementsWithPrefix.get(""));

      } else if (toImplement.getReturnType().isPrimitive() != null
          && toImplement.getParameters().length == 0) {
        writeDefAssignment(logger, sw, toImplement, stylesheetMap.get(method));

      } else {
        logger.log(TreeLogger.ERROR, "Don't know how to implement method "
            + toImplement.getName());
        throw new UnableToCompleteException();
      }
    }

    sw.println("public String getText() {");
    sw.indent();

    boolean strict = method.getAnnotation(Strict.class) != null;
    if (!strict) {
      /*
       * The developer may choose to force strict behavior onto the system. If
       * the method does already have the @Strict annotation, print a warning.
       */
      try {
        PropertyOracle propertyOracle = context.getGeneratorContext().getPropertyOracle();
        String propertyValue = propertyOracle.getPropertyValue(logger,
            "CssResource.forceStrict");
        if (Boolean.valueOf(propertyValue)) {
          logger.log(TreeLogger.WARN, "CssResource.forceStrict is true, but "
              + method.getName() + "() is missing the @Strict annotation.");
          strict = true;
        }
      } catch (BadPropertyValueException e) {
        // Ignore
      }
    }

    String cssExpression = makeExpression(logger, context, cssResourceSubtype,
        stylesheetMap.get(method), replacementsWithPrefix, strict);
    sw.println("return " + cssExpression + ";");
    sw.outdent();
    sw.println("}");

    sw.println("public String getName() {");
    sw.indent();
    sw.println("return \"" + method.getName() + "\";");
    sw.outdent();
    sw.println("}");

    sw.outdent();
    sw.println("}");

    return sw.toString();
  }

  @Override
  public void init(TreeLogger logger, ResourceContext context)
      throws UnableToCompleteException {
    try {
      PropertyOracle propertyOracle = context.getGeneratorContext().getPropertyOracle();
      String style = propertyOracle.getPropertyValue(logger,
          "CssResource.style").toLowerCase();
      prettyOutput = style.equals("pretty");

      String merge = propertyOracle.getPropertyValue(logger,
          "CssResource.enableMerge").toLowerCase();
      enableMerge = merge.equals("true");

      classPrefix = propertyOracle.getPropertyValue(logger,
          "CssResource.globalPrefix");
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.WARN, "Unable to query module property", e);
      throw new UnableToCompleteException();
    }

    if ("default".equals(classPrefix)) {
      // Compute it later in computeObfuscatedNames();
      classPrefix = null;
    } else if ("empty".equals(classPrefix)) {
      classPrefix = "";
    }

    // Find all of the types that we care about in the type system
    TypeOracle typeOracle = context.getGeneratorContext().getTypeOracle();

    cssResourceType = typeOracle.findType(CssResource.class.getName());
    assert cssResourceType != null;

    elementType = typeOracle.findType(Element.class.getName());
    assert elementType != null;

    stringType = typeOracle.findType(String.class.getName());
    assert stringType != null;

    replacementsByClassAndMethod = new IdentityHashMap<JClassType, Map<JMethod, String>>();
    replacementsForSharedMethods = new IdentityHashMap<JMethod, String>();
    stylesheetMap = new IdentityHashMap<JMethod, CssStylesheet>();

    computeObfuscatedNames(logger);
  }

  @Override
  public void prepare(TreeLogger logger, ResourceContext context,
      ResourceBundleRequirements requirements, JMethod method)
      throws UnableToCompleteException {

    URL[] resources = ResourceGeneratorUtil.findResources(logger, context,
        method);

    if (method.getReturnType().isInterface() == null) {
      logger.log(TreeLogger.ERROR, "Return type must be an interface");
      throw new UnableToCompleteException();
    }

    // Create the AST and do a quick scan for requirements
    CssStylesheet sheet = GenerateCssAst.exec(logger, resources);
    stylesheetMap.put(method, sheet);
    (new RequirementsCollector(logger, requirements)).accept(sheet);
  }

  /**
   * Each distinct type of CssResource has a unique collection of values that it
   * will return, excepting for those methods that are defined within an
   * interface that is tagged with {@code @Shared}.
   */
  private void computeObfuscatedNames(TreeLogger logger) {
    logger = logger.branch(TreeLogger.DEBUG, "Computing CSS class replacements");

    SortedSet<JClassType> cssResourceSubtypes = computeOperableTypes(logger);

    if (classPrefix == null) {
      Adler32 checksum = new Adler32();
      for (JClassType type : cssResourceSubtypes) {
        checksum.update(Util.getBytes(type.getQualifiedSourceName()));
      }
      classPrefix = "G"
          + Long.toString(checksum.getValue(), Character.MAX_RADIX);
    }

    int count = 0;
    for (JClassType type : cssResourceSubtypes) {
      Map<JMethod, String> replacements = new IdentityHashMap<JMethod, String>();
      replacementsByClassAndMethod.put(type, replacements);

      for (JMethod method : type.getOverridableMethods()) {
        String name = method.getName();
        if ("getName".equals(name) || "getText".equals(name)
            || !stringType.equals(method.getReturnType())) {
          continue;
        }

        // The user provided the class name to use
        ClassName classNameOverride = method.getAnnotation(ClassName.class);
        if (classNameOverride != null) {
          name = classNameOverride.value();
        }

        String obfuscatedClassName;
        if (prettyOutput) {
          obfuscatedClassName = classPrefix + "-"
              + type.getQualifiedSourceName().replaceAll("[.$]", "-") + "-"
              + name;
        } else {
          obfuscatedClassName = classPrefix + makeIdent(count++);
        }

        replacements.put(method, obfuscatedClassName);

        if (method.getEnclosingType() == type) {
          Shared shared = type.getAnnotation(Shared.class);
          if (shared != null) {
            replacementsForSharedMethods.put(method, obfuscatedClassName);
          }
        }

        logger.log(TreeLogger.SPAM, "Mapped " + type.getQualifiedSourceName()
            + "." + name + " to " + obfuscatedClassName);
      }
    }
  }

  /**
   * Returns all interfaces derived from CssResource, sorted by qualified name.
   * <p>
   * We'll ignore concrete implementations of CssResource, which include types
   * previously-generated by CssResourceGenerator and user-provided
   * implementations of CssResource, which aren't valid for use with
   * CssResourceGenerator anyway. By ignoring newly-generated CssResource types,
   * we'll ensure a stable ordering, regardless of the actual execution order
   * used by the Generator framework.
   * <p>
   * It is still possible that additional pure-interfaces could be introduced by
   * other generators, which would change the result of this computation, but
   * there is presently no way to determine when, or by what means, a type was
   * added to the TypeOracle.
   */
  private SortedSet<JClassType> computeOperableTypes(TreeLogger logger) {
    logger = logger.branch(TreeLogger.DEBUG,
        "Finding operable CssResource subtypes");

    SortedSet<JClassType> toReturn = new TreeSet<JClassType>(
        new JClassOrderComparator());

    JClassType[] cssResourceSubtypes = cssResourceType.getSubtypes();
    for (JClassType type : cssResourceSubtypes) {
      if (type.isInterface() != null) {
        logger.log(TreeLogger.SPAM, "Added " + type.getQualifiedSourceName());
        toReturn.add(type);

      } else {
        logger.log(TreeLogger.SPAM, "Ignored " + type.getQualifiedSourceName());
      }
    }

    return toReturn;
  }

  /**
   * Compute the mapping of original class names to obfuscated type names for a
   * given subtype of CssResource. Mappings are inherited from the type's
   * supertypes.
   */
  private Map<JMethod, String> computeReplacementsForType(JClassType type) {
    Map<JMethod, String> toReturn = new IdentityHashMap<JMethod, String>();

    /*
     * We check to see if the type is derived from CssResource so that we can
     * handle the case of a CssResource type being derived from a
     * non-CssResource base type. This basically collapses the non-CssResource
     * base types into their least-derived CssResource subtypes.
     */
    if (type == null || !derivedFromCssResource(type)) {
      return toReturn;
    }

    if (replacementsByClassAndMethod.containsKey(type)) {
      toReturn.putAll(replacementsByClassAndMethod.get(type));
    }

    /*
     * Replacements for methods defined in shared types will override any
     * locally-computed values.
     */
    for (JMethod method : type.getOverridableMethods()) {
      if (replacementsForSharedMethods.containsKey(method)) {
        assert toReturn.containsKey(method);
        toReturn.put(method, replacementsForSharedMethods.get(method));
      }
    }

    return toReturn;
  }

  /**
   * Determine if a type is derived from CssResource.
   */
  private boolean derivedFromCssResource(JClassType type) {
    List<JClassType> superInterfaces = Arrays.asList(type.getImplementedInterfaces());
    if (superInterfaces.contains(cssResourceType)) {
      return true;
    }

    JClassType superClass = type.getSuperclass();
    if (superClass != null) {
      if (derivedFromCssResource(superClass)) {
        return true;
      }
    }

    for (JClassType superInterface : superInterfaces) {
      if (derivedFromCssResource(superInterface)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a Java expression that evaluates to the string representation of the
   * stylesheet resource.
   */
  private String makeExpression(TreeLogger logger, ResourceContext context,
      JClassType cssResourceType, CssStylesheet sheet,
      Map<String, Map<JMethod, String>> classReplacementsWithPrefix,
      boolean strict) throws UnableToCompleteException {

    try {

      // Create CSS sprites
      (new Spriter(logger, context)).accept(sheet);

      // Perform @def and @eval substitutions
      SubstitutionCollector collector = new SubstitutionCollector();
      collector.accept(sheet);

      (new SubstitutionReplacer(logger, context, collector.substitutions)).accept(sheet);

      // Evaluate @if statements based on deferred binding properties
      (new IfEvaluator(logger,
          context.getGeneratorContext().getPropertyOracle())).accept(sheet);

      // Rename css .class selectors
      (new ClassRenamer(logger, classReplacementsWithPrefix, strict)).accept(sheet);

      // Combine rules with identical selectors
      if (enableMerge) {
        // TODO This is an off-switch while this is being developed; remove
        (new SplitRulesVisitor()).accept(sheet);
        (new MergeIdenticalSelectorsVisitor()).accept(sheet);
        (new MergeRulesByContentVisitor()).accept(sheet);
      }

      String standard = makeExpression(logger, context, cssResourceType, sheet,
          prettyOutput);

      (new RtlVisitor()).accept(sheet);

      String reversed = makeExpression(logger, context, cssResourceType, sheet,
          prettyOutput);

      return "com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ? ("
          + reversed + ") : (" + standard + ")";

    } catch (CssCompilerException e) {
      // Take this as a sign that one of the visitors was unhappy, but only
      // log the stack trace if there's a causal (i.e. unknown) exception.
      logger.log(TreeLogger.ERROR, "Unable to process CSS",
          e.getCause() == null ? null : e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Write the CssResource accessor method for simple String return values.
   */
  private void writeClassAssignment(SourceWriter sw, JMethod toImplement,
      Map<JMethod, String> classReplacements) {

    String replacement = classReplacements.get(toImplement);
    assert replacement != null;

    sw.println(toImplement.getReadableDeclaration(false, true, true, true, true)
        + "{");
    sw.indent();
    sw.println("return \"" + replacement + "\";");
    sw.outdent();
    sw.println("}");
  }

  private void writeDefAssignment(TreeLogger logger, SourceWriter sw,
      JMethod toImplement, CssStylesheet cssStylesheet)
      throws UnableToCompleteException {
    SubstitutionCollector collector = new SubstitutionCollector();
    collector.accept(cssStylesheet);

    String name = toImplement.getName();
    // TODO: Annotation for override

    CssDef def = collector.substitutions.get(name);
    if (def == null) {
      logger.log(TreeLogger.ERROR, "No @def rule for name " + name);
      throw new UnableToCompleteException();
    }

    // TODO: Allow returning an array of values
    if (def.getValues().size() != 1) {
      logger.log(TreeLogger.ERROR, "@def rule " + name
          + " must define exactly one value");
      throw new UnableToCompleteException();
    }

    NumberValue numberValue = def.getValues().get(0).isNumberValue();

    if (numberValue == null) {
      logger.log(TreeLogger.ERROR, "The define named " + name
          + " does not define a numeric value");
      throw new UnableToCompleteException();
    }

    JPrimitiveType returnType = toImplement.getReturnType().isPrimitive();
    assert returnType != null;

    sw.print(toImplement.getReadableDeclaration(false, false, false, false,
        true));
    sw.println(" {");
    sw.indent();
    if (returnType == JPrimitiveType.INT || returnType == JPrimitiveType.LONG) {
      sw.println("return " + Math.round(numberValue.getValue()) + ";");
    } else if (returnType == JPrimitiveType.FLOAT) {
      sw.println("return " + numberValue.getValue() + "F;");
    } else if (returnType == JPrimitiveType.DOUBLE) {
      sw.println("return " + numberValue.getValue() + ";");
    } else {
      logger.log(TreeLogger.ERROR, returnType.getQualifiedSourceName()
          + " is not a valid return type for @def accessors");
      throw new UnableToCompleteException();
    }
    sw.outdent();
    sw.println("}");

    numberValue.getValue();
  }
}
