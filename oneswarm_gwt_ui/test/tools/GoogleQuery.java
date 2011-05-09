package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleQuery {

    private BufferedWriter out;
    private final static String googleBaseQueryUrl = "http://www.google.com/search?q=";
    private final static String yahooBaseQueryUrl = "http://search.yahoo.com/search?p=";

    public static void main(String[] args) {
        try {
            new GoogleQuery("tcp+port", 1024, 65536, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public GoogleQuery(String queryString, int from, int to, boolean useYahoo) throws IOException,
            InterruptedException {
        out = new BufferedWriter(new FileWriter(new File("query" + System.currentTimeMillis())));
        Integer num = to - from + 1;

        Integer[] queries = new Integer[num];

        for (int i = 0; i < queries.length; i++) {
            int val = from + i;
            queries[i] = val;
        }

        List<Integer> shuffled = new ArrayList<Integer>();
        Collections.addAll(shuffled, queries);
        Collections.shuffle(shuffled);

        for (Integer val : shuffled) {
            String fulllQueryString;

            if (useYahoo) {
                fulllQueryString = yahooBaseQueryUrl + queryString + "+" + val;

            } else {
                fulllQueryString = googleBaseQueryUrl + queryString + "+" + val;
            }
            URL url = new URL(fulllQueryString);

            HttpURLConnection m_con = (HttpURLConnection) url.openConnection();
            m_con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.8.1.6) Gecko/20070725 Firefox/2.0.0.6");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(m_con.getInputStream()));
            String line;

            boolean found = false;
            while ((line = reader.readLine()) != null && !found) {
                // System.out.println(line + "\n\n");
                String split[] = line.split(" ");

                for (int i = 0; i < split.length - 3 && !found; i++) {
                    if (split[i].equals("of") && split[i + 1].equals("about")) {
                        String pageNum;
                        if (useYahoo) {
                            pageNum = split[i + 2];
                        } else {
                            pageNum = split[i + 2].substring(3, split[i + 2].length() - 4);
                        }
                        int pageInt = Integer.parseInt(pageNum.replaceAll(",", ""));
                        System.out.println(val + "\t" + pageInt);
                        out.write(val + "\t" + pageInt + "\n");
                        out.flush();
                        found = true;
                    }
                }
            }
            reader.close();
            m_con.disconnect();

            if (!found) {
                System.out.println("did not find anything, stange? got: " + line);
                System.exit(1);
            }

            // sleep 30 sek + random*60 sek, average one query per min
            long sleepTime = Math.round(30 * 1000 + Math.random() * 60 * 1000);
            System.out.println("sleeping: " + (sleepTime / 1000));
            Thread.sleep(sleepTime);
        }
        out.close();
    }
}
