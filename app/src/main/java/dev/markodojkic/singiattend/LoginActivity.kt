package dev.markodojkic.singiattend

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64

class LoginActivity : AppCompatActivity() {
    private lateinit var indexTxt: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val windowMetrics = windowManager.currentWindowMetrics
        window.setLayout((windowMetrics.bounds.width() * .95).toInt(), (windowMetrics.bounds.height() * .55).toInt())
        val params = window.attributes
        params.gravity = Gravity.CENTER
        params.x = 0
        params.y = 0
        window.attributes = params
        indexTxt = findViewById(R.id.index_txt)
        indexTxt.filters = arrayOf<InputFilter>(LengthFilter(11))
        indexTxt.inputType = InputType.TYPE_CLASS_NUMBER
        indexTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                indexTxt.setOnKeyListener { _: View?, keyCode: Int, _: KeyEvent? ->
                    if (indexTxt.text.toString().length == 5) {
                        if (keyCode == KeyEvent.KEYCODE_DEL) {
                            indexTxt.setText(indexTxt.text.toString().substring(0, 3))
                            indexTxt.setSelection(3)
                        } else {
                            indexTxt.setText(String.format("%s/%s", indexTxt.text.toString().substring(0, 4), indexTxt.text.toString().substring(4, indexTxt.text.length)))
                            indexTxt.setSelection(6)
                        }
                    }
                    false
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
    }

    fun onLogin(v: View) {
        login(indexTxt.text.toString(), (findViewById<View>(R.id.pass_txt) as EditText).text.toString())
    }

    fun onLoginWithBiometrics(v: View) {
        if(MainActivity.sharedPreferences.getString("biometricsStudentIndex", null) == null){
            runOnUiThread {
                val failedDialog =
                    SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                failedDialog
                    .setTitleText(R.string.loginTitleFailed)
                    .setContentText(resources.getString(R.string.biometricsNoAccountMessage))
                    .setConfirmText(resources.getString(R.string.ok))
                    .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                    .show()
            }

            return
        }

        BiometricPrompt(this, ContextCompat.getMainExecutor(applicationContext), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                login(MainActivity.sharedPreferences.getString("biometricsStudentIndex", null).toString(), MainActivity.sharedPreferences.getString("biometricsStudentPassword", null).toString())
            }

            override fun onAuthenticationFailed() {
                runOnUiThread {
                    val failedDialog =
                        SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                    failedDialog
                        .setTitleText(R.string.loginTitleFailed)
                        .setContentText(resources.getString(R.string.biometricsAuthenticationFailed))
                        .setConfirmText(resources.getString(R.string.ok))
                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                        .show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                lateinit var message: String

                when(errorCode){
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> message = resources.getString(R.string.biometricsNotAvailable)
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> message = resources.getString(R.string.biometricsNotAvailable)
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> message = resources.getString(R.string.biometricsNotEnrolled)
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> message = resources.getString(R.string.biometricsNotEnrolled)
                    BiometricPrompt.ERROR_LOCKOUT -> message = resources.getString(R.string.biometricsLockout)
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> message = resources.getString(R.string.biometricsLockout)
                    else -> message = resources.getString(R.string.biometricsGenericError)
                }

                runOnUiThread {
                    val failedDialog =
                        SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                    failedDialog
                        .setTitleText(R.string.loginTitleFailed)
                        .setContentText(message)
                        .setConfirmText(resources.getString(R.string.ok))
                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                        .show()
                }
            }
        }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(resources.getString(R.string.loginUsingBiometricsAuthentication))
            .setSubtitle(String.format(resources.getString(R.string.biometricsLoginReason), MainActivity.sharedPreferences.getString("biometricsStudentIndex", null).toString()))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build())
    }

    fun onRegistracija(v: View) {
        startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
    }

    private fun login(index: String, password: String) {
        val thread = Thread {
            try {
                val connection = URL(String.format("%s/api/checkPassword/student/%s", BuildConfig.SERVER_URL, index.replace("/", ""))).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Accept", "text/plain;charset=UTF-8")
                connection.setRequestProperty("Content-Type", "text/plain;charset=UTF-8")
                connection.setRequestProperty("Authorization", String.format("Basic %s", String(Base64.getEncoder().encode(BuildConfig.SERVER_CREDENTIALS.toByteArray(StandardCharsets.UTF_8)))))
                connection.doInput = true
                connection.doOutput = true
                connection.connectTimeout = 1000
                connection.connect()
                val output = OutputStreamWriter(connection.outputStream)
                output.write(password)
                output.flush()
                output.close()
                val input = BufferedReader(InputStreamReader(connection.inputStream)) //Must be after output or after response is present
                when (connection.responseCode) {
                    200 -> {
                        when (input.readLine()) {
                            "VALID" -> runOnUiThread {
                                val successDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.SUCCESS_TYPE)
                                successDialog
                                        .setTitleText(R.string.loginTitleSuccess)
                                        .setConfirmText(resources.getString(R.string.ok))
                                        .setConfirmClickListener { _: SweetAlertDialog? ->
                                            successDialog.dismiss()
                                            val returnIntent = Intent()
                                            if (findViewById<View>(R.id.save_for_biometrics_switch).isActivated) {
                                                MainActivity.sharedPreferences.edit().putString("biometricsStudentIndex", index).apply()
                                                MainActivity.sharedPreferences.edit().putString("biometricsStudentPassword", password).apply()
                                            }
                                            MainActivity.sharedPreferences.edit().putString("loggedInUserIndex", index).apply()
                                            setResult(RESULT_OK, returnIntent)
                                            finish()
                                        }
                                        .show()
                            }

                            "INVALID" -> runOnUiThread {
                                val failedDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                                failedDialog
                                        .setTitleText(R.string.loginTitleFailed) //accepts also int (can be used)
                                        .setContentText(resources.getString(R.string.loginMessageFailed)) //doesn't accept int (use string explicitly)
                                        .setConfirmText(resources.getString(R.string.ok))
                                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                        .show()
                            }

                            "UNKNOWN" -> runOnUiThread {
                                val failedDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                                failedDialog
                                        .setTitleText(R.string.loginTitleFailed) //accepts also int (can be used)
                                        .setContentText(resources.getString(R.string.loginMessageUnknown)) //doesn't accept int (use string explicitly)
                                        .setConfirmText(resources.getString(R.string.ok))
                                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                        .show()
                            }

                            else -> runOnUiThread {
                                val failedDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.WARNING_TYPE)
                                failedDialog
                                        .setTitleText(R.string.loginTitleFailed)
                                        .setContentText(resources.getString(R.string.regMessageServerError))
                                        .setConfirmText(resources.getString(R.string.ok))
                                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                        .show()
                            }
                        }
                    }
                    500 -> {
                        runOnUiThread {
                            val failedDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                            failedDialog
                                    .setTitleText(R.string.loginTitleFailed)
                                    .setContentText(resources.getString(R.string.loginMessageFailed))
                                    .setConfirmText(resources.getString(R.string.ok))
                                    .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                    .show()
                        }
                    }
                    else -> {
                        runOnUiThread {
                            val failedDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.WARNING_TYPE)
                            failedDialog
                                    .setTitleText(R.string.loginTitleFailed)
                                    .setContentText(resources.getString(R.string.regMessageServerError))
                                    .setConfirmText(resources.getString(R.string.ok))
                                    .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                                    .show()
                        }
                    }
                }
                connection.disconnect()
            } catch (e: IOException) {
                runOnUiThread {
                    val failedDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.WARNING_TYPE)
                    failedDialog
                            .setTitleText(R.string.loginTitleFailed)
                            .setContentText(resources.getString(R.string.regMessageServerError))
                            .setConfirmText(resources.getString(R.string.ok))
                            .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                            .show()
                }
                e.printStackTrace()
            }
        }
        thread.start()
    }
}