package me.bytebeats.android.network.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.text.TextUtils
import me.bytebeats.android.network.NetworkChangeListenHelper
import me.bytebeats.android.network.NetworkChangeListenHelper.NetworkChangeListener


/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2022/4/2 12:12
 * @Version 1.0
 * @Description TO-DO
 */

class MyApplication : Application() {
    private var mNetworkChangeListenHelper: NetworkChangeListenHelper? = null

    override fun onCreate() {
        super.onCreate()
        if (isAppMainProcess()) {
            registerNetworkListener();
        }
    }

    /**
     * 监听网络变化
     */
    private fun registerNetworkListener() {
        if (mNetworkChangeListenHelper != null && mNetworkChangeListenHelper!!.hasRegisterNetworkCallback()) {
            return
        }
        mNetworkChangeListenHelper = NetworkChangeListenHelper()
        mNetworkChangeListenHelper!!.registerNetworkCallback(this, object : NetworkChangeListener {
            override fun onNetworkChange(isNetworkAvailable: Boolean) {
                // TODO: 2022/4/2 network state changed here
            }
        })
    }

    /**
     * Application.onCreate 在多进程时会被调用多次.
     * 为了使一些初始化操作只能在主进程中进行, 特使用该方法来区分进程
     * @return
     */
    private fun isAppMainProcess(): Boolean {
        return try {
            val pid: Int = Process.myPid()
            val process = getProcessNameByPid(pid)
            !TextUtils.isEmpty(process) && TextUtils.equals(packageName, process)
        } catch (e: Exception) {
            true
        }
    }

    private fun getProcessNameByPid(pid: Int): String {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (manager.runningAppProcesses == null) {
            return ""
        }
        for (runningAppProcess in manager.runningAppProcesses) {
            if (runningAppProcess.pid == pid) {
                return runningAppProcess.processName
            }
        }
        return ""
    }

}