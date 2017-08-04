package me.shreyasr.chatse.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import me.shreyasr.chatse.App
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.ChatActivity
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.IOException


/**
 * Activity to login the user.
 */
class LoginActivity : AppCompatActivity() {
    /**
     * Initiate variables to be used.
     * @emailView: The EditText used to take in the user's email
     * @passwordView: The EditText used to take in the user's password
     * @progressBar: The ProgressBar displayed while the authentication is being performed
     * @prefs: Variable used to contain the default SharedPreferences for the app. Set to App.sharedPreferences
     */
    lateinit var emailView: EditText
    lateinit var passwordView: EditText
    lateinit var progressBar: ProgressBar
    lateinit var loginButton: Button
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Determine if the user has logged in already, if they have, proceed to the ChatActivity and finish the LoginActivity
        prefs = App.sharedPreferences
        if (prefs.getBoolean(App.PREF_HAS_CREDS, false)) {
            this.startActivity(Intent(this, ChatActivity::class.java))
            this.finish()
            return
        }

        //If the user has not logged in already, display the chat login layout
        setContentView(R.layout.activity_login)

        // Set variables to the layout
        emailView = findViewById(R.id.login_email) as EditText
        passwordView = findViewById(R.id.login_password) as EditText
        progressBar = findViewById(R.id.login_progress) as ProgressBar
        loginButton = findViewById(R.id.login_submit) as Button

        //Set the toolbar as the SupportActionBar for the Activity
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        //If the loginButton is clicked attempt a login.
        loginButton.setOnClickListener { attemptLogin() }

        //Set the emailView teext to the email saved in the preferences.
        emailView.setText(prefs.getString(App.PREF_EMAIL, ""))

        //When the user presses submit inside the passwordView, attempt a login.
        passwordView.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == R.id.login_submit || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
    }

    /**
     * Receives the input from the email and password views.
     * @emailView gets it's errors reset
     * @passwordView gets it's errors reset
     * The inputs are validated
     * @loginButton is re-enabled if the inputs are not valid
     * @progressBar is visible if all inputs are valid and the login is attempted
     * @LoginAsyncTask is called
     */
    fun attemptLogin() {
        loginButton.isEnabled = false

        // Reset errors.
        emailView.error = null
        passwordView.error = null

        if (!validateInputs()) {
            loginButton.isEnabled = true
            return
        }

        progressBar.visibility = View.VISIBLE

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

                if (loginToSite(client, "https://stackoverflow.com", email, password)) {
                    seOpenIdLogin(client, email, password)
                    loginToSE(client)
                    runOnUiThread {
                        prefs.edit().putBoolean(App.PREF_HAS_CREDS, true).apply()
                        this@LoginActivity.startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                        this@LoginActivity.finish()
                    }
                } else {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = false
                    Toast.makeText(this@LoginActivity, "Failed to log in, try again!", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                Timber.e(e)
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
        val initElement = scriptElements.toMutableList().filter { it.html().contains("userId") && it.html().contains("accountId") }[0].html()

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
        client.newCall(loginRequest).execute()

        //Get the main StackExchange ID (which is different from the SE user's chat ID and set the chat ID by calling setSEChatId
        val SEID = defaultSharedPreferences.getInt("SEMAINID", -1)
        if (SEID != -1) {
            setSEChatId(client)
        }
    }

    /**
     * Logins to OpenId, takes in 1 params
     * @param client = the OkHttp ClientManager.client
     */
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
        Timber.i("Se openid login: " + seLoginResponse.toString())
    }

    /**
     * Gets the user's chat ID for StackExchange which is different from their normal ID
     * Sets it to the defaultSharedPreferences as "SEID"
     */
    private fun setSEChatId(client: Client) {
        val sePageRequest = Request.Builder()
                .url("https://chat.stackexchange.com/")
                .build()
        val sePageResponse = client.newCall(sePageRequest).execute()

        val sePageDoc = Jsoup.parse(sePageResponse.body().string())

        //Get the URL from the topbar (to their profile)
        val url = sePageDoc.getElementsByClass("topbar-menu-links")[0].child(0).attr("href")

        //Split the URL and get's their id which is between two slashes /theirid/their-profile-name
        val res = url.split("/")[2]
        defaultSharedPreferences.edit().putInt("SEID", res.toInt()).apply()
    }
}
