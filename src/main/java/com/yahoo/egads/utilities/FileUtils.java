/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// A utility for creating an array of timeseries objects from the
// csv file.

package com.yahoo.egads.utilities;


import com.yahoo.egads.data.TimeSeries;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {
    
    // Creates a time-series from a file.
    public static ArrayList<TimeSeries> createTimeSeries(String csv_file, Properties config) {

        // Input file which needs to be parsed
        String fileToParse = csv_file;
        BufferedReader fileReader = null;
        ArrayList<TimeSeries> output = new ArrayList<TimeSeries>();

        // Delimiter used in CSV file
        final String delimiter = ",";
        Long interval = null;
        Long prev = null;
        Integer aggr = 1;
        boolean fillMissing = false;
        if (config.getProperty("FILL_MISSING") != null && config.getProperty("FILL_MISSING").equals("1")) {
        	fillMissing = true;
        }
        if (config.getProperty("AGGREGATION") != null) {
            aggr = new Integer(config.getProperty("AGGREGATION"));
        }
        try {
            String line = "";
            // Create the file reader.
            fileReader = new BufferedReader(new FileReader(fileToParse));

            // Read the file line by line
            boolean firstLine = true;

            //initialize class which will fetch data from influx
            FetchDataFronInflux influxData = new FetchDataFronInflux();
            List<List<Object>> datas = new ArrayList<List<Object>>();
            datas = influxData.getInfluxData();

            long dataSize = datas.size();


            for (int k =0 ; k<datas.size();k++) {
                // Get all tokens available in line.

                // Java influxDB client returns timestamp in yyyy-MM-dd'T'HH:mm:ss'Z' this format. so, to make it compatible
                // we must convert this time to our desired timestamp and so we convert like bellow.
                String yourString = (String) datas.get(k).get(0);
                Timestamp timestamp = null;
                long unixTime = 0;
                try{
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    unixTime = (long) dateFormat.parse(yourString).getTime();
                    unixTime = unixTime/1000;

                }catch(Exception e){//this generic but you can control another types of exception
                    e.printStackTrace();
                }

                String[] tokens = new String[2] ;

                // this unixtime is our converted time stamp and so our code continues from here.
                tokens[0] = "" + unixTime;
                tokens[1] = ""+ datas.get(k).get(1);


                Long curTimestamp = null;
                
                // Check for the case where there is more than one line preceding the data 
                if (firstLine == true) {
                    if (!isNumeric(tokens[0]) && tokens[0].equals("timestamp") == false) {
                        continue;
                    }
                }
                if (firstLine == false && tokens.length > 1) {
                    curTimestamp = (new Double(tokens[0])).longValue();
                }
                for (int i = 1; i < tokens.length; i++) {
                    // Assume that the first line contains the column names.
                    if (firstLine) {
                        TimeSeries ts = new TimeSeries();
                        ts.meta.fileName = csv_file;
                        output.add(ts);
                        if (isNumeric(tokens[i]) == false) { // Just in case there's a numeric column heading
                            ts.meta.name = tokens[i];
                        } else {
                            ts.meta.name = "metric_" + i;
                            output.get(i - 1).append((new Double(tokens[0])).longValue(),
                                    new Float(tokens[i]));
                        }
                    } else {
                        // A naive missing data handler.
                        if (interval != null && prev != null && interval > 0 && fillMissing == true) {
                            if ((curTimestamp - prev) != interval) {
                                int missingValues = (int) ((curTimestamp - prev) / interval);
                                
                                Long curTimestampToFill = prev + interval;
                                for (int j = (missingValues - 1); j > 0; j--) {
                                    Float valToFill =  new Float(tokens[i]);
                                    if (output.get(i - 1).size() >= missingValues) {
                                        valToFill = output.get(i - 1).data.get(output.get(i - 1).size() - missingValues).value;
                                    }
                                    output.get(i - 1).append(curTimestampToFill, valToFill);
                                    curTimestampToFill += interval;
                                }
                            }
                        }
                        // Infer interval.
                        if (interval == null && prev != null) {
                            interval = curTimestamp - new Long(prev);
                        }
                        
                        output.get(i - 1).append(curTimestamp,
                                new Float(tokens[i]));
                    }
                }
                if (firstLine == false) {
                    prev = curTimestamp;
                }
                firstLine = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Handle aggregation.
        if (aggr > 1) {
            for (TimeSeries t : output) {
                t.data = t.aggregate(aggr);
                t.meta.name += "_aggr_" + aggr;
            }
        }
        return output;
    }
        
    // Checks if the string is numeric.
    public static boolean isNumeric(String str) {  
        try {  
            Double.parseDouble(str);  
        } catch (NumberFormatException nfe) {  
            return false;  
        }  
        return true;  
    }
    
    // Parses the string array property into an integer property.
public static int[] splitInts(String str) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        int n = tokenizer.countTokens();
        int[] list = new int[n];
        for (int i = 0; i < n; i++) {
          String token = tokenizer.nextToken();
          list[i] = Integer.parseInt(token);
        }
        return list;
      }
    
    // Initializes properties from a string (key:value, separated by ";").
    public static void initProperties(String config, Properties p) {
    	String delims1 = ";";
    	String delims2 = ":";
 
		StringTokenizer st1 = new StringTokenizer(config, delims1);
		while (st1.hasMoreElements()) {
			String[] st2 = (st1.nextToken()).split(delims2);
			p.setProperty(st2[0], st2[1]);
		}
    }
}
