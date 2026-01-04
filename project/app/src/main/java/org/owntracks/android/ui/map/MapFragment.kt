package org.owntracks.android.ui.map

import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.StrictMode
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toLatLng
import org.owntracks.android.model.Contact
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter
import timber.log.Timber

abstract class MapFragment<V : ViewDataBinding>
internal constructor(
    private val contactImageBindingAdapter: ContactImageBindingAdapter,
    preferences: Preferences
) : Fragment() {
  protected abstract val layout: Int
  protected lateinit var binding: V

  abstract fun updateCamera(latLng: LatLng)

  abstract fun updateMarkerOnMap(id: String, latLng: LatLng, image: Bitmap)

  abstract fun removeMarkerFromMap(id: String)

  abstract fun currentMarkersOnMap(): Set<String>

  /**
   * Init map is called to initialize the given fragments implementation of the map. This needs to
   * take care of initializing the specific map implementation, setting the theme, drawing the
   * waypoints and the contacts on the map.
   */
  abstract fun initMap()

  abstract fun reDrawRegions(regions: Set<WaypointModel>)

  abstract fun addRegion(waypoint: WaypointModel)

  abstract fun deleteRegion(waypoint: WaypointModel)

  abstract fun updateRegion(waypoint: WaypointModel)

  abstract fun setMapLayerType(mapLayerStyle: MapLayerStyle)

  protected fun drawAllContactsAndRegions() {
    viewModel.allContacts.values.toSet().run(::updateAllMarkers)
    //    viewModel.allWaypoints.toSet().run(::reDrawRegions)
  }

  protected val viewModel: MapViewModel by activityViewModels()

  /* Callback that's fired when a new location is observed */
  protected val onLocationObserved: (Location, () -> Unit) -> Unit =
      { location, additionalCallback ->
        preferences.ignoreInaccurateLocations.run {
          if (this > 0 && location.accuracy >= this) {
            Timber.d("Ignoring location with accuracy ${location.accuracy} >= $this")
            return@run
          }
        }
        viewModel.setCurrentBlueDotLocation(location.toLatLng())
        if (viewModel.viewMode == MapViewModel.ViewMode.Device) {
          updateCamera(location.toLatLng())
        }
        additionalCallback()
      }

  protected fun getRegionColor(): Int {
    val typedValue = TypedValue()
    requireContext().theme.resolveAttribute(R.attr.colorRegion, typedValue, true)
    return typedValue.data
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    // 临时禁用StrictMode磁盘检查，因为百度地图MapView初始化时会进行磁盘操作
    val originalPolicy = StrictMode.allowThreadDiskReads().apply {
      StrictMode.allowThreadDiskWrites()
    }
    binding = try {
      DataBindingUtil.inflate<V>(inflater, layout, container, false).apply {
        lifecycleOwner = this@MapFragment.viewLifecycleOwner
      }
    } finally {
      StrictMode.setThreadPolicy(originalPolicy)
    }

    // Here we set up all the flow collectors to react to the universe changing. Usually contacts
    // and waypoints coming and going.
    viewModel.apply {
      mapCenter.observe(viewLifecycleOwner, this@MapFragment::updateCamera)
      updateAllMarkers(allContacts.values.toSet())
      lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
          launch {
            contactUpdatedEvent.collect {
              when (it) {
                ContactsRepoChange.AllCleared -> {
                  updateAllMarkers(emptySet())
                }
                is ContactsRepoChange.ContactAdded -> {
                  updateMarkerForContact(it.contact)
                }
                is ContactsRepoChange.ContactLocationUpdated -> {
                  updateMarkerForContact(it.contact)
                  if (viewMode == MapViewModel.ViewMode.Contact(true) &&
                      currentContact.value == it.contact) {
                    it.contact.latLng?.run(this@MapFragment::updateCamera)
                  }
                }
                is ContactsRepoChange.ContactCardUpdated -> {
                  updateMarkerForContact(it.contact)
                }
                is ContactsRepoChange.ContactRemoved -> {
                  removeMarkerFromMap(it.contact.id)
                }
              }
            }
          }
          launch { reDrawRegions(getAllWaypoints().toSet()) }
          launch {
            viewModel.waypointUpdatedEvent.collect {
              Timber.d("waypointUpdatedEvent $it")
              when (it) {
                is WaypointsRepo.WaypointOperation.Clear -> reDrawRegions(emptySet())
                is WaypointsRepo.WaypointOperation.Delete -> deleteRegion(it.waypoint)
                is WaypointsRepo.WaypointOperation.Insert -> addRegion(it.waypoint)
                is WaypointsRepo.WaypointOperation.Update -> updateRegion(it.waypoint)
                is WaypointsRepo.WaypointOperation.InsertMany -> it.waypoints.forEach(::addRegion)
              }
            }
          }
          // 观察当前位置变化，当位置可用时自动更新地图中心（如果是设备视图模式）
          launch {
            currentLocation.observe(viewLifecycleOwner) { location ->
              if (viewMode == MapViewModel.ViewMode.Device) {
                location?.toLatLng()?.run(this@MapFragment::updateCamera)
              }
            }
          }
        }
      }

      mapLayerStyle.observe(viewLifecycleOwner, this@MapFragment::setMapLayerType)
      onMapReady()
    }
    return binding.root
  }

  private fun updateAllMarkers(contacts: Set<Contact>) {
    currentMarkersOnMap().subtract(contacts.map { it.id }.toSet()).forEach(::removeMarkerFromMap)
    contacts.forEach(::updateMarkerForContact)
  }

  private fun updateMarkerForContact(contact: Contact) {
    if (contact.latLng == null) {
      Timber.w("unable to update marker for $contact. no location")
      return
    }
    Timber.v("updating marker for contact: ${contact.id}")
    lifecycleScope.launch {
      contactImageBindingAdapter.run {
        updateMarkerOnMap(contact.id, contact.latLng!!, getBitmapFromCache(contact))
      }
      if (contact == viewModel.currentContact.value) {
        viewModel.refreshGeocodeForContact(contact)
      }
    }
  }

  fun onMapClick() {
    viewModel.onMapClick()
  }

  fun onMarkerClicked(id: String) {
    viewModel.onMarkerClick(id)
  }
}
