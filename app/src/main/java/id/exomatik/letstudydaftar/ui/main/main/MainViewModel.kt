package id.exomatik.letstudydaftar.ui.main.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import id.exomatik.letstudydaftar.base.BaseViewModel
import id.exomatik.letstudydaftar.utils.Constant
import id.exomatik.letstudydaftar.utils.checkInternetConnection

class MainViewModel : BaseViewModel() {

    fun getInfoApps(web: WebView, context: Context?, activity: Activity?) {
        isShowLoading.value = true

        val valueEventListener = object : ValueEventListener {
            override fun onCancelled(result: DatabaseError) {
                isShowLoading.value = false
                message.value = "Gagal mengambil masuk ke aplikasi"
            }

            override fun onDataChange(result: DataSnapshot) {
                isShowLoading.value = false
                if (result.exists()) {
                    val url = result.getValue(String::class.java)

                    if (!url.isNullOrEmpty()){
                        setUpWebView(web, context, activity, url)
                    }
                    else{
                        message.value = "Gagal mengambil masuk ke aplikasi"
                    }
                }
                else{
                    message.value = "Gagal mengambil masuk ke aplikasi"
                }
            }
        }

        FirebaseDatabase.getInstance()
            .getReference(Constant.referenceUrl)
            .addListenerForSingleValueEvent(valueEventListener)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView(web: WebView, context: Context?, activity: Activity?, urlWeb: String){
        cekKoneksi(context, web, activity, urlWeb)

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isShowLoading.value = true
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isShowLoading.value = false
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                webView: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String) {
                try {
                    webView.stopLoading()
                } catch (e: Exception) { }

                cekKoneksi(context, web, activity, urlWeb)
            }
        }

        web.settings.loadsImagesAutomatically = true
        web.settings.javaScriptEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.settings.domStorageEnabled = true

        web.settings.setSupportZoom(true)
        web.settings.displayZoomControls = true

        web.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
    }

    private fun cekKoneksi(context: Context?, web: WebView, activity: Activity?, urlWeb: String){
        if(checkInternetConnection(context)){
            web.loadUrl(urlWeb)
        } else{
            val alertDialog = AlertDialog.Builder(context?:throw Exception("Unknown Error")).create()
            alertDialog.setTitle("Error")
            alertDialog.setMessage("Check your internet connection and try again.")
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Try Again") { dialog, _ ->
                dialog.dismiss()
                web.loadUrl(urlWeb)
            }
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Close") { dialog, _ ->
                dialog.dismiss()
                activity?.finish()
            }

            alertDialog.show()
        }
    }
}