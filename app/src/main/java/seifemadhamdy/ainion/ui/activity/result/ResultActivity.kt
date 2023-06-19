package seifemadhamdy.ainion.ui.activity.result

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import seifemadhamdy.ainion.databinding.ActivityResultBinding
import seifemadhamdy.ainion.ui.activity.upload.UploadActivity
import utils.blur.BlurUtils

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private val blurUtils by lazy { BlurUtils() }
    private var areViewsUpdatedForInsets = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityResultBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            val navigationBarHeight =
                windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            binding.statusBarBlurView.updateLayoutParamsHeight(
                statusBarHeight
            )

            binding.navigationBarBlurView.updateLayoutParamsHeight(
                navigationBarHeight
            )

            if (!areViewsUpdatedForInsets) {
                binding.nestedScrollView.updatePaddingForInsets(top = statusBarHeight)
                binding.nestedScrollView.updatePaddingForInsets(bottom = navigationBarHeight)
                areViewsUpdatedForInsets = true
            }

            binding.root.also {
                blurUtils.apply {
                    initializeBlurView(blurView = binding.statusBarBlurView, viewGroup = it)

                    initializeBlurView(
                        blurView = binding.navigationBarBlurView, viewGroup = it
                    )
                }
            }

            WindowInsetsCompat.CONSUMED
        }

        binding.goBackMaterialButton.setOnClickListener {
            navigateToUploadActivity()
        }

        intent.getStringExtra("receivedMessage")?.let {
            binding.typeTextView.text = it
        }
    }

    private fun View.updateLayoutParamsHeight(heightSize: Int) {
        updateLayoutParams<ViewGroup.LayoutParams> {
            height = heightSize
        }
    }

    private fun navigateToUploadActivity() {
        Intent(this, UploadActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
            finish()
        }
    }

    private fun View.updatePaddingForInsets(
        left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0
    ) {
        setPadding(
            paddingLeft + left, paddingTop + top, paddingRight + right, paddingBottom + bottom
        )
    }
}