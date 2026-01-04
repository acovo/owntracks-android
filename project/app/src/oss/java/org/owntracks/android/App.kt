package org.owntracks.android

import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import org.owntracks.android.support.receiver.StartBackgroundServiceReceiver

@HiltAndroidApp
class App : BaseApp() {
  override fun onCreate() {
    super.onCreate()
    // 初始化百度地图SDK，仅在oss版本中执行
    // 临时禁用StrictMode磁盘检查，因为百度地图SDK初始化时会进行磁盘操作
    val originalPolicy = StrictMode.allowThreadDiskReads().apply {
      StrictMode.allowThreadDiskWrites()
    }
    try {
      // 先设置隐私政策同意状态
      com.baidu.mapapi.SDKInitializer.setAgreePrivacy(this, true)
      // 然后初始化SDK
      com.baidu.mapapi.SDKInitializer.initialize(this)
    } finally {
      StrictMode.setThreadPolicy(originalPolicy)
    }
    StartBackgroundServiceReceiver.enable(this)
  }
}
