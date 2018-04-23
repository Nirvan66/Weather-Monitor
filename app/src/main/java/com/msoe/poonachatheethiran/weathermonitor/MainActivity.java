package com.msoe.poonachatheethiran.weathermonitor;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> condition= new ArrayList<String>();
    ArrayList<String> humidity= new ArrayList<String>();
    ArrayList<String> pressure= new ArrayList<String>();
    ArrayList<String> temp= new ArrayList<String>();
    ArrayList<String> wind= new ArrayList<String>();
    ArrayList<String> date= new ArrayList<String>();

    public static final String NotificationBroadcastIntentFilter = "notification.broadcast.intent.filter";
    public static final int NOTIFICATION_ID=1;
    public static final String KEY_REPLY="notification_key";
    public static final String CHANNEL_ID="notification_key";

    /**
     * Broadcast receiver to handle broadcasts sent from DownloadService service
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                condition= bundle.getStringArrayList("Weather Condition");
                humidity= bundle.getStringArrayList("Humidity");
                pressure= bundle.getStringArrayList("Pressure");
                temp= bundle.getStringArrayList("Temperature");
                wind= bundle.getStringArrayList("Wind speed");
                date= bundle.getStringArrayList("Time Updated");

                TextView textCondition = (TextView) findViewById(R.id.textViewCondition);
                TextView textHumidity = (TextView) findViewById(R.id.textViewHumidity);
                TextView textPressure = (TextView) findViewById(R.id.textViewPressure);
                TextView textTemp = (TextView) findViewById(R.id.textViewTemp);
                TextView textWind = (TextView) findViewById(R.id.textViewWind);
                TextView textDate = (TextView) findViewById(R.id.textViewDate);

                textCondition.setText("Weather Condition: "+condition.get(0));
                textHumidity.setText("Humidity: "+humidity.get(0)+" %");
                textPressure.setText("Pressure: "+pressure.get(0)+" hPa");
                textTemp.setText("Temperature: "+temp.get(0)+" ℃");
                textWind.setText("Wind speed: "+wind.get(0)+ " km/h");
                textDate.setText("Time Updated: "+new Date(Long.parseLong(date.get(0))*1000).toString());

                updateGraph();
                notifyThis(((Spinner)findViewById(R.id.spinnerStates)).getSelectedItem().toString()+" "+"Weather Condition", condition.get(0)+ "\n" +
                                                                                                                                    "Humidity: "+humidity.get(0)+" % \n"+
                                                                                                                                    "Pressure: "+pressure.get(0)+" hPa \n" +
                                                                                                                                    "Temperature: "+temp.get(0)+" ℃ \n"+
                                                                                                                                    "Wind speed: "+wind.get(0)+ " km/h \n"+
                                                                                                                                    "Time Updated: "+new Date(Long.parseLong(date.get(0))*1000).toString());
            }
        }
    };

    /**
     * Used to send notification and set up notification text input receiver
     * @param title Heading of the notification
     * @param message Body of the notification
     */
    public void notifyThis(String title, String message) {
        String replyLabel = getString(R.string.sendMail);
        android.support.v4.app.RemoteInput remoteInput = new android.support.v4.app.RemoteInput.Builder(KEY_REPLY)
                .setLabel(replyLabel)
                .build();

        //Intent intent = new Intent(MainActivity.this, NotificationBroadcastReceiver.getClass());
        Intent intent = new Intent(NotificationBroadcastIntentFilter);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(R.drawable.ic_mail, getString(R.string.sendNotification), replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                //.setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .addAction(replyAction)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My_Channel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Weather condition notification channel");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * Broadcast receiver used to listen for messages sent back from notifications.
     */
    private BroadcastReceiver NotificationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle remoteInput = android.support.v4.app.RemoteInput.getResultsFromIntent(intent);
            String id= remoteInput.getString(KEY_REPLY);
            Log.i("Notification Broadcast", id);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_mail)
                    .setContentText("Sent");
            notificationManager.notify(NOTIFICATION_ID, builder.build());

            Intent i = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, ((Spinner)findViewById(R.id.spinnerStates)).getSelectedItem().toString()+" "+"Weather Condition");
            i.putExtra(Intent.EXTRA_TEXT, "Humidity: "+humidity.get(0)+" % \n"+
                                                "Pressure: "+pressure.get(0)+" hPa \n" +
                                                "Temperature: "+temp.get(0)+" ℃ \n"+
                                                "Wind speed: "+wind.get(0)+ " km/h \n"+
                                                "Time Updated: "+new Date(Long.parseLong(date.get(0))*1000).toString());
            i.setData(Uri.parse("mailto:"+id)); // or just "mailto:" for blank
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
            context.startActivity(i);
        }
    };

    /**
     * Uses collected JSON data to plot weather forecast graph.
     */
    public void updateGraph()
    {
        if(date.size()>0) {
            ArrayList<Entry> yHumidity = new ArrayList<Entry>();
            ArrayList<Entry> yPressure = new ArrayList<Entry>();
            ArrayList<Entry> yTemp = new ArrayList<Entry>();
            ArrayList<Entry> yWind = new ArrayList<Entry>();

            for (int i = 0; i < date.size(); i++) {
                //Float tagDate=Float.parseFloat(Long.toString(Long.parseLong(date.get(i))-Long.parseLong(date.get(0))));
                Float tagDate = Float.parseFloat(date.get(i));
                yHumidity.add(new Entry(tagDate, Float.parseFloat(humidity.get(i))));
                yPressure.add(new Entry(tagDate, Float.parseFloat(pressure.get(i))));
                yTemp.add(new Entry(tagDate, Float.parseFloat(temp.get(i))));
                yWind.add(new Entry(tagDate, Float.parseFloat(wind.get(i))));
            }
            ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();

            LineDataSet lineDataSetHumidity = new LineDataSet(yHumidity, "Humidity (%)");
            lineDataSetHumidity.setDrawCircles(false);
            lineDataSetHumidity.setColor(Color.RED);
            lineDataSetHumidity.setDrawValues(false);
            lineDataSetHumidity.setLineWidth(2f);

            LineDataSet lineDataSetPressure = new LineDataSet(yPressure, "Pressure (hPa)");
            lineDataSetPressure.setDrawCircles(false);
            lineDataSetPressure.setColor(Color.RED);
            lineDataSetPressure.setDrawValues(false);
            lineDataSetPressure.setLineWidth(2f);

            LineDataSet lineDataSetTemp = new LineDataSet(yTemp, "Temperature (℃)");
            lineDataSetTemp.setDrawCircles(false);
            lineDataSetTemp.setColor(Color.RED);
            lineDataSetTemp.setDrawValues(false);
            lineDataSetTemp.setLineWidth(2f);

            LineDataSet lineDataSetWind = new LineDataSet(yWind, "Wind Speed (km/h)");
            lineDataSetWind.setDrawCircles(false);
            lineDataSetWind.setColor(Color.RED);
            lineDataSetWind.setDrawValues(false);
            lineDataSetWind.setLineWidth(2f);

            Spinner attributes = (Spinner) findViewById(R.id.spinnerAttributes);
            switch (attributes.getSelectedItemPosition()) {
                case 0:
                    lineDataSets.add(lineDataSetHumidity);
                    break;
                case 1:
                    lineDataSets.add(lineDataSetPressure);
                    break;
                case 2:
                    lineDataSets.add(lineDataSetTemp);
                    break;
                case 3:
                    lineDataSets.add(lineDataSetWind);
                    break;
            }

            LineChart linechart = (LineChart) findViewById(R.id.chartForecast);
            linechart.setData(new LineData(lineDataSets));
            XAxis x = linechart.getXAxis();
            x.setValueFormatter(new HourAxisValueFormatter());

            linechart.getLineData().setValueTextSize(5f);
            linechart.getXAxis().setTextSize(11f);
            linechart.getXAxis().setGridColor(Color.BLACK);

            linechart.getAxisLeft().setTextSize(11f);
            linechart.getAxisLeft().setGridColor(Color.BLACK);

            linechart.getAxisRight().setTextSize(11f);
            linechart.getAxisRight().setGridColor(Color.BLACK);

            linechart.setBorderColor(Color.BLACK);
            linechart.getLegend().setTextSize(15f);

            linechart.invalidate();
        }
    }

    /**
     * Used to format x axis of the graph
     */
    public class HourAxisValueFormatter implements IAxisValueFormatter
    {
        private DateFormat mDataFormat;
        private Date mDate;
        public HourAxisValueFormatter() {
            this.mDataFormat = new SimpleDateFormat("MMM.dd HH:mm", Locale.ENGLISH);
            this.mDate = new Date();
        }
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            long originalTimestamp = (long)value;
            return getHour(originalTimestamp);
        }
        private String getHour(long timestamp){
            try{
                mDate.setTime(timestamp*1000);
                return mDataFormat.format(mDate);
            }
            catch(Exception ex){
                return "xx";
            }
        }
    }

    /**
     * Sets up listeners for various views.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Spinner states= (Spinner) findViewById(R.id.spinnerStates);
        Spinner attributes= (Spinner) findViewById(R.id.spinnerAttributes);

        states.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Intent inten=new Intent (MainActivity.this, DownloadService.class);
                inten.putExtra("city", states.getSelectedItem().toString());
                startService(inten);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        attributes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateGraph();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadService.DownloadServiceIntentFilter));
        registerReceiver(NotificationBroadcastReceiver, new IntentFilter(NotificationBroadcastIntentFilter));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterReceiver(NotificationBroadcastReceiver);
    }
}

