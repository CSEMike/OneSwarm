/**
 * Created on 10-Jan-2006
 * Created by Allan Crooks
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.core3.xml.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

public class XMLElement {

    protected Object single_content;
    protected Map attributes;
    protected Collection contents;
    protected String tag_name;
    protected boolean auto_order;

    public XMLElement(String tag_name) {
        this(tag_name, false);
    }

    public XMLElement(String tag_name, boolean auto_order) {
        this.single_content = null;
        this.attributes = null;
        this.contents = null;
        this.tag_name = tag_name;
        this.auto_order = auto_order;
    }

    public String getTag() {
        return tag_name;
    }

    public String getAttribute(String key) {
        if (this.attributes == null) {return null;}
        return (String)this.attributes.get(key);
    }

    public void addAttribute(String key, String value) {
        if (attributes == null) {
            this.attributes = new TreeMap(ATTRIBUTE_COMPARATOR);
        }
        this.attributes.put(key, value);
    }

    public void addAttribute(String key, int value) {
        this.addAttribute(key, String.valueOf(value));
    }

    public void addAttribute(String key, boolean value) {
        this.addAttribute(key, (value) ? "yes" : "no");
    }

    public void addContent(String s) {addContent((Object)s);}
    public void addContent(XMLElement e) {addContent((Object)e);}

    protected void addContent(Object o) {
        if (o == null)
            throw new NullPointerException();

        if (this.contents == null) {
            if (this.single_content != null) {
                if (!this.auto_order) {
                    this.contents = new ArrayList();
                }
                else {
                    this.contents = new TreeSet(CONTENT_COMPARATOR);
                }
                this.contents.add(this.single_content);
                this.single_content = null;
            }
        }

        if (this.contents == null) {
            this.single_content = o;
        }
        else {
            this.contents.add(o);
        }
    }

    public void printTo(PrintWriter pw) {
        printTo(pw, 0, false);
    }

    public void printTo(PrintWriter pw, boolean spaced_out) {
        printTo(pw, 0, spaced_out);
    }

    public void printTo(PrintWriter pw, int indent) {
        printTo(pw, indent, false);
    }

    public void printTo(PrintWriter pw, int indent, boolean spaced_out) {

        for (int i=0; i<indent; i++) {pw.print(" ");}

        if (this.attributes == null && this.contents == null && this.single_content == null) {

            if (!spaced_out) {
                pw.print("<");
                pw.print(this.tag_name);
                pw.print(" />");
            }
            return;
        }

        pw.print("<");
        pw.print(this.tag_name);
        Iterator itr = null;

        if (this.attributes != null) {
            itr = this.attributes.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry entry = (Map.Entry)itr.next();
                pw.print(" ");
                pw.print(entry.getKey());
                pw.print("=\"");
                pw.print(quote((String)entry.getValue()));
                pw.print("\"");
            }
        }

        boolean needs_indented_close = (this.contents != null || this.single_content instanceof XMLElement);
        boolean needs_close_tag = needs_indented_close || this.single_content != null;

        needs_indented_close = needs_indented_close || spaced_out;
        needs_close_tag = needs_close_tag || spaced_out;

        if (needs_indented_close) {pw.println(">");}
        else if (needs_close_tag) {pw.print(">");}
        else {pw.print(" />");}

        itr = null;
        if (this.contents != null) {
            itr = this.contents.iterator();
        }
        else if (this.single_content != null) {
            itr = Collections.singletonList(this.single_content).iterator();
        }
        else {
            itr = Collections.singletonList("").iterator();
        }

        Object content_element = null;
        if (itr != null) {
            while (itr.hasNext()) {
                content_element = itr.next();
                if (content_element instanceof XMLElement) {
                    ((XMLElement)content_element).printTo(pw, indent+2, spaced_out);
                }
                else if (spaced_out) {
                    for (int i=0; i<indent+2; i++) {pw.print(" ");}
                    pw.print(quote((String)content_element));
                    pw.println();
                }
                else {
                    pw.print(quote((String)content_element));
                }
            }
        }

        if (needs_indented_close) {
            for (int i=0; i<indent; i++) {pw.print(" ");}
        }

        if (needs_close_tag) {
            pw.print("</");
            pw.print(this.tag_name);
            pw.println(">");
        }
    }

    private String quote(String text) {
        text = text.replaceAll( "&", "&amp;" );
        text = text.replaceAll( ">", "&gt;" );
        text = text.replaceAll( "<", "&lt;" );
        text = text.replaceAll( "\"", "&quot;" );
        text = text.replaceAll( "--", "&#45;&#45;" );
        return text;
    }

    public XMLElement makeContent(String tag_name) {
        return this.makeContent(tag_name, false);
    }

    public XMLElement makeContent(String tag_name, boolean auto_order) {
        XMLElement content = new XMLElement(tag_name, auto_order);
        this.addContent(content);
        return content;
    }

    public void clear() {
        this.single_content = null;
        this.attributes = null;
        this.contents = null;
    }

    public void setAutoOrdering(boolean mode) {
        if (mode == this.auto_order) return;
        this.auto_order = mode;
        if (this.contents == null) return;
        Collection previous_contents = contents;
        if (this.auto_order) {
            this.contents = new TreeSet(CONTENT_COMPARATOR);
            this.contents.addAll(previous_contents);
        }
        else {
            this.contents = new ArrayList(previous_contents);
        }
    }

    public String toString() {
        return "XMLElement[" + this.tag_name + "]@" + Integer.toHexString(System.identityHashCode(this));
    }

    private static Comparator ATTRIBUTE_COMPARATOR = String.CASE_INSENSITIVE_ORDER;

    private static class ContentComparator implements java.util.Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof XMLElement) {
                if (o2 instanceof XMLElement) {
                    XMLElement xe1 = (XMLElement)o1;
                    XMLElement xe2 = (XMLElement)o2;
                    int result = String.CASE_INSENSITIVE_ORDER.compare(xe1.getTag(), xe2.getTag());
                    if (result == 0) {
                        int xe1_index = 0, xe2_index = 0;
                        try {
                            xe1_index = Integer.parseInt(xe1.getAttribute("index"));
                            xe2_index = Integer.parseInt(xe2.getAttribute("index"));
                        }
                        catch (NullPointerException ne) {
                            xe1_index = xe2_index = 0;
                        }
                        catch (NumberFormatException ne) {
                            xe1_index = xe2_index = 0;
                        }

                        if (xe1_index != xe2_index) {
                            return xe1_index - xe2_index;
                        }

                        throw new RuntimeException("Shouldn't be using sorting for contents if you have tags with same name and no index attribute! (e.g. " + o1 + ")");
                    }
                    return result;
                }
                else {
                    return -1; // XMLElements before strings.
                }
            }
            else {
                if (o2 instanceof XMLElement) {
                    return 1; // XMLElements before strings.
                }
                else {
                    // Can't allow the returning of 0.
                    int result = String.CASE_INSENSITIVE_ORDER.compare((String)o1, (String)o2);
                    if (result == 0) {return -1;}
                    return result;
                }
            }
        }
    }

    private static Comparator CONTENT_COMPARATOR = new ContentComparator();

}
