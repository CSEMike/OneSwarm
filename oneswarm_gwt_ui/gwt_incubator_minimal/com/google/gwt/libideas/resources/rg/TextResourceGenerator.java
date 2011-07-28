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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dev.util.Util;
import com.google.gwt.libideas.resources.client.TextResource;
import com.google.gwt.libideas.resources.ext.ResourceContext;
import com.google.gwt.libideas.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.libideas.resources.rebind.StringSourceWriter;
import com.google.gwt.user.rebind.SourceWriter;

import java.net.URL;

/**
 * Provides implementations of TextResource.
 */
public final class TextResourceGenerator extends AbstractResourceGenerator {

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {
    URL[] resources = ResourceGeneratorUtil.findResources(logger, context,
        method);

    if (resources.length != 1) {
      logger.log(TreeLogger.ERROR, "Exactly one resource must be specified",
          null);
      throw new UnableToCompleteException();
    }

    URL resource = resources[0];

    SourceWriter sw = new StringSourceWriter();
    // Write the expression to create the subtype.
    sw.println("new " + TextResource.class.getName() + "() {");
    sw.indent();

    // Convenience when examining the generated code.
    sw.println("// " + resource.toExternalForm());

    sw.println("public String getText() {");
    sw.indent();

    TreeLogger transformLogger = logger.branch(TreeLogger.DEBUG,
        "Applying Transformers", null);
    String toWrite = ResourceGeneratorUtil.applyTransformations(
        transformLogger, method, String.class, new String(
            Util.readURLAsChars(resource)));

    sw.println("return \"" + Generator.escape(toWrite).replaceAll("\0", "\\0")
        + "\";");
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
}
