package com.example.androidstudiolearning;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
//import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import androidx.annotation.ArrayRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.biometric.BiometricManager;
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
        brIndeksaREG_txt.setFilters(new InputFilter[] { new InputFilter.LengthFilter(11)});
        brIndeksaREG_txt.setInputType(InputType.TYPE_CLASS_NUMBER);
        brIndeksaREG_txt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                brIndeksaREG_txt.setOnKeyListener((v, keyCode, event) -> {
                    if(brIndeksaREG_txt.getText().toString().length() == 5){
                        if(keyCode == KeyEvent.KEYCODE_DEL){
                            brIndeksaREG_txt.setText(brIndeksaREG_txt.getText().toString().substring(0,3));
                            brIndeksaREG_txt.setSelection(3);
                        }
                        else {
                            brIndeksaREG_txt.setText(String.format("%s/%s", brIndeksaREG_txt.getText().toString().substring(0, 4), brIndeksaREG_txt.getText().toString().substring(4, brIndeksaREG_txt.getText().length())));
                            brIndeksaREG_txt.setSelection(6);
                        }
                    }
                    return false;
                });
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        studentskaEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                studentskaEmail.setOnKeyListener((v, keyCode, event) -> {
                    if(studentskaEmail.getText().toString().matches("^[a-z]+\\.[a-z]+\\.[0-9]{2,3}@(singimail.rs)?$")){
                        if(keyCode == KeyEvent.KEYCODE_DEL){
                            studentskaEmail.setText(studentskaEmail.getText().toString().substring(0,studentskaEmail.getText().toString().indexOf("@")));
                            studentskaEmail.setSelection(studentskaEmail.getText().length());
                        }
                        else {
                            studentskaEmail.setText(String.format("%s%s", studentskaEmail.getText().toString(), "singimail.rs"));
                            studentskaEmail.setSelection(studentskaEmail.getText().length());
                            studentskaEmail.setFilters(new InputFilter[] {new InputFilter.LengthFilter(studentskaEmail.length())});
                        }
                    }
                    return false;
                });
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        faculties.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                 @ArrayRes int coursesID;

                switch (position){
                    case 0: coursesID = isSerbian ? R.array.courses_f1_srb : R.array.courses_f1_eng; break;
                    case 1: coursesID = isSerbian ? R.array.courses_f2_srb : R.array.courses_f2_eng; break;
                    case 2: coursesID = isSerbian ? R.array.courses_f3_srb : R.array.courses_f3_eng; break;
                    case 3: coursesID = isSerbian ? R.array.courses_f4_srb : R.array.courses_f4_eng; break;
                    case 4: coursesID = isSerbian ? R.array.courses_f5_srb : R.array.courses_f5_eng; break;
                    default: coursesID = R.array.courses_f6_srb;
                }
                updateCourses(coursesID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void updateCourses(@ArrayRes int cID){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.multiline_simple_spinner, getResources().getStringArray(cID));

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

        if(brIndeksaREG_txt.getText() == null || brIndeksaREG_txt.getText().length() != 11) {
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
            final SweetAlertDialog waitingDialog = new SweetAlertDialog(RegisterActivity.this, SweetAlertDialog.PROGRESS_TYPE);
            waitingDialog.getProgressHelper().setBarColor(Color.YELLOW);
            waitingDialog.setTitleText(R.string.regWaiting);
            waitingDialog.setCancelable(false);
            waitingDialog.show();

            Thread thread = new Thread(() -> {
                try {
                    Socket socket = new Socket("192.168.8.105", 21682);
                    String response;
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter output = new PrintWriter(socket.getOutputStream());

                    /*
                    byte[] decodedKey = Base64.decode("8p6pyFULk8j3DV/yHJaGzw==", Base64.NO_WRAP); //8p6pyFULk8j3DV/yHJaGzw== - is Base64 encoded key (Base64.NO_WRAP)
                    SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                    Cipher cipher = Cipher.getInstance("AES"); // cipher is not thread safe

                    //ENCODE PASSWORD
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                    String encryptedPwd = Base64.encodeToString(cipher.doFinal(lozinka.getText().toString().getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);

                    // DECODE encryptedPwd String
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    String decryptedPwd = new String(cipher.doFinal(Base64.decode(encryptedPwd, Base64.NO_WRAP)), "UTF-8");
                    EXAMPLE: jQcRAV7wi/a4JWZekAwP3w== = Mar****99 - EXAMPLE OF ENCRYPTION USING KEY ABOVE
                    */

                    output.print(imePrezime.getText().toString() + "*" + brIndeksaREG_txt.getText().toString() + "*" + studentskaEmail.getText().toString() + "*" + lozinka.getText().toString() + "*" + faculties.getSelectedItem().toString() + "*" + courses.getSelectedItem().toString());
                    output.flush(); // * is delimiter
                    //SOCKET ACCESS DENIED - SOLUTION : PERMISSION FROM INTERNET TO MANIFEST

                    socket.setSoTimeout(0); //wait for approval
                    response = input.readLine();
                    if(response.contains("APPROVED")){
                        waitingDialog.dismiss();
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
                    else if(response.contains("DENIED")){
                        waitingDialog.dismiss();
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
                    output.write("END"); //END CONNECTION ON SERVER SIDE
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
    }
}