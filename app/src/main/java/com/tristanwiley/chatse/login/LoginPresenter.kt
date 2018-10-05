package com.tristanwiley.chatse.login

import com.tristanwiley.chatse.App
import com.tristanwiley.chatse.BuildConfig
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.util.SharedPreferenceManager
import com.tristanwiley.chatse.util.UserPreferenceKeys
import com.tristanwiley.chatse.util.uiScheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern

class LoginPresenter {

    private lateinit var view: LoginView

    private val disposables = CompositeDisposable()

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

        val disposable = LoginUseCase()
                .execute(LoginParams(email, password))
                .observeOn(uiScheduler())
                .subscribe({
                    view.navigateToChat()
                }, {
                    handleLoginError()
                    Timber.e(it)
                })
        disposables.add(disposable)
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


    private fun handleLoginError() {
        view.setLoginInProgressVisibility(false)
        view.setLoginEnabled(true)
        view.showLogInError()
    }

    fun detachView() {
        disposables.clear()
    }

    companion object {
        // Redefined from android.util.Patterns.EMAIL_ADDRESS to avoid using android packages.
        private val EMAIL_PATTERN = Pattern.compile("""[a-zA-Z0-9+._%\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+""")
    }

}
