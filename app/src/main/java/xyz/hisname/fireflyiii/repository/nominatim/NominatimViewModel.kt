package xyz.hisname.fireflyiii.repository.nominatim

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.data.remote.nominatim.NominatimClient
import xyz.hisname.fireflyiii.data.remote.nominatim.api.SearchService
import xyz.hisname.fireflyiii.repository.models.nominatim.LocationSearchModel

class NominatimViewModel: ViewModel() {


    fun getLocationFromQuery(location: String): LiveData<List<LocationSearchModel>>{
        val client = NominatimClient.getClient()
        var locationResult: List<LocationSearchModel>? = null
        val data: MutableLiveData<List<LocationSearchModel>> = MutableLiveData()
        viewModelScope.launch(Dispatchers.IO) {
            locationResult = client?.create(SearchService::class.java)?.searchLocation(location, "jsonv2")
        }.invokeOnCompletion {
            data.postValue(locationResult)
        }
        return data
    }
}