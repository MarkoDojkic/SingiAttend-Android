package dev.markodojkic.singiattend;

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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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
                                connection = (HttpURLConnection) (new URL(BuildConfig.SERVER_URL + ":"+ BuildConfig.SERVER_PORT + "/api/getStudentName/"+ gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("Accept", "text/plain;charset=UTF-8");
                                connection.setRequestProperty("Authorization", "Basic "+ new String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.getBytes(StandardCharsets.UTF_8))));
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
            someActivityResultLauncher.launch(new Intent(getApplicationContext(), LoginActivity.class));
        else {
            disabledGrayOut.setVisibility(View.GONE);
            logout.setVisibility(View.VISIBLE);
            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) (new URL(BuildConfig.SERVER_URL + ":"+ BuildConfig.SERVER_PORT + "/api/getStudentName/"+ gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "text/plain;charset=UTF-8");
                    connection.setRequestProperty("Authorization", "Basic "+ new String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.getBytes(StandardCharsets.UTF_8))));
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
                        connection = (HttpURLConnection) (new URL(BuildConfig.SERVER_URL + ":"+ BuildConfig.SERVER_PORT + "/api/getCourseData/"+ gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                        connection.setRequestProperty("Authorization", "Basic "+ new String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.getBytes(StandardCharsets.UTF_8))));
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

                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();

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
                        sCC_btn.setLayoutParams(new LinearLayout.LayoutParams((int)(windowMetrics.getBounds().width()*0.15), (int)(Math.ceil(windowMetrics.getBounds().height()*0.08))));
                        sCC_btn.setX(-25);

                        TextView sC_text = new TextView(MainActivity.this);
                        sC_text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        sC_text.setWidth((int) (windowMetrics.getBounds().width() * 0.64));
                        try {
                            if (Locale.getDefault().getDisplayLanguage().equals("српски") || Locale.getDefault().getDisplayLanguage().equals("srpski"))
                                sC_text.setText(String.format("%s - %s", json.getJSONObject(i).getString("subject").split("-")[0], json.getJSONObject(i).getString("subject").split("-")[1]));
                            else
                                sC_text.setText(String.format("%s - %s", json.getJSONObject(i).getString("subjectEnglish").split("-")[0], json.getJSONObject(i).getString("subjectEnglish").split("-")[1]));

                            DateTimeFormatter fmt = new DateTimeFormatterBuilder().appendPattern("EEE MMM dd kk:mm:ss zzzz yyyy").toFormatter(Locale.ENGLISH);

                            ZonedDateTime beginDate = ZonedDateTime.parse(json.getJSONObject(i).getString("beginTime").replace("CEST","Central European Summer Time" ).replace("CET", "Central European Time" ), fmt);
                            ZonedDateTime endDate = ZonedDateTime.parse(json.getJSONObject(i).getString("endTime").replace("CEST","Central European Summer Time" ).replace("CET", "Central European Time" ), fmt);

                            sC_text.setText(String.format("%s\n%s\n(%s - %s)", sC_text.getText(), json.getJSONObject(i).getString("nameSurname"), beginDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
                            final String sId = json.getJSONObject(i).getString("subjectId");
                            singleClass.setId(json.getJSONObject(i).getString("subjectId").hashCode() * 21682);

                            final String is_vezbe = (sC_text.getText().toString().contains("предавања") || sC_text.getText().toString().contains("lecture")) ? "0": "1";

                            sCC_btn.setOnClickListener(v -> new Thread(() -> {
                                HttpURLConnection buttonConnection = null;

                                try {
                                    buttonConnection = (HttpURLConnection) (new URL(BuildConfig.SERVER_URL + ":"+ BuildConfig.SERVER_PORT + "/api/recordAttendance/"+ gameCache.getString("loggedInUserIndex", "null").replace("/", "") + "/"+ sId + "/"+ is_vezbe.contains("1"))).openConnection();
                                    buttonConnection.setRequestMethod("GET");
                                    buttonConnection.setRequestProperty("Accept", "text/plain;charset=UTF-8");
                                    buttonConnection.setRequestProperty("Authorization", "Basic "+ new String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.getBytes(StandardCharsets.UTF_8))));
                                    buttonConnection.setDoInput(true);
                                    buttonConnection.setConnectTimeout(1000);
                                    buttonConnection.connect();
                                    System.out.println(buttonConnection.getURL());

                                    if (buttonConnection.getResponseCode() == 200) {
                                        BufferedReader buttonInput = new BufferedReader(new InputStreamReader(buttonConnection.getInputStream()));
                                        String response = buttonInput.readLine();
                                        runOnUiThread(() -> {
                                            if (response.equals("ALREADY RECORDED ATTENDANCE")) {
                                                sC_text.setText(String.format("%s\n%s", sC_text.getText(), getResources().getString(R.string.alreadyRecordedAttendance)));
                                            } else if (response.equals("SUCCESSFULLY RECORDED ATTENDANCE")) {
                                                sC_text.setText(String.format("%s\n%s", sC_text.getText(), getResources().getString(R.string.newlyRecordedAttendance)));
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
                        connection = (HttpURLConnection) (new URL(BuildConfig.SERVER_URL + ":"+ BuildConfig.SERVER_PORT + "/api/getAttendanceData/"+ gameCache.getString("loggedInUserIndex", "null").replace("/", ""))).openConnection();
                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                        connection.setRequestProperty("Authorization", "Basic "+ new String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.getBytes(StandardCharsets.UTF_8))));
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
        View singleClassAttendance = inflater.inflate(R.layout.single_lecture_template, linearLayout2, false);
        linearLayout2.removeAllViews();

        AnimatedPieView attendancePie = singleClassAttendance.findViewById(R.id.attendancePie);
        TextView lecture_text = singleClassAttendance.findViewById(R.id.lecture_text);
        TextView info_text = singleClassAttendance.findViewById(R.id.info_text);
        TextView detail_text = singleClassAttendance.findViewById(R.id.detail_text);
        final Button leftArrow_btn = singleClassAttendance.findViewById(R.id.leftArrow_btn);
        final Button rightArrow_btn = singleClassAttendance.findViewById(R.id.rightArrow_btn);

        if(i == 0) leftArrow_btn.setVisibility(View.INVISIBLE);
        if(i == json.length() - 1) rightArrow_btn.setVisibility(View.INVISIBLE);

        if(json.length() == 0) return;

        try {
            double forecast_attendance_points =
                    Math.ceil(10.0 / ( Integer.parseInt(json.getJSONObject(i).getString("totalLectures")) + Integer.parseInt(json.getJSONObject(i).getString("totalPractices")) )
                                            * (Integer.parseInt(json.getJSONObject(i).getString("attendedLectures")) + Integer.parseInt(json.getJSONObject(i).getString("attendedPractices")))); // 10*ukupanBrojLekcija/brojPrisutnosti
            if (Locale.getDefault().getDisplayLanguage().equals("српски") || Locale.getDefault().getDisplayLanguage().equals("srpski")) {
                lecture_text.setText(String.format("%s\n", json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("title")));
                info_text.setText(String.format("Прогноза бодова за присуство: %d/10", Math.round(forecast_attendance_points)));
                if(json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("isInactive").equals("1"))
                    info_text.setText(String.format("%s\n (КРАЈ НАСТАВЕ)", info_text.getText()));
            }
            else {
                lecture_text.setText(String.format("%s\n", json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("titleEnglish")));
                info_text.setText(String.format("Forecast points for attendance: %d/10", Math.round(forecast_attendance_points)));
                if(json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("isInactive").equals("1"))
                    info_text.setText(String.format("%s\n (LECTURES ARE OVER)", info_text.getText()));
            }

            if (json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameA").isEmpty())
                lecture_text.setText(String.format("%s%s", lecture_text.getText(), json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameT")));
            else
                lecture_text.setText(String.format("%s%s\n (%s)", lecture_text.getText(), json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameT"), json.getJSONObject(i).getJSONObject("attendanceSubobjectInstance").getString("nameA")));

            int aL = json.getJSONObject(i).getInt("attendedLectures");
            int tL = json.getJSONObject(i).getInt("totalLectures");
            int aP = json.getJSONObject(i).getInt("attendedPractices");
            int tP = json.getJSONObject(i).getInt("totalPractices");


            attendancePie.applyConfig(new AnimatedPieViewConfig().startAngle(-90)
                    .addData(new SimplePieInfo(aL, Color.GREEN, getString(R.string.descAL)))
                    .addData(new SimplePieInfo(tL-aL, Color.RED, getString(R.string.descTL)))
                    .addData(new SimplePieInfo(aP, Color.CYAN, getString(R.string.descAP)))
                    .addData(new SimplePieInfo(tP-aP, Color.MAGENTA, getString(R.string.descTP)))
                    .selectListener((pieInfo, isFloatUp) -> {
                        double percentage = 0.0;
                        String detail = "";
                        if(isFloatUp){
                            switch (pieInfo.getColor()){
                                case Color.GREEN: percentage = ((double) aL)/tL*100.0; detail="("+ aL + "/"+ tL + ")"; detail_text.setTextColor(Color.GREEN); break;
                                case Color.RED: percentage = ((double) (tL-aL))/tL*100.0; detail="("+ (tL-aL) + "/"+ tL + ")"; detail_text.setTextColor(Color.RED); break;
                                case Color.CYAN: percentage = ((double) aP)/tP*100.0; detail="("+ aP + "/"+ tP + ")"; detail_text.setTextColor(Color.GREEN); break;
                                case Color.MAGENTA: percentage = ((double) (tP-aP))/tP*100.0; detail="("+ (tP-aP) + "/"+ tP + ")"; detail_text.setTextColor(Color.RED); break;
                            }

                            detail_text.setText(String.format("%s%%\n%s", String.format("%.2f", percentage), detail));
                        } else {
                            detail_text.setText("");
                        }
                    })
                    .duration(1000)
                    .drawText(true)
                    .pieRadius(160)
                    .textSize(20)
                    .textMargin(2)
                    .textGravity(AnimatedPieViewConfig.ABOVE)
                    .canTouch(true));

            attendancePie.start();
        } catch (JSONException e) {
            System.out.println("Error occurred while reading attendance data JSON for classID: "+ i);
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
