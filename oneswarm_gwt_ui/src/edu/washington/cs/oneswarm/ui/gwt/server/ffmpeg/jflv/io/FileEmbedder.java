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
 * file name  : FileEmbedder.java
 * authors    : Jon Keys
 * created    : July 9, 2007, 8:40 AM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 9, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.BufferedReader;
import java.io.FileReader;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.EmbeddedData;

/**
 * 
 * @author Jon Keys
 */
public class FileEmbedder {

    private boolean debug;
    private EmbeddedData emb;

    /** Creates a new instance of FileEmbedder */
    public FileEmbedder(EmbeddedData emb) {
        this.debug = false;
        this.emb = emb;
    }

    public void embedFile(String varname, String filepath) {

        StringBuffer filecontents = new StringBuffer();

        try {

            BufferedReader in = new BufferedReader(new FileReader(filepath));
            String str = null;

            while ((str = in.readLine()) != null) {
                filecontents.append(str);
            }

            in.close();

        } catch (Exception e) {

            System.out.println("Error - could not read external metatdata file");
            if (debug) {
                e.printStackTrace();
            }

        }// catch()

        emb.addData(varname, filecontents.toString());

    }// embedFile()

    public void embedVars(String[] vars) {

        int index = 0;

        for (String str : vars) {

            try {
                index = str.indexOf("=");
                embedVar(str.substring(0, index), str.substring(index + 1));
            } catch (Exception e) {
                System.out.println("Error parsing embed vars ... skipping malformed pair '" + str
                        + "'");
            }

        }// for

    }// embedVars()

    public void embedVar(String varname, String value) {
        emb.addData(varname, value);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}// FileEmbedder
