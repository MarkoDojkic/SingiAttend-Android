package dev.markodojkic.singiattend

import android.annotation.SuppressLint
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
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import dev.markodojkic.singiattend.MainActivity.Companion.csrfTokenManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64

class LoginActivity : AppCompatActivity() {
    private lateinit var indexTxt: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val windowMetrics = windowManager.currentWindowMetrics
        window.setLayout(
            (windowMetrics.bounds.width() * .95).toInt(),
            (windowMetrics.bounds.height() * .55).toInt()
        )
        val params = window.attributes
        params.gravity = Gravity.CENTER
        params.x = 0
        params.y = 0
        window.attributes = params
        indexTxt = findViewById(R.id.index_txt)
        indexTxt.filters = arrayOf<InputFilter>(LengthFilter(11))
        indexTxt.inputType = InputType.TYPE_CLASS_NUMBER
        indexTxt.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                indexTxt.setOnKeyListener { _: View?, keyCode: Int, _: KeyEvent? ->
                    if (indexTxt.text.toString().length == 5) {
                        if (keyCode == KeyEvent.KEYCODE_DEL) {
                            indexTxt.setText(indexTxt.text.toString().substring(0, 3))
                            indexTxt.setSelection(3)
                        } else {
                            indexTxt.setText(
                                "${
                                    indexTxt.text.toString().substring(0, 4)
                                }/${indexTxt.text.toString().substring(4, indexTxt.text.length)}"
                            )
                            indexTxt.setSelection(6)
                        }
                    }
                    false
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { /* Not used */ }
            override fun afterTextChanged(s: Editable) { /* Not used */ }
        })

        findViewById<Button>(R.id.login_btn).setOnClickListener {
            login(
                indexTxt.text.toString(),
                (findViewById<View>(R.id.pass_txt) as EditText).text.toString(),
                getProxyIdentifier()
            )
        }

        findViewById<Button>(R.id.login_with_biometrics_btn).setOnClickListener {
            if (SecureStorage.load(this@LoginActivity, "biometricsStudentIndex")?.contentToString() == null) {
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

                return@setOnClickListener
            }

            BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(applicationContext),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        login(
                            SecureStorage.load(this@LoginActivity, "biometricsStudentIndex").toString(),
                            SecureStorage.load(this@LoginActivity, "biometricsStudentPassword")
                                .toString(),
                            SecureStorage.load(this@LoginActivity, "biometricsStudentProxyIdentifier")
                                .toString()
                        )
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

                        val message: String = when (errorCode) {
                            BiometricPrompt.ERROR_HW_UNAVAILABLE -> resources.getString(R.string.biometricsNotAvailable)
                            BiometricPrompt.ERROR_HW_NOT_PRESENT -> resources.getString(R.string.biometricsNotAvailable)
                            BiometricPrompt.ERROR_NO_BIOMETRICS -> resources.getString(R.string.biometricsNotEnrolled)
                            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> resources.getString(R.string.biometricsNotEnrolled)
                            BiometricPrompt.ERROR_LOCKOUT -> resources.getString(R.string.biometricsLockout)
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> resources.getString(R.string.biometricsLockout)
                            else -> resources.getString(R.string.biometricsGenericError)
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
                }).authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(resources.getString(R.string.loginUsingBiometricsAuthentication))
                    .setSubtitle(
                        String.format(
                            resources.getString(R.string.biometricsLoginReason),
                            SecureStorage.load(this@LoginActivity, "biometricsStudentIndex").toString()
                        )
                    )
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
            )
        }

        findViewById<Button>(R.id.onRegistration_btn).setOnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun getProxyIdentifier(): String {
        val radioGroup = findViewById<RadioGroup>(R.id.faculty_place_rg)
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId

        return if (selectedRadioButtonId != -1) findViewById<RadioButton>(radioGroup.checkedRadioButtonId).tag.toString() else ""
    }

    private fun login(index: String, password: String, proxyIdentifier: String) {
        if (proxyIdentifier.isEmpty()) {
            val failedDialog = SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
            failedDialog
                .setTitleText(R.string.loginTitleFailed)
                .setContentText(resources.getString(R.string.facultyPlaceNotSelected))
                .setConfirmText(resources.getString(R.string.ok))
                .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                .show()

            return
        }

        csrfTokenManager.proxyIdentifier = proxyIdentifier

        csrfTokenManager.fetchCsrfSession { success ->
            if (!success) {
                runOnUiThread {
                    val failedDialog =
                        SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                    failedDialog
                        .setTitleText(R.string.loginTitleFailed)
                        .setContentText(resources.getString(R.string.serverInactive))
                        .setConfirmText(resources.getString(R.string.ok))
                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                        .show()
                }
            }


            if (csrfTokenManager.proxyIdentifier.isEmpty() || csrfTokenManager.sessionData.jsessionId.isEmpty() || csrfTokenManager.sessionData.xsrfToken.isEmpty() || csrfTokenManager.sessionData.csrfTokenSecret.isEmpty() || csrfTokenManager.sessionData.csrfHeaderName.isEmpty()) {
                runOnUiThread {
                    val failedDialog =
                        SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                    failedDialog
                        .setTitleText(R.string.loginTitleFailed)
                        .setContentText(resources.getString(R.string.serverInactive))
                        .setConfirmText(resources.getString(R.string.ok))
                        .setConfirmClickListener { _: SweetAlertDialog? -> failedDialog.dismiss() }
                        .show()
                }
                csrfTokenManager.logoutFromCsrfSession()
                return@fetchCsrfSession
            }

            OkHttpClient().newCall(
                Request.Builder()
                    .url(
                        "${BuildConfig.SERVER_URL}/api/checkPassword/student/${
                            index.replace(
                                "/",
                                ""
                            )
                        }"
                    )
                    .post(password.toRequestBody("text/plain".toMediaTypeOrNull()))
                    .addHeader(
                        "Authorization", "Basic ${
                            Base64.getEncoder()
                                .encodeToString(
                                    BuildConfig.SERVER_CREDENTIALS.toByteArray(
                                        StandardCharsets.UTF_8
                                    )
                                )
                        }"
                    )
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Tenant-ID", proxyIdentifier)
                    .addHeader(
                        csrfTokenManager.sessionData.csrfHeaderName,
                        csrfTokenManager.sessionData.csrfTokenSecret
                    )
                    .addHeader(
                        "Cookie",
                        "JSESSIONID=${csrfTokenManager.sessionData.jsessionId}; XSRF-TOKEN=${csrfTokenManager.sessionData.xsrfToken}"
                    )
                    .build()
            ).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        SweetAlertDialog(this@LoginActivity, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText(R.string.loginTitleFailed)
                            .setContentText(resources.getString(R.string.regMessageServerError))
                            .setConfirmText(resources.getString(R.string.ok))
                            .setConfirmClickListener { it?.dismiss() }
                            .show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        val responseBody = response.body?.string() ?: ""
                        when (response.code) {
                            200 -> when (responseBody) {
                                "VALID" -> SweetAlertDialog(
                                    this@LoginActivity,
                                    SweetAlertDialog.SUCCESS_TYPE
                                )
                                    .setTitleText(R.string.loginTitleSuccess)
                                    .setConfirmText(resources.getString(R.string.ok))
                                    .setConfirmClickListener {
                                        it?.dismiss()
                                        val returnIntent = Intent()
                                        SecureStorage.save(this@LoginActivity, "loggedInStudentIndex", index.toByteArray())
                                        SecureStorage.save(this@LoginActivity, "loggedInStudentProxyIdentifier", proxyIdentifier.toByteArray())

                                        if (findViewById<SwitchCompat>(R.id.save_for_biometrics_switch).isChecked) {
                                            SecureStorage.save(this@LoginActivity, "biometricsStudentIndex", index.toByteArray())
                                            SecureStorage.save(this@LoginActivity, "biometricsStudentPassword", password.toByteArray())
                                            SecureStorage.save(this@LoginActivity, "biometricsStudentProxyIdentifier", proxyIdentifier.toByteArray())

                                            SweetAlertDialog(
                                                this@LoginActivity,
                                                SweetAlertDialog.SUCCESS_TYPE
                                            )
                                                .setTitleText(R.string.titleCredentialsSaved)
                                                .setContentText(resources.getString(R.string.messageCredentialsSaved))
                                                .setConfirmText(resources.getString(R.string.ok))
                                                .setConfirmClickListener { it2 ->
                                                    it2?.dismiss()
                                                    setResult(RESULT_OK, returnIntent)
                                                    finish()
                                                }
                                                .show()
                                        } else {
                                            setResult(RESULT_OK, returnIntent)
                                            finish()
                                        }
                                    }.show()

                                "INVALID" -> showFailDialog(R.string.loginMessageFailed)
                                "UNKNOWN" -> showFailDialog(R.string.loginMessageUnknown)
                                else -> showFailDialog(
                                    R.string.regMessageServerError,
                                    SweetAlertDialog.WARNING_TYPE
                                )
                            }

                            500 -> {
                                SecureStorage.deleteAll(this@LoginActivity)
                                showFailDialog(R.string.loginMessageFailed)
                            }

                            else -> showFailDialog(
                                R.string.regMessageServerError,
                                SweetAlertDialog.WARNING_TYPE
                            )
                        }
                    }
                }

                fun showFailDialog(msgRes: Int, type: Int = SweetAlertDialog.ERROR_TYPE) {
                    runOnUiThread {
                        SweetAlertDialog(this@LoginActivity, type)
                            .setTitleText(R.string.loginTitleFailed)
                            .setContentText(resources.getString(msgRes))
                            .setConfirmText(resources.getString(R.string.ok))
                            .setConfirmClickListener { it?.dismiss() }
                            .show()
                    }
                }
            })
        }
    }
}