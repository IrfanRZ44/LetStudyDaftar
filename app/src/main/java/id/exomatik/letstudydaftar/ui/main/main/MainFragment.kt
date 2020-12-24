package id.exomatik.letstudydaftar.ui.main.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import id.exomatik.letstudydaftar.R
import id.exomatik.letstudydaftar.base.BaseFragmentBind
import id.exomatik.letstudydaftar.databinding.FragmentMainBinding
import id.exomatik.letstudydaftar.utils.Constant
import java.io.File
import java.io.IOException

class MainFragment : BaseFragmentBind<FragmentMainBinding>(){
    override fun getLayoutResource(): Int = R.layout.fragment_main
    private lateinit var viewModel: MainViewModel
    private var mUploadMessage: ValueCallback<Uri>? = null
    private var mCapturedImageURI: Uri? = null

    // the same for Android 5.0 methods only
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null

    override fun myCodeHere() {
        bind.lifecycleOwner = this
        viewModel = MainViewModel()
        bind.viewModel = viewModel

        viewModel.getInfoApps(bind.web, context, activity)
        setUpWebChromeClient()

        setUpAdmob()
    }

    private fun setUpAdmob(){
        MobileAds.initialize(context) {}

        val adRequest = AdRequest.Builder().build()
        bind.adView.loadAd(adRequest)
    }

    private fun setUpWebChromeClient(){
        val act : Activity? = activity
        bind.web.webChromeClient = object : WebChromeClient() {
            // for Lollipop, all in one
            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback?.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (act?.packageManager?.let { takePictureIntent!!.resolveActivity(it) } != null) {

                    // create the file where the photo should go
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent?.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        viewModel.message.value = "Unable to create Image File"
                    }

                    // continue only if the file was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent?.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        takePictureIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"
                val intentArray = takePictureIntent?.let { arrayOf(it) }
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, Constant.codeRequestGallery)
                return true
            }

            // creating image files (Lollipop only)
            @Suppress("DEPRECATION")
            @Throws(IOException::class)
            private fun createImageFile(): File? {
                var imageStorageDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "DirectoryNameHere"
                )
                if (!imageStorageDir.exists()) {
                    imageStorageDir.mkdirs()
                }

                // create an image file name
                imageStorageDir = File(
                    "imageStorageDir ${File.separator} IMG_ ${System.currentTimeMillis()}.jpg"
                )
                return imageStorageDir
            }

            // openFileChooser for Android 3.0+
            @Suppress("DEPRECATION")
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri>?,
                acceptType: String?
            ) {
                mUploadMessage = uploadMsg
                try {
                    val imageStorageDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "DirectoryNameHere"
                    )
                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs()
                    }
                    val file = File(
                        "imageStorageDir ${File.separator} IMG_ ${System.currentTimeMillis()}.jpg"
                    )
                    mCapturedImageURI =
                        Uri.fromFile(file) // save to the private variable
                    val captureIntent =
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
                    // captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.type = "image/*"
                    val chooserIntent =
                        Intent.createChooser(i, "image_chooser")
                    chooserIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        arrayOf<Parcelable>(captureIntent)
                    )
                    startActivityForResult(chooserIntent, Constant.codeRequestGallery)
                } catch (e: Exception) {
                    Toast.makeText(act, "Camera Exception:$e", Toast.LENGTH_LONG)
                        .show()
                }
            }

            // openFileChooser for Android < 3.0
            fun openFileChooser(uploadMsg: ValueCallback<Uri>?) {
                openFileChooser(uploadMsg, "")
            }

            // openFileChooser for other Android versions
            /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
               https://code.google.com/p/android/issues/detail?id=62220
               however newer versions of KitKat fixed it on some devices */
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri>?,
                acceptType: String?,
                capture: String?
            ) {
                openFileChooser(uploadMsg, acceptType)
            }


        }
    }

    // return here when file selected from camera or from SD Card
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == Constant.codeRequestGallery) {
                if (null == mUploadMessage) {
                    return
                }
                var result: Uri? = null
                try {
                    result = if (resultCode != Activity.RESULT_OK) {
                        null
                    } else {
                        // retrieve from the private variable if the intent is null
                        if (data == null) mCapturedImageURI else data.data
                    }
                } catch (e: java.lang.Exception) {
                    Toast.makeText(
                        activity,
                        "activity :$e",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        } // end of code for all versions except of Lollipop

        // start of code for Lollipop only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != Constant.codeRequestGallery || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            var results: Array<Uri>? = null

            // check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.data == null) {
                    // if there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = arrayOf(Uri.parse(mCameraPhotoPath))
                    }
                } else {
                    val dataString = data.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
            }
            mFilePathCallback!!.onReceiveValue(results)
            mFilePathCallback = null
        } // end of code for Lollipop only
    }


}