package com.tristanwiley.chatse.login

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException


/**
 * Activity to login the user.
 */
class LoginActivity : AppCompatActivity() {
    /**
     * Initiate variables to be used.
     * @param emailView: The EditText used to take in the user's email
     * @param passwordView: The EditText used to take in the user's password
     * @param progressBar: The ProgressBar displayed while the authentication is being performed
     * @param prefs: Variable used to contain the default SharedPreferences for the app. Set to App.sharedPreferences
     */
    lateinit var emailView: EditText
    lateinit var passwordView: EditText
    lateinit var loginButton: FloatingActionButton
    lateinit var prefs: SharedPreferences
    lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Determine if the user has logged in already, if they have, proceed to the ChatActivity and finish the LoginActivity
        prefs = com.tristanwiley.chatse.App.sharedPreferences
        if (prefs.getBoolean(com.tristanwiley.chatse.App.PREF_HAS_CREDS, false)) {
            startActivity(Intent(this, com.tristanwiley.chatse.chat.ChatActivity::class.java))
            finish()
            return
        }

        //If the user has not logged in already, display the chat login layout
        setContentView(R.layout.activity_login_beautiful)

        dialog = ProgressDialog(this)
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.setMessage("Attempting to log in")
        dialog.isIndeterminate = true
        dialog.setTitle("Loading")
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        // Set variables to the layout
        emailView = findViewById(R.id.login_email)
        passwordView = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.fab_submit)


        //If the loginButton is clicked attempt a login.
        loginButton.setOnClickListener { attemptLogin() }

        //Set the emailView text to the email saved in the preferences.
        emailView.setText(prefs.getString(com.tristanwiley.chatse.App.PREF_EMAIL, ""))

        //When the user presses submit inside the passwordView, attempt a login.
        passwordView.setOnEditorActionListener({ _, id, _ ->
            if (id == R.id.fab_submit || id == EditorInfo.IME_NULL) {
                attemptLogin()
            }
            false
        })
    }

    /**
     * Receives the input from the email and password views.
     * @param emailView gets it's errors reset
     * @param passwordView gets it's errors reset
     * The inputs are validated
     * @param loginButton is re-enabled if the inputs are not valid
     * @param progressBar is visible if all inputs are valid and the login is attempted
     * @param LoginAsyncTask is called
     */
    fun attemptLogin() {
        loginButton.isClickable = false

        // Reset errors.
        emailView.error = null
        passwordView.error = null

        if (!validateInputs()) {
            loginButton.isClickable = true
            return
        }

        dialog.show()

        loginToSites(emailView.text.toString(), passwordView.text.toString())
    }

    /**
     * Simple function that validates inputs by checking if the two views have errors in them
     */
    private fun validateInputs(): Boolean {
        var isValid = true
        val email = emailView.text.toString()
        val password = passwordView.text.toString()

        if (!isEmailValid(email)) {
            emailView.error = getString(R.string.err_invalid_email)
            isValid = false
        }

        if (email.isEmpty()) {
            emailView.error = getString(R.string.err_blank_email)
            isValid = false
        }

        if (password.isNullOrBlank()) {
            passwordView.error = getString(R.string.err_blank_password)
            isValid = false
        }

        return isValid
    }

    /**
     * Function that determines if an email is valid.
     * As of now only checks if email contains an @
     * @return a boolean, true if the email is valid. False if not.
     */
    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") //TODO Improve email prevalidation
    }

    /**
     * Function that uses Anko's doAsync to login to all sites
     */
    fun loginToSites(vararg params: String) {
        doAsync {
            val email = params[0]
            val password = params[1]

            try {
                val client = ClientManager.client

                if (seOpenIdLogin(client, email, password)) {
                    loginToSE(client)
                    loginToSite(client, "https://stackoverflow.com", email, password)
                    runOnUiThread {
                        prefs.edit().putBoolean(com.tristanwiley.chatse.App.PREF_HAS_CREDS, true).apply()
                        this@LoginActivity.startActivity(Intent(this@LoginActivity, com.tristanwiley.chatse.chat.ChatActivity::class.java))
                        this@LoginActivity.finish()
                    }
                } else {
                    runOnUiThread {
                        dialog.dismiss()
                        loginButton.isClickable = true
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
        val soFkey = Jsoup.connect(site + "/users/login/")
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
                .url(site + "/users/login/")
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
                .url("http://stackexchange.com/users/login/")
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