package com.tristanwiley.chatse.login

import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import com.tristanwiley.chatse.util.SharedPreferenceManager
import com.tristanwiley.chatse.util.UserPreferenceKeys
import com.tristanwiley.chatse.util.ioScheduler
import io.reactivex.Completable
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

class LoginUseCase {

    private val prefs = SharedPreferenceManager.sharedPreferences

    fun execute(params: LoginParams): Completable =
            Completable.fromAction {
                loginToSites(params.email, params.password)
            }
                    .subscribeOn(ioScheduler())

    /**
     * Log in to all sites.
     */
    private fun loginToSites(email: String, password: String) {
        val client = ClientManager.client

        if (seOpenIdLogin(client, email, password)) {
            loginToSE(client)
            loginToSite(client, "https://stackoverflow.com", email, password)
            prefs.edit().putBoolean(UserPreferenceKeys.IS_LOGGED_IN, true).apply()
        } else {
            throw LoginFailedException()
        }
    }

    /**
     * Logs in to a site.
     */
    @Throws(IOException::class)
    private fun loginToSite(client: Client, site: String, email: String, password: String): Boolean {

        // Connect to /users/login and get the fkey, this key is necessary to login to the site.
        val soFkey = Jsoup.connect("$site/users/login/")
                .userAgent(Client.USER_AGENT).get()
                .select("input[name=fkey]").attr("value")

        // The request body is created by adding the email, password, and fkey to the request body.
        val soLoginRequestBody = FormEncodingBuilder()
                .add("email", email)
                .add("password", password)
                .add("fkey", soFkey)
                .build()

        // The login request is built by combining the URL and the request body created earlier.
        val soLoginRequest = Request.Builder()
                .url("$site/users/login/")
                .post(soLoginRequestBody)
                .build()

        // The call is executed and the response is received to be parsed for future use.
        val soLoginResponse = client.newCall(soLoginRequest).execute()

        // The response document is pared into a Jsoup document.
        val responseDoc = Jsoup.parse(soLoginResponse.body().string())

        // All script elements are received from the HTML.
        val scriptElements = responseDoc.getElementsByTag("script")

        /** The scripts are filtered so we get the one we want. We want the one that contains the text of both "userId" and "accountId"
         * @accountId is used for the StackExchange userID
         * @userId is used for the StackOverflow userID
         */
        val initElements = scriptElements.filter { it.html().contains("userId") && it.html().contains("accountId") }
        val initElement: String

        // Verify that there's the correct script tag, otherwise the login was bad.
        if (initElements.isNotEmpty()) {
            initElement = initElements[0].html()
        } else {
            return false
        }

        // Verify this element contains the following text, meaning it's the one we want.
        if (initElement.contains("StackExchange.init(")) {
            // Get only the JSON from the text and parse it as such.
            var json = initElement.replace("StackExchange.init(", "")
            json = json.substring(0, json.length - 2)
            val userObj = JSONObject(json).getJSONObject("user")

            // If the JSON has the two Strings we want, parse them.
            if (userObj.has("userId") && userObj.has("accountId")) {
                val SOID = JSONObject(json).getJSONObject("user").getInt("userId")
                val SEID = JSONObject(json).getJSONObject("user").getInt("accountId")

                prefs.edit().putInt("SOID", SOID).putInt("SEMAINID", SEID).putString("email", email).apply()
            } else {
                return false
            }
        }
        return true
    }

    /**
     * Logs in to Stack Exchange.
     */
    @Throws(IOException::class)
    private fun loginToSE(client: Client) {
        // Build the request for logging into StackExchange.
        val loginPageRequest = Request.Builder()
                .url("https://stackexchange.com/users/login/")
                .build()
        // Execute the request so we can parse the response.
        val loginPageResponse = client.newCall(loginPageRequest).execute()

        // Parse the response and get the glorious fkey!
        val doc = Jsoup.parse(loginPageResponse.body().string())
        loginPageResponse.body().close()

        val fkeyElements = doc.select("input[name=fkey]")
        val fkey = fkeyElements.attr("value")

        // Make sure fkey is not empty.
        if (fkey == "") throw IOException("Fatal: No fkey found.")

        // Build a request to login to SE.
        val data = FormEncodingBuilder()
                .add("oauth_version", "")
                .add("oauth_server", "")
                .add("openid_identifier", "https://openid.stackexchange.com/")
                .add("fkey", fkey)

        // Login to SE and execute request.
        val loginRequest = Request.Builder()
                .url("https://stackexchange.com/users/authenticate/")
                .post(data.build())
                .build()
        val response = client.newCall(loginRequest).execute()
        val body = response.body()
        body.close()

        // Get the main StackExchange ID (which is different from the SE user's chat ID and
        // set the chat ID by calling setSEChatId.
        setSEChatId()
    }

    /**
     * Logs in to OpenId, takes in 1 params
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
     * Gets the user's chat ID for StackExchange which is different from their normal ID and
     * sets it to the defaultSharedPreferences as "SEID".
     */
    private fun setSEChatId() {
        val sePageRequest = Request.Builder()
                .url("https://chat.stackexchange.com/")
                .build()
        val sePageResponse = ClientManager.client.newCall(sePageRequest).execute()

        val sePageDoc = Jsoup.parse(sePageResponse.body().string())
        sePageResponse.body().close()

        // Get the URL from the topbar (to their profile).
        val element = sePageDoc.getElementsByClass("topbar-menu-links")[0].child(0)

        val url: String
        if (element.hasAttr("title")) {
            url = element.attr("href")
        } else {
            return
        }

        // Split the URL and get's their id which is between two slashes
        // E.g. /theirid/their-profile-name.
        val res = url.split("/")[2]
        prefs.edit().putInt("SEID", res.toInt()).apply()
    }

}

data class LoginParams(
        val email: String,
        val password: String
)

class LoginFailedException(cause: Throwable? = null) : Exception(cause)
