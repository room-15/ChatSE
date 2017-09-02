package me.shreyasr.chatse.about

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import me.shreyasr.chatse.BuildConfig
import me.shreyasr.chatse.R
import me.shreyasr.chatse.about.adapters.AboutDevCardAdapter
import me.shreyasr.chatse.about.adapters.AboutIconAdapter
import me.shreyasr.chatse.about.pokos.AboutIconPoko
import me.shreyasr.chatse.about.pokos.DevPoko


class AboutActivity : AppCompatActivity() {

    lateinit var tvHeaderAppAbout: TextView
    lateinit var tvHeaderVersion : TextView
    lateinit var rvHeader : RecyclerView
    lateinit var rvDevs : RecyclerView

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
                    collapsingToolbarLayout.title = " "//carefull there should a space between double quote otherwise it wont work
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

        val headerArray = ArrayList<AboutIconPoko>()
        // Rate app
        headerArray.add(AboutIconPoko(
                R.drawable.ic_star_black,
                getString(R.string.about_app_header_share),
                View.OnClickListener { Toast.makeText(this, "Rated!", Toast.LENGTH_LONG).show() }
            )
        )
        // Play Store
        headerArray.add(AboutIconPoko(
                R.drawable.ic_google_play,
                getString(R.string.about_app_header_play_store),
                View.OnClickListener { /* TODO */ }
            )
        )
        // Share app
        headerArray.add(AboutIconPoko(
                R.drawable.ic_share_black_24dp,
                getString(R.string.about_app_header_share),
                View.OnClickListener { /* TODO */ }
            )
        )
        // Changelog
        headerArray.add(AboutIconPoko(
                R.drawable.ic_log_black_24dp,
                getString(R.string.about_app_header_changelog),
                View.OnClickListener { /* TODO */ }
            )
        )
        // Contact us
        headerArray.add(AboutIconPoko(
                R.drawable.ic_feedback_black_24dp,
                getString(R.string.about_app_header_contact_us),
                View.OnClickListener { /* TODO */ }
            )
        )
        // Report bug
        headerArray.add(AboutIconPoko(
                R.drawable.ic_bug_report_black_24dp,
                getString(R.string.about_app_header_bug_report),
                View.OnClickListener { /* TODO */ }
            )
        )
        // Donate
        headerArray.add(AboutIconPoko(
                R.drawable.ic_favorite_black_24dp,
                getString(R.string.about_app_header_share),
                View.OnClickListener { /* TODO */ }
            )
        )

        rvHeader.adapter = AboutIconAdapter(this, headerArray)

        // Devs RV
        rvDevs = findViewById(R.id.rv_about_devs)
        rvDevs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val maukerArray = ArrayList<AboutIconPoko>()

        // Twitter
        maukerArray.add(AboutIconPoko(
                R.drawable.ic_twitter_circle,
                "Twitter",
                View.OnClickListener { Toast.makeText(this, "Clicked!", Toast.LENGTH_LONG).show() }
            )
        )

        // Instagram
        maukerArray.add(AboutIconPoko(
                R.drawable.ic_instagram,
                "Instagram",
                View.OnClickListener { Toast.makeText(this, "Clicked!", Toast.LENGTH_LONG).show() }
            )
        )

        // Github
        maukerArray.add(AboutIconPoko(
                R.drawable.ic_github_circle,
                "Github",
                View.OnClickListener { Toast.makeText(this, "Clicked!", Toast.LENGTH_LONG).show() }
            )
        )

        // Website
        maukerArray.add(AboutIconPoko(
                R.drawable.ic_web_black_24dp,
                "Website",
                View.OnClickListener { Toast.makeText(this, "Clicked!", Toast.LENGTH_LONG).show() }
            )
        )

        // Email
        maukerArray.add(AboutIconPoko(
                R.drawable.ic_email_black_24dp,
                "Email",
                View.OnClickListener { Toast.makeText(this, "Clicked!", Toast.LENGTH_LONG).show() }
            )
        )

        val devList: ArrayList<DevPoko> = ArrayList()

        val maukerPoko: DevPoko = DevPoko(
                "Mauricio Pessoa",
                "Software Engineer",
                "Computer Science Master's Student on UFMA and a tech lover. Sometimes I also grab my Nikon and go out for some pictures.\n" +
                        "\n" +
                        "I'm mostly an Android developer, but I also have other unhealthy obsessions.",
                R.drawable.dev_mauker,
                maukerArray
        )

        val tristanPoko: DevPoko = DevPoko(
                "Tristan Wiley",
                "Full stack developer",
                "I love Kotlin <3 Android development",
                R.drawable.dev_tristan,
                maukerArray
        )

        val anoobianPoko: DevPoko = DevPoko(
                "Shreyas",
                "Full stack developer",
                "Developer of the base application, got too busy.",
                R.drawable.dev_anoobian,
                maukerArray
        )

        val adamPoko: DevPoko = DevPoko(
                "Adam McNeilly",
                "Full stack Android developer",
                "Android developer at HelloWorld, and author of @androidessence. Also an organizer for @GrizzHacks.",
                R.drawable.dev_adam,
                maukerArray
        )

        val ericPoko: DevPoko = DevPoko(
                "Eric Cugota",
                "Android developer",
                "...",
                R.drawable.dev_eric,
                maukerArray
        )



        devList.add(tristanPoko)
        devList.add(maukerPoko)
        devList.add(anoobianPoko)
        devList.add(adamPoko)
        devList.add(ericPoko)

        rvDevs.adapter = AboutDevCardAdapter(this, devList)
    }
}
