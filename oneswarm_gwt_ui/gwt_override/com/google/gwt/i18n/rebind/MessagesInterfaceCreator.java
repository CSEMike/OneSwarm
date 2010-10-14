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
package com.google.gwt.i18n.rebind;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.rebind.MessageFormatParser.ArgumentChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.TemplateChunk;

/**
 * Creates a MessagesInterface from a Resource file.
 */
public class MessagesInterfaceCreator extends AbstractLocalizableInterfaceCreator {

	/**
	 * Searches for MessageFormat-style args in the template string and returns
	 * a set of argument indices seen.
	 * 
	 * @param template
	 *            template to parse
	 * @return set of argument indices seen
	 * @throws ParseException
	 *             if the template is incorrect.
	 */
	private static Set<Integer> numberOfMessageArgs(String template) throws ParseException {
		Set<Integer> seenArgs = new HashSet<Integer>();
		for (TemplateChunk chunk : MessageFormatParser.parse(template)) {
			if (chunk instanceof ArgumentChunk) {
				seenArgs.add(((ArgumentChunk) chunk).getArgumentNumber());
			}
		}
		return seenArgs;
	}

	private static Map<Integer, String> numberOfMessageArgsWithType(String template) throws ParseException {
		Map<Integer, String> seenArgs = new HashMap<Integer, String>();
		for (TemplateChunk chunk : MessageFormatParser.parse(template)) {
			if (chunk instanceof ArgumentChunk) {
				ArgumentChunk ac = (ArgumentChunk) chunk;
				String javaType = "String";
				String format = ac.getFormat();
				String subFormat = ac.getSubFormat();
				if (format != null) {
					if (format.equals("number")) {
						javaType = "double";
						if (subFormat != null) {
							if (subFormat.equals("integer")) {
								javaType = "int";
							}
						}
					}
				}
				seenArgs.put(ac.getArgumentNumber(), javaType);
			}
		}
		return seenArgs;
	}

	/**
	 * Constructor for <code>MessagesInterfaceCreator</code>.
	 * 
	 * @param className
	 *            class name
	 * @param packageName
	 *            package name
	 * @param resourceBundle
	 *            resource bundle
	 * @param targetLocation
	 *            target location
	 * @throws IOException
	 */
	public MessagesInterfaceCreator(String className, String packageName, File resourceBundle, File targetLocation) throws IOException {
		super(className, packageName, resourceBundle, targetLocation, Messages.class);
	}

	@Override
	protected void genMethodArgs(String defaultValue) {
		try {
			Map<Integer, String> seenArgs = numberOfMessageArgsWithType(defaultValue);
			int maxArgSeen = -1;
			for (int arg : seenArgs.keySet()) {
				if (arg > maxArgSeen) {
					maxArgSeen = arg;
				}
			}
			for (int i = 0; i <= maxArgSeen; i++) {
				if (i > 0) {
					composer.print(",  ");
				}
				if (!seenArgs.containsKey(i)) {
					composer.print("@Optional ");
				}
				composer.print(seenArgs.get(i) + " arg" + i);
			}
		} catch (ParseException e) {
			throw new RuntimeException(defaultValue + " could not be parsed as a MessageFormat string.", e);
		}
	}

	@Override
	protected void genValueAnnotation(String defaultValue) {
		composer.println("@DefaultMessage(" + makeJavaString(defaultValue) + ")");
	}

	@Override
	protected String javaDocComment(String path) {
		return "Interface to represent the messages contained in resource bundle:\n\t" + path + "'.";
	}
}
