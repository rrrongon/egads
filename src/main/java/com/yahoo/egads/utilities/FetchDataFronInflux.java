package com.yahoo.egads.utilities;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by rongon on 4/16/17.
 */
public class FetchDataFronInflux {

    public List<List<Object>> getInfluxData(){
        InfluxDB influxDB = InfluxDBFactory.connect("http://10.10.10.84:8086","root","");
        String dbName = "telegraf";

        //Query query = new Query("select \"usage_user\"::field from \"cpu\" where cpu='cpu-total' and dc= 'openstack' and host='computenode1.devops.allcolo.com' and time > now() - 7d", dbName); Olympic model
        //diskio,dc=etl1,host=hadoop1,name=dm-0

        // query over any of the SERIES of influxDB with your own series. in my case bellow is a SERIES.
        Query query = new Query("select \"interrupts\"::field from \"kernel\" where \"dc\"= 'openstack' and \"host\"='cinderapi1.devops.allcolo.com'  and time > now() - 7d", dbName);
        // simply run the query. this returns QueryResult type object by java InfluxDBClient.
        QueryResult result = influxDB.query(query);

        for(int i=0;i<result.getResults().size();i++){
            //System.out.println(result.getResults().get(i).getSeries().get(i).getValues());
            //System.out.println(result.getResults().get(i).getSeries().get(i).getClass());
        }

        QueryResult.Series series = (QueryResult.Series) result.getResults().get(0).getSeries().get(0);
        for (int i = 0; i< series.getValues().size();i++){

            String yourString = (String) series.getValues().get(i).get(0);
            //System.out.println(series.getValues().get(i).get(0));
            Timestamp timestamp = null;
            long unixTime = 0;
            try{
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                //Date parsedDate = dateFormat.parse(yourString);
                unixTime = (long) dateFormat.parse(yourString).getTime();
                unixTime = unixTime/1000;

            }catch(Exception e){//this generic but you can control another types of exception
                e.printStackTrace();
            }
            //System.out.println("index "  + i + "timestamp" + unixTime+ "value " + series.getValues().get(i));
        }


        // return the series values i.e list of a list
        return  series.getValues();
        /*List<Object> eachResult = new ArrayList<Object>();

        for (int i =0 ; i<series.getValues().size();i++) {
            System.out.println(series.getValues().get(i));
            //eachResult.add(series.getValues().get(i));
            System.out.println("time : "+ series.getValues().get(i).get(0));
            System.out.println("value : "+ series.getValues().get(i).get(1));
        }*/
    }

}
