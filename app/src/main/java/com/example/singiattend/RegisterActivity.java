package com.example.singiattend;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.ArrayRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class RegisterActivity extends AppCompatActivity {

    private boolean isSerbian;
    protected boolean hasError;
    private EditText imePrezime;
    private EditText brIndeksaREG_txt;
    private EditText studentskaEmail;
    private EditText lozinka;
    private Spinner faculties;
    private Spinner courses;
    private Spinner yearIndex;
    private List<String> studyId;
    private HttpURLConnection connection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        imePrezime = findViewById(R.id.nameSurnameREG_txt);
        brIndeksaREG_txt = findViewById(R.id.indexREG_txt);
        studentskaEmail = findViewById(R.id.singiMailREG_txt);
        lozinka = findViewById(R.id.passREG_txt);
        faculties = findViewById(R.id.faculty_spin);
        courses = findViewById(R.id.course_spin);
        yearIndex = findViewById(R.id.indexYear_spin);
        studyId = new ArrayList<>();

        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, IntStream.rangeClosed(2000, 9999).boxed().collect(Collectors.toList()));
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearIndex.setAdapter(yearAdapter);

        brIndeksaREG_txt.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6)});
        brIndeksaREG_txt.setInputType(InputType.TYPE_CLASS_NUMBER);

        studentskaEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(studentskaEmail.getText().toString().matches("^[a-z]+\\.[a-z]+\\.[0-9]{2,3}@$")){
                    studentskaEmail.setText(String.format("%s%s", studentskaEmail.getText().subSequence(0, studentskaEmail.getText().toString().indexOf("@") + 1), "singimail.rs"));
                    studentskaEmail.setSelection(studentskaEmail.getText().toString().indexOf("@"));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        faculties.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                 @ArrayRes int facultyID;

                switch (position){
                    case 0: facultyID = isSerbian ? R.array.courses_f1_srb : R.array.courses_f1_eng; break;
                    case 1: facultyID = isSerbian ? R.array.courses_f2_srb : R.array.courses_f2_eng; break;
                    case 2: facultyID = isSerbian ? R.array.courses_f3_srb : R.array.courses_f3_eng; break;
                    case 3: facultyID = isSerbian ? R.array.courses_f4_srb : R.array.courses_f4_eng; break;
                    case 4: facultyID = isSerbian ? R.array.courses_f5_srb : R.array.courses_f5_eng; break;
                    default: facultyID = R.array.courses_f6_srb;
                }
                updateCourses(facultyID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void updateCourses(@ArrayRes int fID){
        studyId.clear();

        switch (fID){
            case 2130903041: studyId.addAll(List.of("61b612d3e1534b76962f2568", "61b612d3e1534b76962f256d")); break;
            case 2130903043: studyId.addAll(List.of("61b612d3e1534b76962f2572")); break;
            case 2130903045: studyId.addAll(List.of("61b612d3e1534b76962f256c")); break;
            case 2130903047: studyId.addAll(List.of("61b612d3e1534b76962f256f")); break;
            case 2130903049: studyId.addAll(List.of("61b612d3e1534b76962f2570")); break;
            case 2130903042: studyId.addAll(List.of("61b612d3e1534b76962f2564", "61b612d3e1534b76962f256b")); break;
            case 2130903044: studyId.addAll(List.of("61b612d3e1534b76962f2563")); break;
            case 2130903046: studyId.addAll(List.of("61b612d3e1534b76962f256e", "61b612d3e1534b76962f2566")); break;
            case 2130903048: studyId.addAll(List.of("61b612d3e1534b76962f2569")); break;
            case 2130903050: studyId.addAll(List.of("61b612d3e1534b76962f2571", "61b612d3e1534b76962f256a")); break;
            default: studyId.addAll(List.of("61b612d3e1534b76962f2565", "61c7328fe22ce55efb31ac02")); break;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.multiline_simple_spinner, getResources().getStringArray(fID));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courses.setAdapter(adapter);
    }

    public void englishCourses(View v){
        isSerbian = false;
        findViewById(R.id.english_course_btn).setBackgroundResource(R.drawable.border_green);
        findViewById(R.id.serbian_course_btn).setBackgroundColor(Color.WHITE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.multiline_simple_spinner, getResources().getStringArray(R.array.faculties_eng));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        faculties.setAdapter(adapter);
        courses.setAdapter(null);
    }

    public void serbianCourses(View v){
        isSerbian = true;
        findViewById(R.id.serbian_course_btn).setBackgroundResource(R.drawable.border_green);
        findViewById(R.id.english_course_btn).setBackgroundColor(Color.WHITE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.multiline_simple_spinner, getResources().getStringArray(R.array.faculties_srb));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        faculties.setAdapter(adapter);
        courses.setAdapter(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void onRegister(View v) {

        if(imePrezime.getText() == null || imePrezime.getText().length() < 2) {
            imePrezime.setBackgroundColor(Color.RED);
            hasError = true;
            return;
        }
        else {
            imePrezime.setBackgroundColor(Color.GREEN);
            hasError = false;
        }

        if(yearIndex.getSelectedItem() == null) {
            yearIndex.setBackgroundColor(Color.RED);
            hasError = true;
            return;
        }
        else {
            yearIndex.setBackgroundColor(Color.GREEN);
            hasError = false;
        }

        if(brIndeksaREG_txt.getText() == null || brIndeksaREG_txt.getText().length() != 6) {
            brIndeksaREG_txt.setBackgroundColor(Color.RED);
            hasError = true;
            return;
        }
        else {
            brIndeksaREG_txt.setBackgroundColor(Color.GREEN);
            hasError = false;
        }

        if(studentskaEmail.getText() == null || !studentskaEmail.getText().toString().matches("^[a-z]+\\.[a-z]+\\.[0-9]{2,3}@singimail.rs$")) {
            studentskaEmail.setBackgroundColor(Color.RED);
            hasError = true;
            return;
        }
        else {
            studentskaEmail.setBackgroundColor(Color.GREEN);
            hasError = false;
        }

        if(lozinka.getText() == null || lozinka.getText().length() < 8) {
            lozinka.setBackgroundColor(Color.RED);
            hasError = true;
            return;
        }
        else {
            lozinka.setBackgroundColor(Color.GREEN);
            hasError = false;
        }

        if(faculties.getSelectedItem() == null) {
            faculties.setBackgroundColor(Color.RED);
            hasError = true;
            return;
        }
        else {
            faculties.setBackgroundColor(Color.GREEN);
            hasError = false;
        }

        if(courses.getSelectedItem() == null) {
            courses.setBackgroundColor(Color.RED);
            hasError = true;
        }
        else {
            courses.setBackgroundColor(Color.GREEN);
            hasError = false;
        }

        if(!hasError) {
            /*BiometricManager biometricManager = BiometricManager.from(RegisterActivity.this);
            if(biometricManager.canAuthenticate() ==  BiometricManager.BIOMETRIC_SUCCESS){
                Toast.makeText(RegisterActivity.this, "FINGERPRINT SENSOR ACTIVE", Toast.LENGTH_LONG ).show(); //delete this when implemented
                //add fingerprint sensor here https://developer.android.com/training/sign-in/biometric-auth#java
            }
            else Toast.makeText(RegisterActivity.this, R.string.noFingerprintCapability, Toast.LENGTH_LONG ).show(); //delete this when implemented
            */

            Thread thread = new Thread(() -> {
                try {
                    connection = (HttpURLConnection) (new URL("http://192.168.0.196:62812/api/insert/student")).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Accept", "application/json;charset=UTF-8");
                    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(1000);

                    OutputStreamWriter output = new OutputStreamWriter(connection.getOutputStream());

                    JSONObject data = new JSONObject();
                    data.put("name_surname", imePrezime.getText().toString());
                    data.put("index", yearIndex.getSelectedItem().toString() + "/" + brIndeksaREG_txt.getText().toString());
                    data.put("password_hash", lozinka.getText().toString());
                    data.put("email", studentskaEmail.getText().toString());
                    data.put("studyId", studyId.get(courses.getSelectedItemPosition()));
                    data.put("year", String.valueOf(Math.round(Math.ceil(Math.random()*( studyId.get(courses.getSelectedItemPosition()).compareTo("61c7328fe22ce55efb31ac02") == 0 ? 5 : 4 ))))); //Pharmacy is 5 years course

                    connection.connect();
                    output.write(data.toString());
                    output.flush();
                    output.close();

                    if(connection.getResponseCode() == 200){
                        RegisterActivity.this.runOnUiThread(() -> {
                            final SweetAlertDialog successDialog = new SweetAlertDialog(RegisterActivity.this, SweetAlertDialog.SUCCESS_TYPE);
                            successDialog
                                    .setTitleText(R.string.regTitleSuccess)
                                    .setConfirmText(getResources().getString(R.string.regMessageSuccess))
                                    .setConfirmText(getResources().getString(R.string.ok))
                                    .setConfirmClickListener(sweetAlertDialog -> {
                                        successDialog.dismiss();
                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                    })
                                    .show();
                        });
                    }
                    else if(connection.getResponseCode() == 500){
                        RegisterActivity.this.runOnUiThread(() -> {
                            final SweetAlertDialog failedDialog = new SweetAlertDialog(RegisterActivity.this, SweetAlertDialog.ERROR_TYPE);
                            failedDialog
                                    .setTitleText(R.string.regTitleFailed)
                                    .setContentText(getResources().getString(R.string.regMessageFailed))
                                    .setConfirmText(getResources().getString(R.string.ok))
                                    .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                    .show();
                        });
                    }
                    else {
                        RegisterActivity.this.runOnUiThread(() -> {
                            final SweetAlertDialog failedDialog = new SweetAlertDialog(RegisterActivity.this, SweetAlertDialog.WARNING_TYPE);
                            failedDialog
                                    .setTitleText(R.string.regTitleFailed)
                                    .setContentText(getResources().getString(R.string.regMessageServerError))
                                    .setConfirmText(getResources().getString(R.string.ok))
                                    .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                    .show();
                        });
                    }

                } catch (IOException | JSONException e) {
                    if(e.getClass().getName().equals(SocketTimeoutException.class.getName())){
                        RegisterActivity.this.runOnUiThread(() -> {
                            final SweetAlertDialog warningDialog = new SweetAlertDialog(RegisterActivity.this, SweetAlertDialog.WARNING_TYPE);
                            warningDialog
                                    .setTitleText(R.string.regTitleFailed)
                                    .setConfirmText(getResources().getString(R.string.regMessageServerError))
                                    .setConfirmText(getResources().getString(R.string.ok))
                                    .show();
                        });
                    }
                    e.printStackTrace();
                } finally {
                    connection.disconnect();
                }
            });

            thread.start();
        }
    }
}