package com.example.androidstudiolearning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.razerdp.widget.animatedpieview.AnimatedPieView;
import com.razerdp.widget.animatedpieview.AnimatedPieViewConfig;
import com.razerdp.widget.animatedpieview.data.SimplePieInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences gameCache;
    private View disabledGrayOut;
    private String coursesData_json, attendanceData_json;
    private TextView serverInactive, loggedInAs;
    private int currentAttendanceClassID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameCache = getApplicationContext().getSharedPreferences("cachedData", MODE_PRIVATE);
        loggedInAs = findViewById(R.id.loggedInAs_text);
        disabledGrayOut = findViewById(R.id.disabledGrayOut);
        Button logout = findViewById(R.id.login_btn);
        logout.setVisibility(View.INVISIBLE);
        serverInactive = findViewById(R.id.serverInactive_text);

        if(gameCache.getString("loggedInUserIndex", null) == null)
            startActivityForResult(new Intent(getApplicationContext(), LoginActivity.class), 21682);
        else {
            disabledGrayOut.setVisibility(View.GONE);
            logout.setVisibility(View.VISIBLE);
            logout.setBackgroundTintList(null);
            try {
                URL url = new URL("http://192.168.8.102:62812/api/getStudentName/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                connection.setDoInput(true);
                connection.setConnectTimeout(1000);

                BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                connection.connect();

                if (connection.getResponseCode() == 200) {
                    String response = input.readLine();
                    gameCache.edit().putString("loggedInUserName", response).apply();
                    serverInactive.setText("");
                    loggedInAs.setText(String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null")));
                    startDataStreaming();
                } else if (connection.getResponseCode() == 500) {
                    gameCache.edit().putString("loggedInUserName", "-SERVER ERROR-").apply();
                    serverInactive.setText("");
                } else {
                    gameCache.edit().putString("loggedInUserName", "").apply();
                    serverInactive.setText(getResources().getText(R.string.serverInactive));
                }

                connection.disconnect();
            } catch (IOException e){
                gameCache.edit().putString("loggedInUserName", "").apply();
                serverInactive.setText(getResources().getText(R.string.serverInactive));
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 21682 && resultCode == Activity.RESULT_OK){
            disabledGrayOut.setVisibility(View.GONE);
            gameCache.edit().putString("loggedInUserIndex", Objects.requireNonNull(data).getStringExtra("indexNo")).apply();
            try {
                URL url = new URL("http://192.168.8.102:62812/api/getStudentName/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                connection.setDoInput(true);
                connection.setConnectTimeout(1000);

                BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                connection.connect();

                if (connection.getResponseCode() == 200) {
                    String response = input.readLine();
                    gameCache.edit().putString("loggedInUserName", response).apply();
                    serverInactive.setText("");
                    loggedInAs.setText(String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null")));
                    startDataStreaming();
                } else if (connection.getResponseCode() == 500) {
                    gameCache.edit().putString("loggedInUserName", "-SERVER ERROR-").apply();
                    serverInactive.setText("");
                } else {
                    gameCache.edit().putString("loggedInUserName", "").apply();
                    serverInactive.setText(getResources().getText(R.string.serverInactive));
                }

                connection.disconnect();
            } catch (IOException e){
                gameCache.edit().putString("loggedInUserName", "").apply();
                serverInactive.setText(getResources().getText(R.string.serverInactive));
                e.printStackTrace();
            }

            finish();
            startActivity(getIntent());
        }
    }

    private void startDataStreaming() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LinearLayout linearLayout = findViewById(R.id.sv_container);
                try {
                    URL url = new URL("http://192.168.8.102:62812/api/getCourseData/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                    connection.setDoInput(true);
                    connection.setConnectTimeout(1000);

                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    connection.connect();

                    if (connection.getResponseCode() == 200) {
                        coursesData_json = input.lines().collect(Collectors.joining());
                        serverInactive.setText("");
                    } else {
                        serverInactive.setText(getResources().getText(R.string.serverInactive));
                    }

                    connection.disconnect();
                } catch (IOException e){
                    serverInactive.setText(getResources().getText(R.string.serverInactive));
                    e.printStackTrace();
                }


                DisplayMetrics displaymetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                if(coursesData_json != null){
                    JSONArray json = null;
                    try {
                        json = new JSONArray(coursesData_json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    linearLayout.removeAllViews();

                    for(int i = 0; i < Objects.requireNonNull(json).length(); i++) {

                        LinearLayout singleClass = new LinearLayout(MainActivity.this);
                        singleClass.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                        singleClass.setOrientation(LinearLayout.HORIZONTAL);

                        final ImageButton sCC_btn = new ImageButton(MainActivity.this);
                        sCC_btn.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.mipmap.success_foreground));
                        sCC_btn.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        sCC_btn.setLayoutParams(new LinearLayout.LayoutParams((int)(displaymetrics.widthPixels*0.09615385), (int)(Math.ceil(displaymetrics.heightPixels*0.05311077))));
                        sCC_btn.setX(-35);

                        TextView sC_text = new TextView(MainActivity.this);
                        sC_text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        sC_text.setWidth((int) (displaymetrics.widthPixels * 0.64));
                        try {
                            if (Locale.getDefault().getDisplayLanguage().equals("српски") || Locale.getDefault().getDisplayLanguage().equals("srpski"))
                                sC_text.setText(String.format("%s - ", json.getJSONObject(i).getString("subject")));
                            else
                                sC_text.setText(String.format("%s - ", json.getJSONObject(i).getString("subjectEnglish")));

                            sC_text.setText(String.format("%s\n %s \n (%s - %s)", sC_text.getText(), json.getJSONObject(i).getString("nameSurname"), json.getJSONObject(i).getString("beginTime"), json.getJSONObject(i).getString("endTime")));
                            sCC_btn.setId(json.getJSONObject(i).getInt("subjectId"));
                            singleClass.setId(1000 + json.getJSONObject(i).getInt("subjectId"));

                            final String is_vezbe = (sC_text.getText().toString().contains("предавања") || sC_text.getText().toString().contains("lecture")) ? "0" : "1";

                            sCC_btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        URL url = new URL("http://192.168.8.102:62812/api/recordAttendance/" + gameCache.getString("loggedInUserIndex", "null").replace("/", "") + "/" + sCC_btn.getId() + "/" + is_vezbe.contains("1"));
                                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                        connection.setRequestMethod("GET");
                                        connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                                        connection.setDoInput(true);
                                        connection.setConnectTimeout(1000);

                                        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                                        connection.connect();

                                        if (connection.getResponseCode() == 200) {
                                            String response = input.readLine();
                                            if(response.split("\\*")[0] == "0"){
                                                sC_text.setText(sC_text.getText() + "\n" + getResources().getString(R.string.alreadyRecordedAttendance) + response.split("\\*")[1]);
                                            }
                                            else if(response.split("\\*")[0] == "1"){
                                                sC_text.setText(sC_text.getText() + "\n" + getResources().getString(R.string.newlyRecordedAttendance) + response.split("\\*")[1]);
                                            }
                                            sCC_btn.setVisibility(View.INVISIBLE);
                                            serverInactive.setText("");
                                        } else {
                                            final SweetAlertDialog recordingAttendanceFailed = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE);
                                            recordingAttendanceFailed
                                                    .setTitleText(R.string.recordAttendanceFailed)
                                                    .setContentText(getResources().getString(R.string.recordAttendanceServerError))
                                                    .setConfirmText(getResources().getString(R.string.ok))
                                                    .setConfirmClickListener(sweetAlertDialog -> recordingAttendanceFailed.dismiss())
                                                    .show();
                                        }

                                        connection.disconnect();
                                    } catch (IOException e){
                                        serverInactive.setText(getResources().getText(R.string.serverInactive));
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            final SweetAlertDialog recordingAttendanceFailed = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE);
                            recordingAttendanceFailed
                                    .setTitleText(R.string.recordAttendanceFailed)
                                    .setContentText(getResources().getString(R.string.recordAttendanceClientError))
                                    .setConfirmText(getResources().getString(R.string.ok))
                                    .setConfirmClickListener(sweetAlertDialog -> recordingAttendanceFailed.dismiss())
                                    .show();
                            e.printStackTrace();
                        }
                        singleClass.addView(sC_text);
                        singleClass.addView(sCC_btn);

                        linearLayout.addView(singleClass);
                    }
                }

                try {
                    URL url = new URL("http://192.168.8.102:62812/api/getAttendanceData/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                    connection.setDoInput(true);
                    connection.setConnectTimeout(1000);

                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    connection.connect();

                    if (connection.getResponseCode() == 200) {
                        attendanceData_json = input.lines().collect(Collectors.joining());
                        serverInactive.setText("");
                    } else {
                        serverInactive.setText(getResources().getText(R.string.serverInactive));
                    }

                    connection.disconnect();
                } catch (IOException e){
                    serverInactive.setText(getResources().getText(R.string.serverInactive));
                    e.printStackTrace();
                }

                if(attendanceData_json != null){
                    JSONArray json;
                    try {
                        json = new JSONArray(attendanceData_json);
                        getAttendanceByCourseId(json,currentAttendanceClassID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                handler.postDelayed(this, 6000);
            }
        }, 6000);
    }

    public void onLogout(View v){
        final SweetAlertDialog confirmLogoutDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE);
        confirmLogoutDialog.setTitleText(R.string.confirmLogout);
        confirmLogoutDialog.setContentText(getResources().getString(R.string.wannaLogout));
        confirmLogoutDialog.setConfirmText(getResources().getString(R.string.yes));
        confirmLogoutDialog.setCancelText(getResources().getString(R.string.no));
        confirmLogoutDialog.setConfirmClickListener(sweetAlertDialog -> {
            gameCache.edit().clear().apply();
            confirmLogoutDialog.dismiss();
            finish();
            startActivity(getIntent());
        });
        confirmLogoutDialog.show();
    }

    @SuppressLint("DefaultLocale")
    private void getAttendanceByCourseId(final JSONArray json, int i){

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        LinearLayout linearLayout2 = findViewById(R.id.hv_container);
        View singleClassAttendance = inflater.inflate(R.layout.singlelecturetemplate, linearLayout2, false);
        linearLayout2.removeAllViews();

        AnimatedPieView attendancePie = singleClassAttendance.findViewById(R.id.attendancePie);
        TextView lecture_text = singleClassAttendance.findViewById(R.id.lecture_text);
        TextView info_text = singleClassAttendance.findViewById(R.id.info_text);
        TextView pB_text = singleClassAttendance.findViewById(R.id.pB_text);
        final Button leftArrow_btn = singleClassAttendance.findViewById(R.id.leftArrow_btn);
        final Button rightArrow_btn = singleClassAttendance.findViewById(R.id.rightArrow_btn);

        if(i == 0) leftArrow_btn.setVisibility(View.INVISIBLE);
        if(i == json.length()) rightArrow_btn.setVisibility(View.INVISIBLE);

        try {
            //System.out.println(json.getJSONObject(i));
            double forecast_attendance_points =
                    Math.ceil(10.0 / ( Integer.parseInt(json.getJSONObject(i).getString("totalLectures")) + Integer.parseInt(json.getJSONObject(i).getString("totalPractices")) )
                                            * (Integer.parseInt(json.getJSONObject(i).getString("attendedLectures")) + Integer.parseInt(json.getJSONObject(i).getString("attendedPractices")))); // 10*ukupanBrojLekcija/brojPrisutnosti
            if (Locale.getDefault().getDisplayLanguage().equals("српски") || Locale.getDefault().getDisplayLanguage().equals("srpski")) {
                lecture_text.setText(String.format("%s\n", json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("title")));
                info_text.setText(String.format("Прогноза бодова за присуство: %d/10", Math.round(forecast_attendance_points)));
                if(json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getInt("isInactive")==1)
                    info_text.setText(String.format("%s\n (КРАЈ НАСТАВЕ)", info_text.getText()));
            }
            else {
                lecture_text.setText(String.format("%s\n", json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("titleEnglish")));
                info_text.setText(String.format("Forecast points for attendance: %d/10", Math.round(forecast_attendance_points)));
                if(json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getInt("isInactive")==1)
                    info_text.setText(String.format("%s\n (LECTURES ARE OVER)", info_text.getText()));
            }

            if (json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameA").equals("null"))
                lecture_text.setText(String.format("%s%s", lecture_text.getText(), json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameT")));
            else
                lecture_text.setText(String.format("%s%s\n (%s)", lecture_text.getText(), json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameT"), json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameA")));

            attendancePie.applyConfig(new AnimatedPieViewConfig().startAngle(-90)
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("attendedLectures"), Color.parseColor("#FF0000")))
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("totalLectures")-json.getJSONObject(i).getInt("attendedLectures"), Color.parseColor("#FF8E8E")))
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("attendedPractices"), Color.parseColor("#33FF00")))
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("totalPractices")-json.getJSONObject(i).getInt("attendedPractices"), Color.parseColor("#A5FF8E")))
                    .duration(0)
                    .canTouch(false));
            attendancePie.start();
            double percentage = (double) (Integer.parseInt(json.getJSONObject(i).getString("attendedLectures"))+Integer.parseInt(json.getJSONObject(i).getString("attendedPractices"))) / (double) (Integer.parseInt(json.getJSONObject(i).getString("totalLectures"))+Integer.parseInt(json.getJSONObject(i).getString("totalPractices")))*100;
            pB_text.setText(String.format("%s%%\n(%d/%d)", String.format("%.2f", percentage), (json.getJSONObject(i).getInt("attendedLectures")+json.getJSONObject(i).getInt("attendedPractices")), (json.getJSONObject(i).getInt("totalLectures")+json.getJSONObject(i).getInt("totalPractices"))));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        leftArrow_btn.setOnClickListener(view -> {
            currentAttendanceClassID--;
            rightArrow_btn.setVisibility(View.VISIBLE);
            getAttendanceByCourseId(json,currentAttendanceClassID);
        });

        rightArrow_btn.setOnClickListener(view -> {
            currentAttendanceClassID++;
            leftArrow_btn.setVisibility(View.VISIBLE);
            getAttendanceByCourseId(json,currentAttendanceClassID);
        });

        linearLayout2.addView(singleClassAttendance);
    }
}
