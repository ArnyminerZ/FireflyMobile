package xyz.hisname.fireflyiii.repository.summary

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.json.JSONObject
import xyz.hisname.fireflyiii.data.local.pref.SimpleData
import xyz.hisname.fireflyiii.data.remote.firefly.api.SummaryService
import xyz.hisname.fireflyiii.repository.BaseViewModel
import xyz.hisname.fireflyiii.util.network.retrofitCallback

class SummaryViewModel(application: Application): BaseViewModel(application) {

    val networthValue: MutableLiveData<Double> = MutableLiveData()
    val leftToSpendValue: MutableLiveData<Double> = MutableLiveData()
    val balanceValue: MutableLiveData<Double> = MutableLiveData()

    fun getBasicSummary(startDate: String, endDate: String, currencyCode: String){
            val simpleData = SimpleData(PreferenceManager.getDefaultSharedPreferences(getApplication()))
            val summaryService = genericService()?.create(SummaryService::class.java)
            summaryService?.getBasicSummary(startDate, endDate,
                    currencyCode)?.enqueue(retrofitCallback({ response ->
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    // so dirty I went to take a shower after writing this code
                    val netWorth = JSONObject(responseBody)
                            .getJSONObject("net-worth-in-$currencyCode")
                            .getDouble("monetary_value")
                    simpleData.networthValue = netWorth
                    val leftToSpend =  JSONObject(responseBody)
                            .getJSONObject("left-to-spend-in-$currencyCode")
                            .getDouble("monetary_value")
                    simpleData.leftToSpend = leftToSpend
                    val balance = JSONObject(responseBody)
                            .getJSONObject("balance-in-$currencyCode")
                            .getDouble("monetary_value")
                    simpleData.balance = balance
                    leftToSpendValue.postValue(leftToSpend)
                    networthValue.postValue(netWorth)
                    balanceValue.postValue(balance)
                } else {
                    networthValue.postValue(simpleData.networthValue)
                    leftToSpendValue.postValue(simpleData.leftToSpend)
                    balanceValue.postValue(simpleData.balance)
                }
            }))

    }
}