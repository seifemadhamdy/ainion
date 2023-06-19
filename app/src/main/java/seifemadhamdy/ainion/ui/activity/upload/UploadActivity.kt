package seifemadhamdy.ainion.ui.activity.upload

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import seifemadhamdy.ainion.databinding.ActivityUploadBinding
import seifemadhamdy.ainion.ui.activity.result.ResultActivity
import utils.blur.BlurUtils
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.Socket

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private val blurUtils by lazy { BlurUtils() }
    private var areViewsUpdatedForInsets = false
    private var lastKnownVideoViewPosition = 0


    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            result.data!!.data?.let {
                it.toBitmap()?.apply {
                    sendToServer()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        binding = ActivityUploadBinding.inflate(layoutInflater)
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

        prepareVideo()

        binding.uploadImageMaterialButton.setOnClickListener {
            uploadImage()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.videoView.pause()
        lastKnownVideoViewPosition = binding.videoView.currentPosition
    }

    override fun onResume() {
        super.onResume()

        if (lastKnownVideoViewPosition != 0) binding.videoView.seekTo(lastKnownVideoViewPosition)

        binding.videoView.start()
    }

    private fun View.updateLayoutParamsHeight(heightSize: Int) {
        updateLayoutParams<ViewGroup.LayoutParams> {
            height = heightSize
        }
    }

    private fun navigateToResultActivity(receivedMessage: String) {
        Intent(this, ResultActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("receivedMessage", receivedMessage)
            startActivity(this)
        }
    }

    private fun View.updatePaddingForInsets(
        left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0
    ) {
        setPadding(
            paddingLeft + left, paddingTop + top, paddingRight + right, paddingBottom + bottom
        )
    }

    private fun prepareVideo() {
        binding.videoView.also { videoView ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
            } else {
                @Suppress("DEPRECATION") (getSystemService(AUDIO_SERVICE) as AudioManager).requestAudioFocus(
                    null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
                )
            }

            videoView.setOnPreparedListener {
                it.isLooping = true
            }

            videoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + seifemadhamdy.ainion.R.raw.ainion_animation))

            MediaController(this).apply {
                setAnchorView(videoView)
                setMediaPlayer(videoView)
                videoView.setMediaController(null)
            }
        }
    }

    private fun uploadImage() {
        launcher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun Bitmap.sendToServer() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket("192.168.1.14", 7343)
                val outputStream = BufferedOutputStream(socket.getOutputStream())

                // Convert bitmap to byte array
                val byteArrayOutputStream = ByteArrayOutputStream()
                compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                // Write the byte array length to the output stream
                outputStream.write(byteArray.size shr 24 and 0xFF)
                outputStream.write(byteArray.size shr 16 and 0xFF)
                outputStream.write(byteArray.size shr 8 and 0xFF)
                outputStream.write(byteArray.size and 0xFF)

                // Write the byte array to the output stream
                outputStream.write(byteArray, 0, byteArray.size)
                outputStream.flush()

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                // Receive the message from the server
                val receivedMessage = reader.readLine()
                Log.i("Received message", receivedMessage)

                // Close the socket
                socket.close()
                navigateToResultActivity(receivedMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun Uri.toBitmap(): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use ImageDecoder for newer APIs
            try {
                val source = ImageDecoder.createSource(contentResolver, this)
                ImageDecoder.decodeBitmap(source)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            // Fallback for older APIs
            try {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, this)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}