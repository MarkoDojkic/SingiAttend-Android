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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.razerdp.widget.animatedpieview.AnimatedPieView;
import com.razerdp.widget.animatedpieview.AnimatedPieViewConfig;
import com.razerdp.widget.animatedpieview.data.SimplePieInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences gameCache;
    private View disabledGrayOut;
    private String coursesData_json, attendanceData_json;
    private int currentAttendanceClassID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameCache = getApplicationContext().getSharedPreferences("cachedData", MODE_PRIVATE);
        TextView loggedInAs = findViewById(R.id.loggedInAs_text);
        disabledGrayOut = findViewById(R.id.disabledGrayOut);
        Button logout = findViewById(R.id.login_btn);
        logout.setVisibility(View.INVISIBLE);


        if(gameCache.getString("loggedInUserIndex", null) == null)
            startActivityForResult(new Intent(getApplicationContext(), LoginActivity.class), 21682);
        else {
            disabledGrayOut.setVisibility(View.GONE);
            logout.setVisibility(View.VISIBLE);
            communicateWithServer("GETUSERINFO," + gameCache.getString("loggedInUserIndex", "null"), 1);
            loggedInAs.setText(String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null")));
            logout.setBackgroundTintList(null);
            startDataStreaming();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 21682 && resultCode == Activity.RESULT_OK){
            disabledGrayOut.setVisibility(View.GONE);
            gameCache.edit().putString("loggedInUserIndex", Objects.requireNonNull(data).getStringExtra("indexNo")).apply();
            communicateWithServer("GETUSERINFO," + data.getStringExtra("indexNo"), 1);
            finish();
            startActivity(getIntent());
            startDataStreaming();
        }
    }

    private void startDataStreaming() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LinearLayout linearLayout = findViewById(R.id.sv_container);
                communicateWithServer("GETCOURSEDATA," + gameCache.getString("loggedInUserIndex", "null"), 2);
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
                                sC_text.setText(String.format("%s - ", json.getJSONObject(i).getString("predmet")));
                            else
                                sC_text.setText(String.format("%s - ", json.getJSONObject(i).getString("predmet_eng")));

                            sC_text.setText(String.format("%s\n %s \n (%s - %s)", sC_text.getText(), json.getJSONObject(i).getString("nameSurname"), json.getJSONObject(i).getString("begin_time"), json.getJSONObject(i).getString("end_time")));
                            sCC_btn.setId(json.getJSONObject(i).getInt("course_id"));
                            singleClass.setId(1000 + json.getJSONObject(i).getInt("course_id"));

                            final String is_vezbe = (sC_text.getText().toString().contains("предавања") || sC_text.getText().toString().contains("lecture")) ? "0" : "1";

                            sCC_btn.setOnClickListener(view -> communicateWithServer("RECORDATTENDANCE," + gameCache.getString("loggedInUserIndex", "null") + ":" + sCC_btn.getId() + ";" + is_vezbe, 3));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        singleClass.addView(sC_text);
                        singleClass.addView(sCC_btn);

                        linearLayout.addView(singleClass);
                    }
                }

                communicateWithServer("GETATTENDANCESDATA," + gameCache.getString("loggedInUserIndex", "null"), 4);
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

    private void communicateWithServer(final String message, final int type) {
        Thread thread = new Thread(() -> {
            try {
                Socket socket = new Socket("192.168.8.105", 21682);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream());
                output.print(message);
                output.flush();
                switch(type){
                    case 1: gameCache.edit().putString("loggedInUserName", input.readLine()).apply(); break;
                    case 2: coursesData_json = input.readLine(); break;
                    case 3: communicateWithServer("GETCOURSEDATA," + gameCache.getString("loggedInUserIndex", "null"), 2); break;
                    case 4: attendanceData_json = input.readLine(); break;
                }
                output.print("END");
                output.flush();
                input.close();
                output.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        thread.start();
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
                lecture_text.setText(String.format("%s\n", json.getJSONObject(i).getString("predmet")));
                info_text.setText(String.format("Прогноза бодова за присуство: %d/10", Math.round(forecast_attendance_points)));
                if(json.getJSONObject(i).getInt("isCourseEnded")==1)
                    info_text.setText(String.format("%s\n (КРАЈ НАСТАВЕ)", info_text.getText()));
            }
            else {
                lecture_text.setText(String.format("%s\n", json.getJSONObject(i).getString("predmet_eng")));
                info_text.setText(String.format("Forecast points for attendance: %d/10", Math.round(forecast_attendance_points)));
                if(json.getJSONObject(i).getInt("isCourseEnded")==1)
                    info_text.setText(String.format("%s\n (LECTURES ARE OVER)", info_text.getText()));
            }

            if (json.getJSONObject(i).getString("nameSurnameA").equals("null"))
                lecture_text.setText(String.format("%s%s", lecture_text.getText(), json.getJSONObject(i).getString("nameSurnameT")));
            else
                lecture_text.setText(String.format("%s%s\n (%s)", lecture_text.getText(), json.getJSONObject(i).getString("nameSurnameT"), json.getJSONObject(i).getString("nameSurnameA")));

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
