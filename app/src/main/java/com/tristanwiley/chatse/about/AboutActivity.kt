package com.tristanwiley.chatse.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import com.tristanwiley.chatse.BuildConfig
import com.tristanwiley.chatse.R


class AboutActivity : AppCompatActivity() {

    lateinit var tvHeaderAppAbout: TextView
    lateinit var tvHeaderVersion: TextView
    lateinit var rvHeader: RecyclerView
    lateinit var rvDevs: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        getUiElements()

        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        val appBarLayout = findViewById<AppBarLayout>(R.id.about_appbar_layout)
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            internal var isShow = false
            internal var scrollRange = -1

            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.title = "About ChatSE"
                    isShow = true
                } else if (isShow) {
                    collapsingToolbarLayout.title = " "
                    isShow = false
                }
            }
        })
    }

    fun getUiElements() {
        tvHeaderVersion = findViewById(R.id.about_text_app_version)
        tvHeaderAppAbout = findViewById(R.id.about_text_app)

        tvHeaderVersion.text = String.format(
                getString(R.string.about_version), getString(R.string.app_name), BuildConfig.VERSION_NAME)
        tvHeaderAppAbout.text = getString(R.string.about_app)

        // Setup RecyclerView
        rvHeader = findViewById(R.id.about_rv_header_icons)

        rvHeader.layoutManager =
                GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)

        val headerArray = ArrayList<com.tristanwiley.chatse.about.pokos.AboutIconPoko>()
        // Rate app
        headerArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                R.drawable.ic_star_black,
                getString(R.string.buy_coffee),
                View.OnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.tristanwiley.chatse")))
                }))

        // Play Store
        headerArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                R.drawable.ic_google_play,
                getString(R.string.about_app_header_play_store),
                View.OnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.tristanwiley.chatse")))
                }
        ))

        // Share app
        headerArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                R.drawable.ic_share_black_24dp,
                getString(R.string.share_app),
                View.OnClickListener {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                            "Check out StackExchange's chat app! https://play.google.com/store/apps/details?id=com.tristanwiley.chatse")
                    sendIntent.type = "text/plain"
                    startActivity(sendIntent)
                }
        ))

        // Changelog
        headerArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                R.drawable.ic_log_black_24dp,
                getString(R.string.about_app_header_changelog),
                View.OnClickListener {
                    AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_SO))
                            .setTitle(getString(R.string.about_app_header_changelog))
                            .setView(R.layout.fragment_changelog)
                            .setPositiveButton("Close",
                                    { dialog, _ -> dialog.dismiss() }
                            )
                            .create()
                            .show()
                }
        ))

        // Contact us
        headerArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                R.drawable.ic_feedback_black_24dp,
                getString(R.string.about_app_header_contact_us),
                View.OnClickListener {
                    val emailIntent = Intent(android.content.Intent.ACTION_SEND)
                    emailIntent.type = "plain/text"
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("tristan@tristanwiley.com"))
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "ChatSE")
                    startActivity(Intent.createChooser(emailIntent, "Send mail..."))
                }
        ))

        // Donate
        headerArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                R.drawable.ic_favorite_black_24dp,
                getString(R.string.buy_coffee),
                View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/A44341P5"))) }
        ))

        rvHeader.adapter = com.tristanwiley.chatse.about.adapters.AboutIconAdapter(this, headerArray)

        // Devs RV
        rvDevs = findViewById(R.id.rv_about_devs)
        rvDevs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val devList: ArrayList<com.tristanwiley.chatse.about.pokos.DevPoko> = ArrayList()

        val tristanPoko = com.tristanwiley.chatse.about.pokos.DevPoko(
                "Tristan Wiley",
                "Full stack developer",
                "A lover of Android development and code of all stacks. Currently pursuing a Computer Science degree at The University at Buffalo.",
                R.drawable.dev_tristan,
                generateAboutIconArray("https://github.com/TristanWiley", "http://tristanwiley.com/", "https://stackoverflow.com/users/1064310/tristan-wiley", "tristan@tristanwiley.com", "https://twitter.com/lesirhype")
        )

        val maukerPoko = com.tristanwiley.chatse.about.pokos.DevPoko(
                "Mauricio Pessoa",
                "Software Engineer",
                "Computer Science Master's Student on UFMA and a tech lover. Sometimes I also grab my Nikon and go out for some pictures.\n" +
                        "\n" +
                        "I'm mostly an Android developer, but I also have other unhealthy obsessions.",
                R.drawable.dev_mauker,
                generateAboutIconArray("https://github.com/mauker1", "", "https://stackoverflow.com/users/4070469/mauker", "mauricio.ufma@gmail.com", "https://twitter.com/mauker")
        )

        val anoobianPoko = com.tristanwiley.chatse.about.pokos.DevPoko(
                "Shreyas",
                "Full stack developer",
                "Developer of the base application, got too busy with life <3.",
                R.drawable.dev_anoobian,
                generateAboutIconArray("https://github.com/AnubianN00b", "", "", "", "")
        )

        val adamPoko = com.tristanwiley.chatse.about.pokos.DevPoko(
                "Adam McNeilly",
                "Full stack Android developer",
                "Android developer at HelloWorld, and author of @androidessence. Also an organizer for @GrizzHacks.",
                R.drawable.dev_adam,
                generateAboutIconArray("https://github.com/AdamMc331", "http://adammcneilly.com/", "https://stackoverflow.com/users/3131147/adammc331", "amcneilly331@gmail.com", "https://twitter.com/adammc331")
        )

        val ericPoko = com.tristanwiley.chatse.about.pokos.DevPoko(
                "Eric Cugota",
                "Android developer",
                "...",
                R.drawable.dev_eric,
                generateAboutIconArray("https://github.com/tryadelion", "http://cugotaeric.wixsite.com/", "https://stackoverflow.com/users/4763177/cpteric", "", "https://twitter.com/lesirhype")
        )



        devList.add(tristanPoko)
        devList.add(maukerPoko)
        devList.add(anoobianPoko)
        devList.add(adamPoko)
        devList.add(ericPoko)

        rvDevs.adapter = com.tristanwiley.chatse.about.adapters.AboutDevCardAdapter(this, devList)
    }

    /**
     * Function to create clickable icons in the About cards
     */
    fun generateAboutIconArray(githubURL: String, websiteURL: String, stackoverflowURL: String, emailAddress: String, twitterURL: String): ArrayList<com.tristanwiley.chatse.about.pokos.AboutIconPoko> {
        val iconArray = ArrayList<com.tristanwiley.chatse.about.pokos.AboutIconPoko>()

        if (!githubURL.isBlank()) {
            // Github
            iconArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                    R.drawable.ic_github_circle,
                    "Github",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubURL))) }
            ))
        }

        if (!websiteURL.isBlank()) {
            // Website
            iconArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                    R.drawable.ic_web_black_24dp,
                    "Website",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteURL))) }
            ))
        }

        if (!stackoverflowURL.isBlank()) {
            // Website
            iconArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                    R.drawable.ic_web_black_24dp,
                    "StackOverflow",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(stackoverflowURL))) }
            ))
        }

        if (!emailAddress.isBlank()) {
            // Email
            iconArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                    R.drawable.ic_email_black_24dp,
                    "Email",
                    View.OnClickListener {
                        val emailIntent = Intent(android.content.Intent.ACTION_SEND)
                        emailIntent.type = "plain/text"
                        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "ChatSE")
                        startActivity(Intent.createChooser(emailIntent, "Send mail..."))
                    }
            ))
        }

        if (!twitterURL.isBlank()) {
            // Twitter
            iconArray.add(com.tristanwiley.chatse.about.pokos.AboutIconPoko(
                    R.drawable.ic_twitter_circle,
                    "Twitter",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(twitterURL))) }
            ))
        }
        return iconArray
    }
}