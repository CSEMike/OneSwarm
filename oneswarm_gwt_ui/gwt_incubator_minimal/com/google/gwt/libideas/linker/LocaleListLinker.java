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

package com.google.gwt.libideas.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;

/**
 * Produces a list of locales as a linker artifact for use by server-side code.
 * 
 * The locale list, which will include the default locale, will be stored in a
 * text file [linkerName]/locale-list.txt in the module's auxilliary output
 * directory (typically www/moduleName-aux/localeList/locale-list.txt).
 * 
 * Example .gwt.xml usage:
 * <pre>
 *  <define-linker name="localeList" class="com.google.gwt.libideas.linker.LocaleListLinker"/>
 *  <add-linker name="localeList"/>
 * </pre>
 */
@LinkerOrder(Order.PRE)
public class LocaleListLinker extends AbstractLinker {

  @Override
  public String getDescription() {
    return "LocaleListLinker";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context, ArtifactSet inArtifacts)
      throws UnableToCompleteException {
    for (SelectionProperty property : context.getProperties()) {
      if (property.getName().equals("locale")) {
        StringBuffer buf = new StringBuffer();
        for (String locale : property.getPossibleValues()) {
          buf.append(locale);
          buf.append('\n');
        }
        ArtifactSet artifacts = new ArtifactSet(inArtifacts);
        SyntheticArtifact localeListArtifact = emitString(logger, buf.toString(),
            "locale-list.txt");
        localeListArtifact.setPrivate(true);
        artifacts.add(localeListArtifact);
        return artifacts;
      }
    }
    logger.log(
        TreeLogger.WARN,
        "LocaleListLinker - No locale property defined, did you inherit the i18n module?",
        null);
    return inArtifacts;
  }
}
