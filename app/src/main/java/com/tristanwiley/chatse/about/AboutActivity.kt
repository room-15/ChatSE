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
import com.tristanwiley.chatse.about.adapters.AboutDevCardAdapter
import com.tristanwiley.chatse.about.adapters.AboutIconAdapter
import com.tristanwiley.chatse.about.pokos.AboutIconPoko
import com.tristanwiley.chatse.about.pokos.DevPoko


class AboutActivity : AppCompatActivity() {

    private lateinit var tvHeaderAppAbout: TextView
    private lateinit var tvHeaderVersion: TextView
    private lateinit var rvHeader: RecyclerView
    private lateinit var rvDevs: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        getUiElements()

        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        val appBarLayout = findViewById<AppBarLayout>(R.id.about_appbar_layout)
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            var isShow = false
            var scrollRange = -1

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

    private fun getUiElements() {
        tvHeaderVersion = findViewById(R.id.about_text_app_version)
        tvHeaderAppAbout = findViewById(R.id.about_text_app)

        tvHeaderVersion.text = String.format(
                getString(R.string.about_version), getString(R.string.app_name), BuildConfig.VERSION_NAME)
        tvHeaderAppAbout.text = getString(R.string.about_app)

        // Setup RecyclerView
        rvHeader = findViewById(R.id.about_rv_header_icons)

        rvHeader.layoutManager =
                GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)

        val headerArray = ArrayList<AboutIconPoko>()
        // Rate app
        headerArray.add(AboutIconPoko(
                R.drawable.ic_star_black,
                getString(R.string.rate_app),
                View.OnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.tristanwiley.chatse")))
                }))

        // Play Store
        headerArray.add(AboutIconPoko(
                R.drawable.ic_google_play,
                getString(R.string.about_app_header_play_store),
                View.OnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.tristanwiley.chatse")))
                }
        ))

        // Share app
        headerArray.add(AboutIconPoko(
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
        headerArray.add(AboutIconPoko(
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
        headerArray.add(AboutIconPoko(
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
        headerArray.add(AboutIconPoko(
                R.drawable.ic_favorite_black_24dp,
                getString(R.string.buy_coffee),
                View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/A44341P5"))) }
        ))

        rvHeader.adapter = AboutIconAdapter(this, headerArray)

        // Devs RV
        rvDevs = findViewById(R.id.rv_about_devs)
        rvDevs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val devList: ArrayList<DevPoko> = ArrayList()

        val tristanPoko = DevPoko(
                "Tristan Wiley",
                "Full stack developer",
                "A lover of Android development, Kotlin and all stacks. Currently pursuing a Computer Science degree at The University at Buffalo.",
                R.drawable.dev_tristan,
                generateAboutIconArray("https://github.com/TristanWiley", "http://tristanwiley.com/", "https://stackoverflow.com/users/1064310/tristan-wiley", "tristan@tristanwiley.com", "https://twitter.com/lesirhype")
        )

        val maukerPoko = DevPoko(
                "Mauricio Pessoa",
                "Android engineer",
                "MSc. in Computer Science and a tech lover. Sometimes I also grab my Nikon and go out for some pictures.\n" +
                        "\n" +
                        "Love all things Android since the first Donut bite.",
                R.drawable.dev_mauker,
                generateAboutIconArray("https://github.com/mauker1", "", "https://stackoverflow.com/users/4070469/mauker", "mauricio.ufma@gmail.com", "https://twitter.com/mauker")
        )

        val anoobianPoko = DevPoko(
                "Shreyas",
                "Full stack developer",
                "Developer of the base application, got too busy with life <3.",
                R.drawable.dev_anoobian,
                generateAboutIconArray("https://github.com/AnubianN00b", "", "", "", "")
        )

        val adamPoko = DevPoko(
                "Adam McNeilly",
                "Full stack Android engineer",
                "Android engineer at OkCupid, and author of @androidessence. Also an organizer for @GrizzHacks.",
                R.drawable.dev_adam,
                generateAboutIconArray("https://github.com/AdamMc331", "http://adammcneilly.com/", "https://stackoverflow.com/users/3131147/adammc331", "amcneilly331@gmail.com", "https://twitter.com/adammc331")
        )

        val ericPoko = DevPoko(
                "Eric Cugota",
                "Mobile developer at Useit",
                "Rugby aficionado, couch surfer.",
                R.drawable.dev_eric,
                generateAboutIconArray("https://github.com/tryadelion", "http://cugotaeric.wixsite.com/", "https://stackoverflow.com/users/4763177/cpteric", "", "https://twitter.com/lesirhype")
        )

        val nabahPoko = DevPoko(
                "Nabah Rizvi",
                "Aspiring UI Designer & Engineer",
                "Junior majoring in Information Technology with a minor in Digital Art & Design at the University of Toledo.",
                R.drawable.dev_nabah,
                generateAboutIconArray("https://github.com/nrizvi", "http://www.nrizvi.me/", "", "", "")
        )

        devList.add(tristanPoko)
        devList.add(maukerPoko)
        devList.add(anoobianPoko)
        devList.add(adamPoko)
        devList.add(ericPoko)
        devList.add(nabahPoko)

        rvDevs.adapter = AboutDevCardAdapter(this, devList)
    }

    /**
     * Function to create clickable icons in the About cards
     */
    private fun generateAboutIconArray(githubURL: String, websiteURL: String, stackoverflowURL: String, emailAddress: String, twitterURL: String): ArrayList<AboutIconPoko> {
        val iconArray = ArrayList<AboutIconPoko>()

        if (!githubURL.isBlank()) {
            // Github
            iconArray.add(AboutIconPoko(
                    R.drawable.ic_github_circle,
                    "Github",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubURL))) }
            ))
        }

        if (!websiteURL.isBlank()) {
            // Website
            iconArray.add(AboutIconPoko(
                    R.drawable.ic_web_black_24dp,
                    "Website",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteURL))) }
            ))
        }

        if (!stackoverflowURL.isBlank()) {
            // Website
            iconArray.add(AboutIconPoko(
                    R.drawable.ic_web_black_24dp,
                    "StackOverflow",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(stackoverflowURL))) }
            ))
        }

        if (!emailAddress.isBlank()) {
            // Email
            iconArray.add(AboutIconPoko(
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
            iconArray.add(AboutIconPoko(
                    R.drawable.ic_twitter_circle,
                    "Twitter",
                    View.OnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(twitterURL))) }
            ))
        }
        return iconArray
    }
}