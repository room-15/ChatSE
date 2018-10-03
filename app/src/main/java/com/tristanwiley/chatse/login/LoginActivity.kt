package com.tristanwiley.chatse.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.BuildConfig
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatActivity
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import com.tristanwiley.chatse.util.SharedPreferenceManager
import com.tristanwiley.chatse.util.UserPreferenceKeys
import com.tristanwiley.chatse.util.hide
import com.tristanwiley.chatse.util.show
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*

class LoginActivity : AppCompatActivity() {

    private val prefs = SharedPreferenceManager.sharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_tv_version.text = String.format(Locale.getDefault(), getString(R.string.app_version), BuildConfig.VERSION_NAME)

        beta_text.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/room-15/ChatSE/")))
        }

        fab_submit.setOnClickListener { attemptLogin() }

        login_email.setText(prefs.getString(UserPreferenceKeys.EMAIL, ""))

        login_password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    attemptLogin()
                    false
                }
                else -> false
            }
        }
    }

    private fun attemptLogin() {
        fab_submit.isClickable = false

        // Reset errors.
        login_email.error = null
        login_password.error = null

        if (!validateInputs()) {
            fab_submit.isClickable = true
            return
        }

        progress_bar_logging_in.show()

        loginToSites(login_email.text.toString(), login_password.text.toString())
    }

    /**
     * Simple function that validates inputs by checking if the two views have errors in them
     */
    private fun validateInputs(): Boolean {
        var isValid = true
        val email = login_email.text.toString()
        val password = login_password.text.toString()

        if (!isEmailValid(email)) {
            login_email.error = getString(R.string.err_invalid_email)
            isValid = false
        }

        if (email.isEmpty()) {
            login_email.error = getString(R.string.err_blank_email)
            isValid = false
        }

        if (password.isBlank()) {
            login_email.error = getString(R.string.err_blank_password)
            isValid = false
        }

        return isValid
    }

    private fun isEmailValid(email: String): Boolean {
        return email.matches(Patterns.EMAIL_ADDRESS.toRegex())
    }

    /**
     * Function that uses Anko's doAsync to login to all sites
     */
    private fun loginToSites(vararg params: String) {
        doAsync {
            val email = params[0]
            val password = params[1]

            try {
                val client = ClientManager.client

                if (seOpenIdLogin(client, email, password)) {
                    loginToSE(client)
                    loginToSite(client, "https://stackoverflow.com", email, password)
                    runOnUiThread {
                        prefs.edit().putBoolean(UserPreferenceKeys.IS_LOGGED_IN, true).apply()
                        this@LoginActivity.startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                        this@LoginActivity.finish()
                    }
                } else {
                    runOnUiThread {
                        progress_bar_logging_in.hide()
                        fab_submit.isClickable = true
                        Toast.makeText(this@LoginActivity, "Failed to log in, try again!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                Log.e("LoginActivity", e.message)
            }
        }
    }

    /**
     * Logins to site, takes in 4 params
     * @param client = the OkHttp ClientManager.client
     * @param site = the url for the site to login to
     * @param email = the user's email as a String
     * @param password = the user's password as a String
     */
    @Throws(IOException::class)
    private fun loginToSite(client: Client, site: String,
                            email: String, password: String): Boolean {

        //Connect to /users/login and get the fkey, this key is necessary to login to the site.
        val soFkey = Jsoup.connect("$site/users/login/")
                .userAgent(Client.USER_AGENT).get()
                .select("input[name=fkey]").attr("value")

        //The request body is created by adding the email, password, and fkey to the request body
        val soLoginRequestBody = FormEncodingBuilder()
                .add("email", email)
                .add("password", password)
                .add("fkey", soFkey)
                .build()

        //The login request is built by combining the URL and the request body created earlier
        val soLoginRequest = Request.Builder()
                .url("$site/users/login/")
                .post(soLoginRequestBody)
                .build()

        //The call is executed and the response is received to be parsed for future use.
        val soLoginResponse = client.newCall(soLoginRequest).execute()

        //The response document is pared into a Jsoup document
        val responseDoc = Jsoup.parse(soLoginResponse.body().string())

        //All script elements are received from the HTML
        val scriptElements = responseDoc.getElementsByTag("script")

        /** The scripts are filtered so we get the one we want. We want the one that contains the text of both "userId" and "accountId"
         * @accountId is used for the StackExchange userID
         * @userId is used for the StackOverflow userID
         */
        val initElements = scriptElements.filter { it.html().contains("userId") && it.html().contains("accountId") }
        val initElement: String

        //Verify that there's the correct script tag, otherwise the login was bad
        if (initElements.isNotEmpty()) {
            initElement = initElements[0].html()
        } else {
            return false
        }

        //Verify this element contains the following text, meaning it's the one we want
        if (initElement.contains("StackExchange.init(")) {
            //Get only the JSON from the text and parse it as such
            var json = initElement.replace("StackExchange.init(", "")
            json = json.substring(0, json.length - 2)
            val userObj = JSONObject(json).getJSONObject("user")

            //If the JSON has the two Strings we want, parse them
            if (userObj.has("userId") && userObj.has("accountId")) {
                val SOID = JSONObject(json).getJSONObject("user").getInt("userId")
                val SEID = JSONObject(json).getJSONObject("user").getInt("accountId")

                //Save the two IDs in the shared preferences
                defaultSharedPreferences.edit().putInt("SOID", SOID).putInt("SEMAINID", SEID).putString("email", email).apply()
            } else {
                return false
            }
        }
        return true
    }

    /**
     * Logins to site, takes in 1 params
     * @param client = the OkHttp ClientManager.client
     */
    @Throws(IOException::class)
    private fun loginToSE(client: Client) {
        //Build the request for logging into StackExchange
        val loginPageRequest = Request.Builder()
                .url("https://stackexchange.com/users/login/")
                .build()
        //Execute the request so we can parse the response
        val loginPageResponse = client.newCall(loginPageRequest).execute()

        //Parse the response and get the glorious fkey!
        val doc = Jsoup.parse(loginPageResponse.body().string())
        loginPageResponse.body().close()

        val fkeyElements = doc.select("input[name=fkey]")
        val fkey = fkeyElements.attr("value")

        //Make sure fkey is not empty
        if (fkey == "") throw IOException("Fatal: No fkey found.")

        //Build a request to login to SE
        val data = FormEncodingBuilder()
                .add("oauth_version", "")
                .add("oauth_server", "")
                .add("openid_identifier", "https://openid.stackexchange.com/")
                .add("fkey", fkey)

        //Login to SE and execute request
        val loginRequest = Request.Builder()
                .url("https://stackexchange.com/users/authenticate/")
                .post(data.build())
                .build()
        val response = client.newCall(loginRequest).execute()
        val body = response.body()
        body.close()

        //Get the main StackExchange ID (which is different from the SE user's chat ID and set the chat ID by calling setSEChatId
        setSEChatId()
    }

    /**
     * Logins to OpenId, takes in 1 params
     * @param client = the OkHttp ClientManager.client
     */
    @Throws(IOException::class)
    private fun seOpenIdLogin(client: Client, email: String, password: String): Boolean {
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
        val response = client.newCall(seLoginRequest).execute()
        val title = Jsoup.parse(response.body().string()).title()
        return !title.contains("error")
    }

    /**
     * Gets the user's chat ID for StackExchange which is different from their normal ID
     * Sets it to the defaultSharedPreferences as "SEID"
     */
    private fun setSEChatId() {
        val sePageRequest = Request.Builder()
                .url("https://chat.stackexchange.com/")
                .build()
        val sePageResponse = ClientManager.client.newCall(sePageRequest).execute()

        val sePageDoc = Jsoup.parse(sePageResponse.body().string())
        sePageResponse.body().close()

        //Get the URL from the topbar (to their profile)
        val element = sePageDoc.getElementsByClass("topbar-menu-links")[0].child(0)

        val url: String
        if (element.hasAttr("title")) {
            url = element.attr("href")
        } else {
            return
        }

        //Split the URL and get's their id which is between two slashes /theirid/their-profile-name
        val res = url.split("/")[2]
        defaultSharedPreferences.edit().putInt("SEID", res.toInt()).apply()
    }
}