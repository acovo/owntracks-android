package org.owntracks.android.ui.map.baidu

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.location.LatLng as OwnTracksLatLng
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.ui.map.MapFragment
import org.owntracks.android.ui.map.MapLayerStyle

internal class BaiduMapFragment(
    preferences: org.owntracks.android.preferences.Preferences,
    contactImageBindingAdapter: ContactImageBindingAdapter
) : MapFragment<ViewDataBinding>(contactImageBindingAdapter, preferences) {

  private lateinit var baiduMap: BaiduMap
  private lateinit var mapView: MapView
  private val markers = mutableMapOf<String, Marker>()
  private val regions = mutableMapOf<Long, Overlay>()

  override val layout: Int = R.layout.fragment_map_baidu

  override fun updateCamera(latLng: OwnTracksLatLng) {
    val baiduLatLng = LatLng(latLng.latitude.value, latLng.longitude.value)
    val builder = MapStatus.Builder()
    builder.target(baiduLatLng)
    baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
  }

  override fun updateMarkerOnMap(id: String, latLng: OwnTracksLatLng, image: Bitmap) {
    val baiduLatLng = LatLng(latLng.latitude.value, latLng.longitude.value)
    val markerOptions =
        MarkerOptions()
            .position(baiduLatLng)
            .icon(BitmapDescriptorFactory.fromBitmap(image))
            .zIndex(9)
            .draggable(false)

    val existingMarker = markers[id]
    if (existingMarker != null) {
      existingMarker.position = baiduLatLng
      existingMarker.icon = BitmapDescriptorFactory.fromBitmap(image)
    } else {
      val newMarker = baiduMap.addOverlay(markerOptions) as Marker
      markers[id] = newMarker
    }
  }

  override fun removeMarkerFromMap(id: String) {
    markers.remove(id)?.remove()
  }

  override fun currentMarkersOnMap(): Set<String> {
    return markers.keys
  }

  override fun initMap() {
    mapView = binding.root.findViewById(R.id.baiduMapView)
    baiduMap = mapView.map

    // 设置地图默认状态
    baiduMap.uiSettings.isCompassEnabled = true
    baiduMap.uiSettings.isScrollGesturesEnabled = true
    baiduMap.uiSettings.isZoomGesturesEnabled = true
    baiduMap.uiSettings.isRotateGesturesEnabled = true
    baiduMap.uiSettings.isOverlookingGesturesEnabled = true

    // 设置默认地图类型
    setMapLayerType(MapLayerStyle.BaiduMapNormal)

    // 设置点击事件
    baiduMap.setOnMarkerClickListener { marker ->
      markers.entries.find { entry -> entry.value == marker }?.key?.let { onMarkerClicked(it) }
      true
    }

    baiduMap.setOnMapClickListener(
        object : BaiduMap.OnMapClickListener {
          override fun onMapClick(p0: LatLng?) {
            onMapClick()
          }

          override fun onMapPoiClick(p0: MapPoi?) {
            // 空实现
          }
        })
  }

  override fun reDrawRegions(regions: Set<WaypointModel>) {
    // 清除所有现有区域
    this.regions.values.forEach { it.remove() }
    this.regions.clear()

    // 添加新区域
    regions.forEach { addRegion(it) }
  }

  override fun addRegion(waypoint: WaypointModel) {
    val center = LatLng(waypoint.geofenceLatitude.value, waypoint.geofenceLongitude.value)
    val circleOptions =
        CircleOptions().center(center).radius(waypoint.geofenceRadius).fillColor(0x4000FF00)

    val overlay = baiduMap.addOverlay(circleOptions)
    overlay?.let { regions[waypoint.id] = overlay }
  }

  override fun deleteRegion(waypoint: WaypointModel) {
    regions.remove(waypoint.id)?.remove()
  }

  override fun updateRegion(waypoint: WaypointModel) {
    deleteRegion(waypoint)
    addRegion(waypoint)
  }

  override fun setMapLayerType(mapLayerStyle: MapLayerStyle) {
    when (mapLayerStyle) {
      MapLayerStyle.BaiduMapNormal -> baiduMap.mapType = BaiduMap.MAP_TYPE_NORMAL
      MapLayerStyle.BaiduMapSatellite -> baiduMap.mapType = BaiduMap.MAP_TYPE_SATELLITE
      MapLayerStyle.BaiduMapHybrid -> {
        // 根据百度地图SDK 7.6.4版本，混合模式需要通过开启卫星地图并显示路网来实现
        baiduMap.mapType = BaiduMap.MAP_TYPE_SATELLITE
        baiduMap.isTrafficEnabled = false // 关闭交通图层
        baiduMap.isBaiduHeatMapEnabled = false // 关闭热力图
      }
      else -> baiduMap.mapType = BaiduMap.MAP_TYPE_NORMAL
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initMap()
  }

  override fun onResume() {
    super.onResume()
    mapView.onResume()
  }

  override fun onPause() {
    super.onPause()
    mapView.onPause()
  }

  override fun onDestroy() {
    super.onDestroy()
    mapView.onDestroy()
  }
}
