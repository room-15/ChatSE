package me.shreyasr.chatse.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import java.io.IOException

import me.shreyasr.chatse.App
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.ChatActivity
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager

class LoginActivity : AppCompatActivity() {

    internal lateinit var emailView: EditText
    internal lateinit var passwordView: EditText
    internal lateinit var progressBar: ProgressBar
    internal lateinit var loginButton: Button

    internal lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = App.getPrefs(this)

        if (prefs.getBoolean(App.PREF_HAS_CREDS, false)) {
            this.startActivity(Intent(this@LoginActivity, ChatActivity()::class.java))
            this.finish()
            return
        }

        setContentView(R.layout.activity_login)

        emailView = findViewById(R.id.login_email) as EditText
        passwordView = findViewById(R.id.login_password) as EditText
        progressBar = findViewById(R.id.login_progress) as ProgressBar
        loginButton = findViewById(R.id.login_submit) as Button

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        loginButton.setOnClickListener { attemptLogin() }

        emailView.setText(prefs.getString(App.PREF_EMAIL, ""))
        passwordView.setText(prefs.getString("password", "")) // STOPSHIP
        passwordView.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == R.id.login_submit || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
    }

    internal fun attemptLogin() {
        loginButton.isEnabled = false

        // Reset errors.
        emailView.error = null
        passwordView.error = null

        val errorView = validateInputs()

        if (errorView != null) {
            errorView.requestFocus()
            loginButton.isEnabled = true
            return
        }

        progressBar.visibility = View.VISIBLE

        LoginAsyncTask().execute(emailView.text.toString(), passwordView.text.toString())
    }

    private fun validateInputs(): View? {
        val email = emailView.text.toString()
        val password = passwordView.text.toString()

        if (password.isEmpty()) {
            passwordView.error = getString(R.string.err_blank_password)
            return passwordView
        }

        if (email.isEmpty()) {
            emailView.error = getString(R.string.err_blank_email)
            return emailView
        }

        if (!isEmailValid(email)) {
            emailView.error = getString(R.string.err_invalid_email)
            return emailView
        }

        // STOPSHIP
        prefs.edit().putString(App.PREF_EMAIL, email).putString("password", password).apply()

        return null
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") //TODO Improve email prevalidation
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private inner class LoginAsyncTask : AsyncTask<String, Void, Boolean>() {

        override fun doInBackground(vararg params: String): Boolean? {
            val email = params[0]
            val password = params[1]

            try {
                val client = ClientManager.client

                seOpenIdLogin(client, email, password)
                loginToSE(client)
                loginToSite(client, "https://stackoverflow.com", email, password)
                return true
            } catch (e: IOException) {
                Log.e(e.javaClass.getSimpleName(), e.message, e)
                return false
            }

        }

        override fun onPostExecute(success: Boolean?) {
            if (success!!) {
                prefs.edit().putBoolean(App.PREF_HAS_CREDS, true).apply()
                this@LoginActivity.startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                this@LoginActivity.finish()
            } else {
                progressBar.visibility = View.GONE
                loginButton.isEnabled = false
                Toast.makeText(this@LoginActivity, "Failed to connect", Toast.LENGTH_LONG).show()
            }
        }

        @Throws(IOException::class)
        private fun loginToSite(client: Client, site: String,
                                email: String, password: String) {
            val soFkey = Jsoup.connect(site + "/users/login/")
                    .userAgent(Client.USER_AGENT).get()
                    .select("input[name=fkey]").attr("value")

            val soLoginRequestBody = FormEncodingBuilder()
                    .add("email", email)
                    .add("password", password)
                    .add("fkey", soFkey)
                    .build()
            val soLoginRequest = Request.Builder()
                    .url(site + "/users/login/")
                    .post(soLoginRequestBody)
                    .build()
            val soLoginResponse = client.newCall(soLoginRequest).execute()
            Log.i("site login", soLoginResponse.toString())
        }

        @Throws(IOException::class)
        private fun loginToSE(client: Client) {
            val loginPageRequest = Request.Builder()
                    .url("http://stackexchange.com/users/login/")
                    .build()
            val loginPageResponse = client.newCall(loginPageRequest).execute()

            val doc = Jsoup.parse(loginPageResponse.body().string())
            val fkeyElements = doc.select("input[name=fkey]")
            val fkey = fkeyElements.attr("value")

            if (fkey == "") throw IOException("Fatal: No fkey found.")

            val data = FormEncodingBuilder()
                    .add("oauth_version", "")
                    .add("oauth_server", "")
                    .add("openid_identifier", "https://openid.stackexchange.com/")
                    .add("fkey", fkey)

            val loginRequest = Request.Builder()
                    .url("https://stackexchange.com/users/authenticate/")
                    .post(data.build())
                    .build()
            val loginResponse = client.newCall(loginRequest).execute()
            Log.i("se login", loginResponse.toString())
        }

        @Throws(IOException::class)
        private fun seOpenIdLogin(client: Client, email: String, password: String) {
            val seLoginPageRequest = Request.Builder()
                    .url("https://openid.stackexchange.com/account/login/")
                    .build()
            val seLoginPageResponse = client.newCall(seLoginPageRequest).execute()

            val seLoginDoc = Jsoup.parse(seLoginPageResponse.body().string())
            val seLoginFkeyElements = seLoginDoc.select("input[name=fkey]")
            val seFkey = seLoginFkeyElements.attr("value")

            val seLoginRequestBody = FormEncodingBuilder()
                    .add("email", email)
                    .add("password", password)
                    .add("fkey", seFkey)
                    .build()
            val seLoginRequest = Request.Builder()
                    .url("https://openid.stackexchange.com/account/login/submit/")
                    .post(seLoginRequestBody)
                    .build()
            val seLoginResponse = client.newCall(seLoginRequest).execute()
            Log.i("se openid login", seLoginResponse.toString())
        }
    }
}
