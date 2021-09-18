package com.example.androidstudiolearning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.razerdp.widget.animatedpieview.AnimatedPieView;
import com.razerdp.widget.animatedpieview.AnimatedPieViewConfig;
import com.razerdp.widget.animatedpieview.data.SimplePieInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences gameCache;
    private View disabledGrayOut;
    private String coursesData_json, attendanceData_json;
    private TextView serverInactive, loggedInAs;
    private int currentAttendanceClassID = 0;

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        disabledGrayOut.setVisibility(View.GONE);
                        gameCache.edit().putString("loggedInUserIndex", Objects.requireNonNull(result.getData()).getStringExtra("indexNo")).apply();
                        new Thread(() -> {
                            HttpURLConnection connection = null;
                            try {
                                connection = (HttpURLConnection) (new URL("http://192.168.8.102:62812/api/getStudentName/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("Accept", "text/plain;charset=UTF-8");
                                connection.setDoInput(true);
                                connection.setConnectTimeout(1000);

                                connection.connect();
                                if (connection.getResponseCode() == 200) {
                                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                    gameCache.edit().putString("loggedInUserName", input.readLine()).apply();
                                    serverInactive.setText("");
                                    input.close();
                                    runOnUiThread(MainActivity.this::startDataStreaming);
                                } else if (connection.getResponseCode() == 500) {
                                    gameCache.edit().putString("loggedInUserName", "-SERVER ERROR-").apply();
                                    serverInactive.setText("");
                                } else {
                                    gameCache.edit().putString("loggedInUserName", "").apply();
                                    serverInactive.setText(getResources().getText(R.string.serverInactive));
                                }
                            } catch (Exception e) {
                                gameCache.edit().putString("loggedInUserName", "").apply();
                                serverInactive.setText(getResources().getText(R.string.serverInactive));
                                e.printStackTrace();
                            } finally {
                                if(connection != null) connection.disconnect();
                                loggedInAs.setText(String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null")));
                                finish();
                                startActivity(getIntent());
                            }
                        }).start();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameCache = getApplicationContext().getSharedPreferences("cachedData", MODE_PRIVATE);
        loggedInAs = findViewById(R.id.loggedInAs_text);
        disabledGrayOut = findViewById(R.id.disabledGrayOut);
        Button logout = findViewById(R.id.logout_btn);
        logout.setVisibility(View.INVISIBLE);
        serverInactive = findViewById(R.id.serverInactive_text);

        if(gameCache.getString("loggedInUserIndex", null) == null)
            someActivityResultLauncher.launch(new Intent(getApplicationContext(), LoginActivity.class)); //Deprecated, needs refactoring to "registerForActivityResult"
        else {
            disabledGrayOut.setVisibility(View.GONE);
            logout.setVisibility(View.VISIBLE);
            logout.setBackgroundTintList(null);
            new Thread(() -> {
                HttpURLConnection connection = null;

                try {
                    connection = (HttpURLConnection) (new URL("http://192.168.8.102:62812/api/getStudentName/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "text/plain;charset=UTF-8");
                    connection.setDoInput(true);
                    connection.setConnectTimeout(1000);

                    if (connection.getResponseCode() == 200) {
                        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        gameCache.edit().putString("loggedInUserName", input.readLine()).apply();
                        serverInactive.setText("");
                        input.close();
                        startDataStreaming();
                    } else if (connection.getResponseCode() == 500) {
                        gameCache.edit().putString("loggedInUserName", "-SERVER ERROR-").apply();
                        serverInactive.setText("");
                        loggedInAs.setText(String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null")));

                    } else {
                        gameCache.edit().putString("loggedInUserName", "").apply();
                        serverInactive.setText(getResources().getText(R.string.serverInactive));
                        loggedInAs.setText(String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null")));
                    }
                } catch (Exception e) {
                    gameCache.edit().putString("loggedInUserName", "").apply();
                    serverInactive.setText(getResources().getText(R.string.serverInactive));
                    e.printStackTrace();
                } finally {
                    if(connection != null) connection.disconnect();
                    loggedInAs.setText(String.format("%s (%s)", gameCache.getString("loggedInUserName", "null"), gameCache.getString("loggedInUserIndex", "null")));
                }
            }).start();
        }
    }

    private void startDataStreaming() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LinearLayout linearLayout = findViewById(R.id.sv_container);
                new Thread(() -> {
                    HttpURLConnection connection = null;

                    try {
                        connection = (HttpURLConnection) (new URL("http://192.168.8.102:62812/api/getCourseData/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                        connection.setDoInput(true);
                        connection.setConnectTimeout(1000);
                        connection.connect();

                        if (connection.getResponseCode() == 200) {
                            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            coursesData_json = input.lines().collect(Collectors.joining());
                            input.close();
                            serverInactive.setText("");
                        } else {
                            serverInactive.setText(getResources().getText(R.string.serverInactive));
                        }
                    } catch (Exception e) {
                        serverInactive.setText(getResources().getText(R.string.serverInactive));
                        e.printStackTrace();
                    } finally {
                        if(connection != null) connection.disconnect();
                    }
                }).start();

                //DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();

                //getWindowManager().getDefaultDisplay().getDisplay().getRealMetrics(displayMetrics);
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
                        sCC_btn.setLayoutParams(new LinearLayout.LayoutParams((int)(windowMetrics.getBounds().width()*0.09*2), (int)(Math.ceil(windowMetrics.getBounds().height()*0.09))));
                        sCC_btn.setX(-25);

                        TextView sC_text = new TextView(MainActivity.this);
                        sC_text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        sC_text.setWidth((int) (windowMetrics.getBounds().width() * 0.64));
                        try {
                            if (Locale.getDefault().getDisplayLanguage().equals("српски") || Locale.getDefault().getDisplayLanguage().equals("srpski"))
                                sC_text.setText(String.format("%s - %s", json.getJSONObject(i).getString("subject").split("-")[0], json.getJSONObject(i).getString("subject").split("-")[1]));
                            else
                                sC_text.setText(String.format("%s - $s", json.getJSONObject(i).getString("subjectEnglish").split("-")[0], json.getJSONObject(i).getString("subjectEnglish").split("-")[1]));

                            sC_text.setText(String.format("%s\n%s\n(%s - %s)", sC_text.getText(), json.getJSONObject(i).getString("nameSurname"), json.getJSONObject(i).getString("beginTime"), json.getJSONObject(i).getString("endTime")));
                            sCC_btn.setId(json.getJSONObject(i).getInt("subjectId"));
                            singleClass.setId(1000 + json.getJSONObject(i).getInt("subjectId"));

                            final String is_vezbe = (sC_text.getText().toString().contains("предавања") || sC_text.getText().toString().contains("lecture")) ? "0" : "1";

                            sCC_btn.setOnClickListener(v -> new Thread(() -> {
                                HttpURLConnection buttonConnection = null;

                                try {
                                    buttonConnection = (HttpURLConnection) (new URL("http://192.168.8.102:62812/api/recordAttendance/" + gameCache.getString("loggedInUserIndex", "null").replace("/", "") + "/" + sCC_btn.getId() + "/" + is_vezbe.contains("1"))).openConnection();
                                    buttonConnection.setRequestMethod("GET");
                                    buttonConnection.setRequestProperty("Accept", "text/plain;charset=UTF-8");
                                    buttonConnection.setDoInput(true);
                                    buttonConnection.setConnectTimeout(1000);
                                    buttonConnection.connect();

                                    if (buttonConnection.getResponseCode() == 200) {
                                        BufferedReader buttonInput = new BufferedReader(new InputStreamReader(buttonConnection.getInputStream()));
                                        String response = buttonInput.readLine();
                                        runOnUiThread(() -> {
                                            if (response.split("\\*")[0].equals("0")) {
                                                sC_text.setText(String.format("%s\n%s %s", sC_text.getText(), getResources().getString(R.string.alreadyRecordedAttendance), response.split("\\*")[1]));
                                            } else if (response.split("\\*")[0].equals("1")) {
                                                sC_text.setText(String.format("%s\n%s %s", sC_text.getText(), getResources().getString(R.string.newlyRecordedAttendance), response.split("\\*")[1]));
                                            }
                                            sCC_btn.setVisibility(View.INVISIBLE);
                                            serverInactive.setText("");
                                        });
                                    } else {
                                        runOnUiThread(() -> {
                                            final SweetAlertDialog recordingAttendanceFailed = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE);
                                            recordingAttendanceFailed
                                                    .setTitleText(R.string.recordAttendanceFailed)
                                                    .setContentText(getResources().getString(R.string.recordAttendanceServerError))
                                                    .setConfirmText(getResources().getString(R.string.ok))
                                                    .setConfirmClickListener(sweetAlertDialog -> recordingAttendanceFailed.dismiss())
                                                    .show();
                                        });
                                    }
                                } catch (Exception e) {
                                    serverInactive.setText(getResources().getText(R.string.serverInactive));
                                    runOnUiThread(() -> {
                                        final SweetAlertDialog recordingAttendanceFailed = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE);
                                        recordingAttendanceFailed
                                                .setTitleText(R.string.recordAttendanceFailed)
                                                .setContentText(getResources().getString(R.string.recordAttendanceClientError))
                                                .setConfirmText(getResources().getString(R.string.ok))
                                                .setConfirmClickListener(sweetAlertDialog -> recordingAttendanceFailed.dismiss())
                                                .show();
                                    });
                                    e.printStackTrace();
                                } finally {
                                    if(buttonConnection != null) buttonConnection.disconnect();
                                }
                            }).start());

                            singleClass.addView(sC_text);
                            singleClass.addView(sCC_btn);

                            linearLayout.addView(singleClass);
                        } catch (Exception e) {
                            System.out.println("Error occurred while reading courses data JSON.");
                            e.printStackTrace();
                        }
                    }
                }

                new Thread(() -> {
                    HttpURLConnection connection = null;

                    try {
                        connection = (HttpURLConnection) (new URL("http://192.168.8.102:62812/api/getAttendanceData/" + gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                        connection.setDoInput(true);
                        connection.setConnectTimeout(1000);
                        connection.connect();

                        if (connection.getResponseCode() == 200) {
                            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            attendanceData_json = input.lines().collect(Collectors.joining());
                            input.close();
                            serverInactive.setText("");
                        } else {
                            serverInactive.setText(getResources().getText(R.string.serverInactive));
                        }
                    } catch (Exception e) {
                        serverInactive.setText(getResources().getText(R.string.serverInactive));
                        e.printStackTrace();
                    } finally {
                        if(connection != null) connection.disconnect();
                    }
                }).start();

                if(attendanceData_json != null){
                    JSONArray json;
                    try {
                        json = new JSONArray(attendanceData_json);
                        getAttendanceByCourseId(json,currentAttendanceClassID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                handler.postDelayed(this, 10000);
            }
        }, 1000);
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

        if(json.length() == 0) return;

        try {
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
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("attendedLectures"), Color.GREEN))
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("totalLectures")-json.getJSONObject(i).getInt("attendedLectures"), Color.RED))
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("attendedPractices"), Color.CYAN))
                    .addData(new SimplePieInfo(json.getJSONObject(i).getInt("totalPractices")-json.getJSONObject(i).getInt("attendedPractices"), Color.MAGENTA))
                    .duration(0)
                    .canTouch(false));
            attendancePie.start();
            double percentage = (double) (Integer.parseInt(json.getJSONObject(i).getString("attendedLectures"))+Integer.parseInt(json.getJSONObject(i).getString("attendedPractices"))) / (double) (Integer.parseInt(json.getJSONObject(i).getString("totalLectures"))+Integer.parseInt(json.getJSONObject(i).getString("totalPractices")))*100;
            pB_text.setText(String.format("%s%%\n(%d/%d)", String.format("%.2f", percentage), (json.getJSONObject(i).getInt("attendedLectures")+json.getJSONObject(i).getInt("attendedPractices")), (json.getJSONObject(i).getInt("totalLectures")+json.getJSONObject(i).getInt("totalPractices"))));
            System.out.println(pB_text.getText().toString());
        } catch (JSONException e) {
            System.out.println("Error occurred while reading attendance data JSON for classID: " + i);
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
