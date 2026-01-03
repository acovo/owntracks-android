package org.owntracks.android.ui.map

import androidx.databinding.ViewDataBinding
import org.owntracks.android.R
import org.owntracks.android.preferences.types.FromConfiguration
import org.owntracks.android.ui.map.baidu.BaiduMapFragment
import org.owntracks.android.ui.map.osm.OSMMapFragment

enum class MapLayerStyle {
  OpenStreetMapNormal,
  OpenStreetMapWikimedia,
  BaiduMapNormal,
  BaiduMapSatellite,
  BaiduMapHybrid;

  fun isSameProviderAs(mapLayerStyle: MapLayerStyle): Boolean {
    return setOf("OpenStreetMap", "BaiduMap").any {
      name.startsWith(it) && mapLayerStyle.name.startsWith(it)
    }
  }

  fun getFragmentClass(): Class<out MapFragment<out ViewDataBinding>> {
    return when (this) {
      OpenStreetMapNormal,
      OpenStreetMapWikimedia -> OSMMapFragment::class.java
      BaiduMapNormal,
      BaiduMapSatellite,
      BaiduMapHybrid -> BaiduMapFragment::class.java
    }
  }

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MapLayerStyle =
        MapLayerStyle.values().firstOrNull { it.name.equals(value, true) } ?: OpenStreetMapNormal
  }
}

val mapLayerSelectorButtonsToStyles =
    mapOf(
        R.id.fabMapLayerBaiduMapNormal to MapLayerStyle.BaiduMapNormal,
        R.id.fabMapLayerBaiduMapSatellite to MapLayerStyle.BaiduMapSatellite,
        R.id.fabMapLayerBaiduMapHybrid to MapLayerStyle.BaiduMapHybrid)
