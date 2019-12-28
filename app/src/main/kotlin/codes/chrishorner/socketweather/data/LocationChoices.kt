package codes.chrishorner.socketweather.data

import android.app.Application
import android.content.Context
import codes.chrishorner.socketweather.util.getOrCreateFile
import codes.chrishorner.socketweather.util.readSet
import codes.chrishorner.socketweather.util.readValue
import codes.chrishorner.socketweather.util.writeSet
import codes.chrishorner.socketweather.util.writeValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext
import java.io.File

class LocationChoices private constructor(app: Application) {

  private val savedSelectionsChannel = ConflatedBroadcastChannel<Set<LocationSelection>>()
  private val currentSelectionChannel = ConflatedBroadcastChannel<LocationSelection>()
  private val savedSelections: File
  private val currentSelection: File

  init {
    val directory = app.getDir("location_choices", Context.MODE_PRIVATE)
    savedSelections = getOrCreateFile(directory, "saved_selections")
    currentSelection = getOrCreateFile(directory, "current_selection")

    val savedLocations: Set<LocationSelection> = savedSelections.readSet()
    savedSelectionsChannel.offer(savedLocations)

    val locationSelection: LocationSelection? = currentSelection.readValue()
    currentSelectionChannel.offer(locationSelection ?: LocationSelection.None)
  }

  fun observeSavedSelections(): Flow<Set<LocationSelection>> = savedSelectionsChannel.asFlow()

  fun observeCurrentSelection(): Flow<LocationSelection> = currentSelectionChannel.asFlow()

  fun getSavedSelections(): Set<LocationSelection> = savedSelectionsChannel.value

  fun getCurrentSelection(): LocationSelection = currentSelectionChannel.value

  fun hasFollowMeSaved(): Boolean = getSavedSelections().contains(LocationSelection.FollowMe)

  suspend fun saveAndSelect(selection: LocationSelection) {
    withContext(Dispatchers.IO) {
      val selections = savedSelections.readSet<LocationSelection>() + selection
      savedSelections.writeSet(selections)
      currentSelection.writeValue(selection)
      savedSelectionsChannel.offer(selections)
      currentSelectionChannel.offer(selection)
    }
  }

  suspend fun select(selection: LocationSelection) {
    withContext(Dispatchers.IO) {
      savedSelections.writeValue(selection)
      currentSelectionChannel.offer(selection)
    }
  }

  fun clear() {
    savedSelections.delete()
    currentSelection.delete()
  }

  companion object {
    private var instance: LocationChoices? = null

    fun init(app: Application) {
      instance = LocationChoices(app)
    }

    fun get(): LocationChoices {
      return requireNotNull(instance) { "LocationChoices.init(app) must be called first." }
    }
  }
}