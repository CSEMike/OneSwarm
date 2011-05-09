/**
 * This file is part of jFlvTool.
 *
 * jFlvTool is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * jFlvTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * file name  : EmbeddedData.java
 * authors    : Jon Keys
 * created    : June 29, 2007, 3:32 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 29, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.text.DecimalFormat;

/**
 * 
 * @author Jon Keys
 */
public class EmbeddedData {

    private static DecimalFormat decf = new DecimalFormat("#####0.00###");
    private HashMap<String, Object> metadata;

    /** Creates a new instance of EmbeddedData */
    public EmbeddedData() {
        metadata = new HashMap<String, Object>();

    }

    public void addData(String key, Object value) {
        metadata.put(key, value);
    }

    public void removeData(String key) {

        try {
            metadata.remove(key);
        } catch (Exception e) {
            // do nothing
        }

    }// removeData()

    public HashMap<String, Object> getData() {
        return metadata;
    }

    public void setData(HashMap<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String printMetaData() {
        return prettyPrintData(metadata);
    }

    public static String prettyPrintData(Object obj) {

        String objClass = obj.getClass().getName();
        StringBuffer metaprint = new StringBuffer();

        if (objClass.endsWith("String")) {
            metaprint.append((String) obj);
        } else if (objClass.endsWith("Double")) {
            metaprint.append(decf.format(((Double) obj).doubleValue()) + "");
        } else if (objClass.endsWith("Boolean")) {
            metaprint.append(((Boolean) obj).toString());
        } else if (objClass.endsWith("HashMap")) {
            metaprint.append(printMixedData((HashMap<String, Object>) obj));
        } else if (objClass.endsWith("ArrayList")) {
            metaprint.append(printArrayData((ArrayList<Object>) obj));
        } else if (objClass.endsWith("AMFTime")) {
            metaprint.append(((AMFTime) obj).getTimeString());
        } else if (objClass.endsWith("AMFObject")) {
            metaprint.append(printObjectData((AMFObject) obj));
        }

        objClass = null;

        return metaprint.toString();

    }// prettyPrintData()

    private static String printObjectData(AMFObject amfObj) {

        String varname = null;
        Object var = null;
        StringBuffer mixprint = new StringBuffer();

        Iterator it = amfObj.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry entry = (Map.Entry) it.next();
            varname = (String) entry.getKey();
            var = (Object) entry.getValue();

            if (varname != null && varname.length() > 0 && var != null) {
                mixprint.append(varname + " : " + prettyPrintData(var) + "\n");
            }

        }// while

        varname = null;
        var = null;
        it = null;

        return mixprint.toString();

    }// printTimeData()

    private static String printArrayData(ArrayList<Object> arrObj) {

        StringBuffer arrprint = new StringBuffer();

        for (Object obj : arrObj) {
            arrprint.append("\n\t" + prettyPrintData(obj));
        }

        arrprint.append("\n");
        return arrprint.toString();

    }// printArrayData()

    private static String printMixedData(HashMap<String, Object> mapObj) {

        String varname = null;
        Object var = null;
        StringBuffer mixprint = new StringBuffer();

        Iterator it = mapObj.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry entry = (Map.Entry) it.next();
            varname = (String) entry.getKey();
            var = (Object) entry.getValue();

            if (varname != null && varname.length() > 0 && var != null) {
                mixprint.append(varname + " : " + prettyPrintData(var) + "\n");
            }

        }// while

        varname = null;
        var = null;
        it = null;

        return mixprint.toString();

    }// printMixedData()

}// EmbeddedData
