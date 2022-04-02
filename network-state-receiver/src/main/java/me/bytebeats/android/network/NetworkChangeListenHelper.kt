package me.bytebeats.android.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2022/4/2 12:06
 * @Version 1.0
 * @Description TO-DO
 */

/**
 * 网络连接变化 监听帮助类
 *
 *
 * 说明：
 * 1、静态注册广播监听网络变化 的方式，[ConnectivityManager.CONNECTIVITY_ACTION]已有说明，
 * 7.0及以后 静态注册的接收器不会收到 CONNECTIVITY_ACTION，只能用动态注册。（这是官方对广播权限的限制）
 * 2、5.0后有新的api[ConnectivityManager.NetworkCallback] ,但是只能在app 存活时监听到。和动态注册效果类似，但有更多细节的回调。
 *
 *
 * 综合这两点，本类实现方案：7.0及以后使用新api，只能在app存活时接收到回调；7.0以前使用静态注册广播。
 */
class NetworkChangeListenHelper {
    fun hasRegisterNetworkCallback(): Boolean {
        return mNetworkChangeListener != null
    }

    @SuppressLint("LongLogTag")
    fun registerNetworkCallback(context: Context, networkChangeListener: NetworkChangeListener?) {
        if (hasRegisterNetworkCallback()) {
            Log.d(TAG, "hasRegisterNetworkCallback")
            return
        }
        mNetworkChangeListener = networkChangeListener

        //7.0及以后 使用这个新的api（7.0以前还是用静态注册广播）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            // 请注意这里会有一个版本适配bug，所以请在这里添加非空判断
            val request = NetworkRequest.Builder().build()
            try {
//      https://issuetracker.google.com/issues/175055271
                connectivityManager?.registerNetworkCallback(request, AkuNetworkCallback())
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }
        }
    }

    private fun handleOnNetworkChange(networkState: Int) {
        when (networkState) {
            NETWORK_STATE_UNAVAILABLE -> mNetworkChangeListener?.onNetworkChange(false)
            NETWORK_STATE_AVAILABLE -> mNetworkChangeListener?.onNetworkChange(true)
//      NETWORK_STATE_AVAILABLE_WIFI -> mNetworkChangeListener?.onNetworkChange(true)
//      NETWORK_STATE_AVAILABLE_MOBILE -> mNetworkChangeListener?.onNetworkChange(true)
            else -> {
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    inner class AkuNetworkCallback : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "网络连接了")
            handleOnNetworkChange(NETWORK_STATE_AVAILABLE)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "网络断开了")
            handleOnNetworkChange(NETWORK_STATE_UNAVAILABLE)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            //网络变化时，这个方法会回调多次, 避免调用该方法
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.d(TAG, "wifi网络已连接")
                    handleOnNetworkChange(NETWORK_STATE_AVAILABLE_WIFI)
                } else {
                    Log.d(TAG, "移动网络已连接")
                    handleOnNetworkChange(NETWORK_STATE_AVAILABLE_MOBILE)
                }
            }
        }
    }

    class NetworkChangeBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            //7.0以下用静态广播
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return
            }
            if (intent == null) {
                return
            }
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
                return
            }
            val noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)
            mNetworkChangeListener?.onNetworkChange(!noConnectivity)
        }
    }

    /**
     * NetworkChangeListener
     */
    interface NetworkChangeListener {
        fun onNetworkChange(isNetworkAvailable: Boolean)
    }

    companion object {
        private const val TAG = "NetworkChange"

        /**
         * 网络不可用
         */
        private const val NETWORK_STATE_UNAVAILABLE = -1

        /**
         * 网络可用
         */
        private const val NETWORK_STATE_AVAILABLE = 0

        /**
         * 网络可用，且是移动数据
         */
        private const val NETWORK_STATE_AVAILABLE_MOBILE = 1

        /**
         * 网络可用，且是wifi
         */
        private const val NETWORK_STATE_AVAILABLE_WIFI = 2

        private var mNetworkChangeListener: NetworkChangeListener? = null
    }
}