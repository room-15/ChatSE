package com.tristanwiley.chatse.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.tristanwiley.chatse.BaseActivity
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatActivity
import com.tristanwiley.chatse.extensions.showIf
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : BaseActivity(), LoginView {

    private lateinit var presenter: LoginPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        presenter = LoginPresenter()

        beta_text.setOnClickListener { presenter.onBetaClicked() }

        fab_submit.setOnClickListener {
            val email = login_email.text.toString()
            val password = login_password.text.toString()
            presenter.onLoginClicked(email, password)
        }

        login_password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    val email = login_email.text.toString()
                    val password = login_password.text.toString()
                    presenter.onFormFilledOut(email, password)
                    false
                }
                else -> false
            }
        }

        presenter.attachView(this)
    }

    override fun setVersionText(text: String) {
        login_tv_version.text = text
    }

    override fun setEmailText(text: String) {
        login_email.setText(text)
    }

    override fun navigateToGithubPage() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/room-15/ChatSE/")))
    }

    override fun setLoginEnabled(isEnabled: Boolean) {
        fab_submit.isClickable = isEnabled
    }

    override fun showEmailEmptyError() {
        login_email.error = getString(R.string.err_blank_email)
    }

    override fun showEmailInvalidError() {
        login_email.error = getString(R.string.err_invalid_email)
    }

    override fun showPasswordError() {
        login_password.error = getString(R.string.err_blank_password)
    }

    override fun hideFormErrors() {
        login_email.error = null
        login_password.error = null
    }

    override fun navigateToChat() {
        runOnUiThread {
            val intent = Intent(this@LoginActivity, ChatActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun showLogInError() {
        runOnUiThread {
            Toast.makeText(this@LoginActivity, "Failed to log in, try again!", Toast.LENGTH_LONG).show()
        }
    }

    override fun setLoginInProgressVisibility(isVisible: Boolean) {
        runOnUiThread {
            progress_bar_logging_in.showIf { isVisible }
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

}

interface LoginView {
    fun setVersionText(text: String)
    fun setEmailText(text: String)
    fun navigateToGithubPage()
    fun setLoginEnabled(isEnabled: Boolean)
    fun showEmailEmptyError()
    fun showEmailInvalidError()
    fun showPasswordError()
    fun hideFormErrors()
    fun setLoginInProgressVisibility(isVisible: Boolean)
    fun showLogInError()
    fun navigateToChat()
}
