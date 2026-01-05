package org.owntracks.android.net.mqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import timber.log.Timber

class MQTTPingAlarmReceiver(
    private val mqttClient: MqttAsyncClient,
    private val keepaliveCounter: KeepaliveCounter
) : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Timber.v("alarm received with requestCode ${intent.getIntExtra("requestCode", -1)}")
    mqttClient.checkPing(null, null)
    // Increment keepalive counter after sending ping
    keepaliveCounter.incrementKeepaliveCounter()
  }
}
