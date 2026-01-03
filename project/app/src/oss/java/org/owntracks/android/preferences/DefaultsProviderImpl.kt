package org.owntracks.android.preferences

import kotlin.reflect.KProperty
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.ui.map.MapLayerStyle

class DefaultsProviderImpl : DefaultsProvider {
  @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
  override fun <T> getDefaultValue(preferences: Preferences, property: KProperty<*>): T {
    return when (property) {
      Preferences::mapLayerStyle -> MapLayerStyle.BaiduMapNormal
      Preferences::reverseGeocodeProvider -> ReverseGeocodeProvider.None
      Preferences::host -> org.owntracks.android.BuildConfig.MQTT_HOST
      Preferences::port -> org.owntracks.android.BuildConfig.MQTT_PORT
      Preferences::keepalive -> org.owntracks.android.BuildConfig.KEEPALIVE
      Preferences::locatorDisplacement -> org.owntracks.android.BuildConfig.LOCATOR_DISPLACEMENT
      Preferences::cmd -> org.owntracks.android.BuildConfig.CMD
      Preferences::remoteConfiguration -> org.owntracks.android.BuildConfig.REMOTE_CONFIGURATION
      else -> super.getDefaultValue<T>(preferences, property)
    }
        as T
  }
}
