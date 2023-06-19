package utils.blur

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import eightbitlab.com.blurview.BlurView
import seifemadhamdy.ainion.R

class BlurUtils {
    fun initializeBlurView(
        blurView: BlurView,
        viewGroup: ViewGroup,
        frameClearDrawable: Drawable = (blurView.context as Activity).window.decorView.background,
        blurRadius: Float = 25F,
        autoUpdate: Boolean = true,
        @ColorInt overlayColor: Int = ContextCompat.getColor(
            blurView.context,
            R.color.default_blur_background
        )
    ) {
        blurView.setupWith(viewGroup)
            .setFrameClearDrawable(frameClearDrawable)
            .setBlurRadius(blurRadius)
            .setBlurAutoUpdate(autoUpdate)
            .setOverlayColor(overlayColor)
    }
}