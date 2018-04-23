package com.msoe.poonachatheethiran.weathermonitor;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by poonachatheethiran on 4/7/2018.
 * Used to manage weather data download service and all related functions.
 */

public class DownloadService extends IntentService {
    public static final String DownloadServiceIntentFilter = "com.msoe.poonachatheethiran.weathermonitor";
    public DownloadService(){
        super("DownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Toast.makeText(this, "Service created", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final JSONObject json=getJSON(this, intent.getStringExtra("city"));
        try {
            JSONArray list = json.getJSONArray("list");
            ArrayList<String> condition= new ArrayList<String>();
            ArrayList<String> humidity= new ArrayList<String>();
            ArrayList<String> pressure= new ArrayList<String>();
            ArrayList<String> temp= new ArrayList<String>();
            ArrayList<String> wind= new ArrayList<String>();
            ArrayList<String> date= new ArrayList<String>();

            for(int i=0;i<list.length();i++)
            {
                JSONObject data_point=list.getJSONObject(i);
                JSONObject details = data_point.getJSONArray("weather").getJSONObject(0);
                JSONObject main = data_point.getJSONObject("main");

                condition.add(details.getString("description").toUpperCase(Locale.US));
                humidity.add(main.getString("humidity"));
                pressure.add(main.getString("pressure"));
                temp.add(String.format("%.2f", main.getDouble("temp")));
                wind.add(data_point.getJSONObject("wind").getString("speed"));
                date.add(Long.toString(data_point.getLong("dt")));
            }

            Intent i = new Intent(DownloadServiceIntentFilter);
            i.putStringArrayListExtra("Weather Condition", condition);
            i.putStringArrayListExtra("Humidity", humidity);
            i.putStringArrayListExtra("Pressure", pressure);
            i.putStringArrayListExtra("Temperature", temp);
            i.putStringArrayListExtra("Wind speed", wind);
            i.putStringArrayListExtra("Time Updated", date);
            sendBroadcast(i);

        }catch(Exception e){
            Log.i("JSON", "One or more fields not found in the JSON data");
        }
    }
    /**
    Used to ger raw weather data in JSON format from openweathermap API
     */
    public static JSONObject getJSON(Context context, String city){
        //String OPEN_WEATHER_MAP_API = "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric";
        String OPEN_WEATHER_MAP_API = "http://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric";
        try {
            URL url = new URL(String.format(OPEN_WEATHER_MAP_API, city));
            HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();

            connection.addRequestProperty("x-api-key",
                    context.getString(R.string.open_weather_maps_app_id));

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            StringBuffer json = new StringBuffer(1024);
            String tmp="";
            while((tmp=reader.readLine())!=null)
                json.append(tmp).append("\n");
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // This value will be 404 if the request was not
            // successful
            if(data.getInt("cod") != 200){
                return null;
            }

            return data;
        }catch(Exception e){
            Log.i("getJSON", "Exception in getting JSON weather");
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }
}
