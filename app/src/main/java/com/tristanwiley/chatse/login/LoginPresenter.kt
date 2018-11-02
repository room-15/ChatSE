package com.tristanwiley.chatse.login

import com.tristanwiley.chatse.App
import com.tristanwiley.chatse.BuildConfig
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.util.SharedPreferenceManager
import com.tristanwiley.chatse.util.UserPreferenceKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext


class LoginPresenter : CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val uiScope = CoroutineScope(coroutineContext)

    private lateinit var view: LoginView

    private val prefs = SharedPreferenceManager.sharedPreferences

    fun attachView(_view: LoginView) {
        view = _view

        view.setVersionText(String.format(Locale.getDefault(), App.instance.getString(R.string.app_version), BuildConfig.VERSION_NAME))
        view.setEmailText(prefs.getString(UserPreferenceKeys.EMAIL, ""))
    }

    fun onBetaClicked() {
        view.navigateToGithubPage()
    }

    fun onLoginClicked(email: String, password: String) {
        attemptLogin(email, password)
    }

    fun onFormFilledOut(email: String, password: String) {
        attemptLogin(email, password)
    }

    private fun attemptLogin(email: String, password: String) {
        view.setLoginInProgressVisibility(true)

        view.setLoginEnabled(false)
        view.hideFormErrors()

        var hasErrors = false
        if (!validateEmail(email)) {
            hasErrors = true
        }

        if (!validatePassword(password)) {
            hasErrors = true
        }

        if (hasErrors) {
            view.setLoginInProgressVisibility(false)
            view.setLoginEnabled(true)
            return
        }

        uiScope.launch {
            try {
                LoginUseCase().execute(LoginParams(email, password))
                view.navigateToChat()
            } catch (e: LoginFailedException) {
                view.showLogInError()
                Timber.e(e)
            } finally {
                view.setLoginInProgressVisibility(false)
                view.setLoginEnabled(true)
            }
        }
    }

    private fun validateEmail(email: String) = when {
        email.isEmpty() -> {
            view.showEmailEmptyError()
            false
        }
        !isEmailValid(email) -> {
            view.showEmailInvalidError()
            false
        }
        else -> true
    }

    private fun isEmailValid(email: String) = email.matches(EMAIL_PATTERN.toRegex())

    private fun validatePassword(password: String) = when {
        password.isBlank() -> {
            view.showPasswordError()
            false
        }
        else -> true
    }

    fun detachView() {
        job.cancel()
    }

    companion object {
        // Redefined from android.util.Patterns.EMAIL_ADDRESS to avoid using android packages.
        private val EMAIL_PATTERN = Pattern.compile("""[a-zA-Z0-9+._%\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+""")
    }

}
