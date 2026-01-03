package org.owntracks.android

import dagger.hilt.android.HiltAndroidApp
import org.owntracks.android.support.receiver.StartBackgroundServiceReceiver

@HiltAndroidApp
class App : BaseApp() {
  override fun onCreate() {
    super.onCreate()
    StartBackgroundServiceReceiver.enable(this)
  }
}
