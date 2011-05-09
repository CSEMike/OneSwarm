package edu.washington.cs.oneswarm.ui.gwt.server.community;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONWriter;

import com.google.common.base.Preconditions;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

// TODO(piatek): Add support for callbacks/error handling for puts. For now, all best-effort.
public class CHTPutOp extends CommunityServerOperation {

    private static Logger logger = Logger.getLogger(CHTPutOp.class.getName());

    private final List<byte[]> values;
    private final List<byte[]> keys;

    public CHTPutOp(CommunityRecord record, List<byte[]> keys, List<byte[]> values) {
        super(record);
        this.keys = keys;
        this.values = values;
    }

    @Override
    void doOp() {
        Preconditions.checkState(mRecord.isAllowAddressResolution(),
                "Attempting CHTPut on server without perms: " + mRecord.getBaseURL());

        Preconditions.checkState(mRecord.getCht_path() != null,
                "Attempting CHTPut on server without a valid CHT path! " + mRecord.getBaseURL());

        String path = mRecord.getBaseURL();
        if (path.endsWith("/") == false) {
            path += "/";
        }
        path += mRecord.getCht_path() + "?put";
        try {
            URL url = new URL(path);
            HttpURLConnection conn = getConnection(url, "POST");

            // JSON array of key, value pairs.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(baos);
            JSONWriter writer = new JSONWriter(outputStreamWriter);

            writer.array();
            for (int i = 0; i < keys.size(); i++) {
                writer.array();
                String encodedKey = Base64.encode(keys.get(i));
                writer.value(encodedKey);
                String encodedValue = Base64.encode(values.get(i));
                writer.value(encodedValue);
                writer.endArray();
            }
            writer.endArray();
            outputStreamWriter.flush();

            System.out.println(baos.toString() + "\n");

            conn.getOutputStream().write(
                    ("q=" + URLEncoder.encode(baos.toString(), "UTF-8")).getBytes());

            System.out.println("CHT put response code: " + conn.getResponseCode() + " / "
                    + conn.getResponseMessage());

        } catch (Exception e) {
            e.printStackTrace();
            logger.warning("Error during CHT Put on server: " + mRecord.getBaseURL() + " / "
                    + e.toString());
        }
    }

}
