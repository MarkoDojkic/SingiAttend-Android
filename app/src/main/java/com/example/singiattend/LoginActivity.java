package com.example.singiattend;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class LoginActivity extends AppCompatActivity {
    private EditText brIndeksa_txt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();

        getWindow().setLayout((int)(windowMetrics.getBounds().width()*.95), (int) (windowMetrics.getBounds().height()*.35));
        WindowManager.LayoutParams params = getWindow().getAttributes();

        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        getWindow().setAttributes(params);


        brIndeksa_txt = findViewById(R.id.index_txt);
        brIndeksa_txt.setFilters(new InputFilter[] { new InputFilter.LengthFilter(11)});
        brIndeksa_txt.setInputType(InputType.TYPE_CLASS_NUMBER);
        brIndeksa_txt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                brIndeksa_txt.setOnKeyListener((v, keyCode, event) -> {
                    if(brIndeksa_txt.getText().toString().length() == 5){
                        if(keyCode == KeyEvent.KEYCODE_DEL){
                            brIndeksa_txt.setText(brIndeksa_txt.getText().toString().substring(0,3));
                            brIndeksa_txt.setSelection(3);
                        }
                        else {
                            brIndeksa_txt.setText(String.format("%s/%s", brIndeksa_txt.getText().toString().substring(0, 4), brIndeksa_txt.getText().toString().substring(4, brIndeksa_txt.getText().length())));
                            brIndeksa_txt.setSelection(6);
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
    }

        public void onLogin(View v){
            /*BiometricManager biometricManager = BiometricManager.from(this);
            if(biometricManager.canAuthenticate() ==  BiometricManager.BIOMETRIC_SUCCESS){
                Toast.makeText(this, "FINGERPRINT SENSOR ACTIVE", Toast.LENGTH_LONG ).show();
                //TODO:add fingerprint sensor here https://developer.android.com/training/sign-in/biometric-auth#java
            }
            else Toast.makeText(this, R.string.noFingerprintCapability, Toast.LENGTH_LONG ).show();*/

            Thread thread = new Thread(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL("http://192.168.8.102:62812/api/checkPassword/student/" + brIndeksa_txt.getText().toString().replace("/", "")).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Accept", "text/plain;charset=UTF-8");
                    connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(1000);
                    connection.connect();

                    OutputStreamWriter output = new OutputStreamWriter(connection.getOutputStream());

                    output.write(((EditText) findViewById(R.id.pass_txt)).getText().toString());
                    output.flush();
                    output.close();

                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream())); //Must be after output or after response is present

                    if(connection.getResponseCode() == 200){
                        String response = input.readLine();

                        switch (response) {
                            case "VALID":
                                LoginActivity.this.runOnUiThread(() -> {
                                    final SweetAlertDialog successDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.SUCCESS_TYPE);
                                    successDialog
                                            .setTitleText(R.string.loginTitleSuccess)
                                            .setConfirmText(getResources().getString(R.string.ok))
                                            .setConfirmClickListener(sweetAlertDialog -> {
                                                successDialog.dismiss();
                                                Intent returnIntent = new Intent();
                                                returnIntent.putExtra("indexNo", brIndeksa_txt.getText().toString());
                                                setResult(Activity.RESULT_OK, returnIntent);
                                                finish();
                                            })
                                            .show();
                                });
                                break;
                            case "INVALID":
                                LoginActivity.this.runOnUiThread(() -> {
                                    final SweetAlertDialog failedDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE);
                                    failedDialog
                                            .setTitleText(R.string.loginTitleFailed) //accepts also int (can be used)
                                            .setContentText(getResources().getString(R.string.loginMessageFailed)) //doesn't accept int (use string explicitly)
                                            .setConfirmText(getResources().getString(R.string.ok))
                                            .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                            .show();
                                });
                                break;
                            case "UNKNOWN":
                                LoginActivity.this.runOnUiThread(() -> {
                                    final SweetAlertDialog failedDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE);
                                    failedDialog
                                            .setTitleText(R.string.loginTitleFailed) //accepts also int (can be used)
                                            .setContentText(getResources().getString(R.string.loginMessageUnknown)) //doesn't accept int (use string explicitly)
                                            .setConfirmText(getResources().getString(R.string.ok))
                                            .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                            .show();
                                });
                                break;
                            default:
                                LoginActivity.this.runOnUiThread(() -> {
                                    final SweetAlertDialog failedDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.WARNING_TYPE);
                                    failedDialog
                                            .setTitleText(R.string.loginTitleFailed)
                                            .setContentText(getResources().getString(R.string.regMessageServerError))
                                            .setConfirmText(getResources().getString(R.string.ok))
                                            .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                            .show();
                                });
                                break;
                        }
                    }
                    else if(connection.getResponseCode() == 500){
                        LoginActivity.this.runOnUiThread(() -> {
                            final SweetAlertDialog failedDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE);
                            failedDialog
                                    .setTitleText(R.string.loginTitleFailed)
                                    .setContentText(getResources().getString(R.string.loginMessageFailed))
                                    .setConfirmText(getResources().getString(R.string.ok))
                                    .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                    .show();
                        });
                    }
                    else {
                        LoginActivity.this.runOnUiThread(() -> {
                            final SweetAlertDialog failedDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.WARNING_TYPE);
                            failedDialog
                                    .setTitleText(R.string.loginTitleFailed)
                                    .setContentText(getResources().getString(R.string.regMessageServerError))
                                    .setConfirmText(getResources().getString(R.string.ok))
                                    .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                    .show();
                        });
                    }

                    connection.disconnect();
                } catch (IOException e) {
                    LoginActivity.this.runOnUiThread(() -> {
                        final SweetAlertDialog failedDialog = new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.WARNING_TYPE);
                        failedDialog
                                .setTitleText(R.string.loginTitleFailed)
                                .setContentText(getResources().getString(R.string.regMessageServerError))
                                .setConfirmText(getResources().getString(R.string.ok))
                                .setConfirmClickListener(sweetAlertDialog -> failedDialog.dismiss())
                                .show();
                    });
                    e.printStackTrace();
                }
            });

            thread.start();
        }
        public void onRegistracija(View v){ startActivity(new Intent(LoginActivity.this, RegisterActivity.class));}
}