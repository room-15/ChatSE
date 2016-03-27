package me.shreyasr.chatse.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.shreyasr.chatse.App;
import me.shreyasr.chatse.R;
import me.shreyasr.chatse.chat.ChatActivity;
import me.shreyasr.chatse.network.Client;
import me.shreyasr.chatse.network.ClientManager;

public class LoginActivity extends AppCompatActivity {

    @Bind(R.id.login_email)    EditText emailView;
    @Bind(R.id.login_password) EditText passwordView;
    @Bind(R.id.login_progress) ProgressBar progressBar;
    @Bind(R.id.login_submit)   Button loginButton;

    SharedPreferences prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = App.getPrefs(this);

        if (prefs.getBoolean(App.PREF_HAS_CREDS, false)) {
            this.startActivity(new Intent(LoginActivity.this, ChatActivity.class));
            this.finish();
            return;
        }

        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        emailView.setText(prefs.getString(App.PREF_EMAIL, ""));
        passwordView.setText(prefs.getString("password", "")); // STOPSHIP
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login_submit || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
    }

    @OnClick(R.id.login_submit) void attemptLogin() {
        loginButton.setEnabled(false);

        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);

        View errorView = validateInputs();

        if (errorView != null) {
            errorView.requestFocus();
            loginButton.setEnabled(true);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        new LoginAsyncTask().execute(emailView.getText().toString(), passwordView.getText().toString());
    }

    private View validateInputs() {
        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        if (password.isEmpty()) {
            passwordView.setError(getString(R.string.err_blank_password));
            return passwordView;
        }

        if (email.isEmpty()) {
            emailView.setError(getString(R.string.err_blank_email));
            return emailView;
        }

        if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.err_invalid_email));
            return emailView;
        }

        // STOPSHIP
        prefs.edit().putString(App.PREF_EMAIL, email).putString("password", password).apply();

        return null;
    }

    private boolean isEmailValid(String email) {
        return email.contains("@"); //TODO Improve email prevalidation
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class LoginAsyncTask extends AsyncTask<String, Void, Boolean> {

        @Override protected Boolean doInBackground(String... params) {
            String email = params[0];
            String password = params[1];

            try {
                Client client = ClientManager.getClient();

                seOpenIdLogin(client, email, password);
                loginToSE(client);
                loginToSite(client, "https://stackoverflow.com", email, password);
                return true;
            } catch (IOException e) {
                Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                return false;
            }
        }

        protected void onPostExecute(Boolean success) {
            if (success) {
                prefs.edit().putBoolean(App.PREF_HAS_CREDS, true).apply();
                LoginActivity.this.startActivity(new Intent(LoginActivity.this, ChatActivity.class));
                LoginActivity.this.finish();
            } else {
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(false);
                Toast.makeText(LoginActivity.this, "Failed to connect", Toast.LENGTH_LONG).show();
            }
        }

        private void loginToSite(Client client, String site,
                                 String email, String password) throws IOException {
            String soFkey = Jsoup.connect(site + "/users/login/")
                    .userAgent(Client.USER_AGENT).get()
                    .select("input[name=fkey]").attr("value");

            RequestBody soLoginRequestBody = new FormEncodingBuilder()
                    .add("email", email)
                    .add("password", password)
                    .add("fkey", soFkey)
                    .build();
            Request soLoginRequest = new Request.Builder()
                    .url(site + "/users/login/")
                    .post(soLoginRequestBody)
                    .build();
            Response soLoginResponse = client.newCall(soLoginRequest).execute();
            Log.i("site login", soLoginResponse.toString());
        }

        private void loginToSE(Client client) throws IOException {
            Request loginPageRequest = new Request.Builder()
                    .url("http://stackexchange.com/users/login/")
                    .build();
            Response loginPageResponse = client.newCall(loginPageRequest).execute();

            Document doc = Jsoup.parse(loginPageResponse.body().string());
            Elements fkeyElements = doc.select("input[name=fkey]");
            String fkey = fkeyElements.attr("value");

            if (fkey.equals("")) throw new IOException("Fatal: No fkey found.");

            FormEncodingBuilder data = new FormEncodingBuilder()
                    .add("oauth_version", "")
                    .add("oauth_server", "")
                    .add("openid_identifier", "https://openid.stackexchange.com/")
                    .add("fkey", fkey);

            Request loginRequest = new Request.Builder()
                    .url("https://stackexchange.com/users/authenticate/")
                    .post(data.build())
                    .build();
            Response loginResponse = client.newCall(loginRequest).execute();
            Log.i("se login", loginResponse.toString());
        }

        private void seOpenIdLogin(Client client, String email, String password) throws IOException {
            Request seLoginPageRequest = new Request.Builder()
                    .url("https://openid.stackexchange.com/account/login/")
                    .build();
            Response seLoginPageResponse = client.newCall(seLoginPageRequest).execute();

            Document seLoginDoc = Jsoup.parse(seLoginPageResponse.body().string());
            Elements seLoginFkeyElements = seLoginDoc.select("input[name=fkey]");
            String seFkey = seLoginFkeyElements.attr("value");

            RequestBody seLoginRequestBody = new FormEncodingBuilder()
                    .add("email", email)
                    .add("password", password)
                    .add("fkey", seFkey)
                    .build();
            Request seLoginRequest = new Request.Builder()
                    .url("https://openid.stackexchange.com/account/login/submit/")
                    .post(seLoginRequestBody)
                    .build();
            Response seLoginResponse = client.newCall(seLoginRequest).execute();
            Log.i("se openid login", seLoginResponse.toString());
        }
    }
}
