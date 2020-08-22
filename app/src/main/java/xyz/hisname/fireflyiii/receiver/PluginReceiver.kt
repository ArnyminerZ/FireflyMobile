package xyz.hisname.fireflyiii.receiver

import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import xyz.hisname.fireflyiii.data.local.account.AuthenticatorManager
import xyz.hisname.fireflyiii.data.local.dao.AppDatabase
import xyz.hisname.fireflyiii.data.local.pref.AppPref
import xyz.hisname.fireflyiii.data.remote.firefly.FireflyClient
import xyz.hisname.fireflyiii.data.remote.firefly.api.TransactionService
import xyz.hisname.fireflyiii.repository.models.transaction.TransactionIndex
import xyz.hisname.fireflyiii.util.network.CustomCa
import xyz.hisname.fireflyiii.util.network.retrofitCallback

class PluginReceiver: AbstractPluginSettingReceiver(){

    private lateinit var customCa: CustomCa
    private lateinit var sharedPref: SharedPreferences
    private lateinit var accountManager: AuthenticatorManager
    private val sslSocketFactory by lazy { customCa.getCustomSSL() }
    private val trustManager by lazy { customCa.getCustomTrust() }

    private fun genericService(): Retrofit? {
        var cert = AppPref(sharedPref).certValue
        return if (AppPref(sharedPref).isCustomCa) {
            FireflyClient.getClient(AppPref(sharedPref).baseUrl,
                    accountManager.accessToken, cert, trustManager, sslSocketFactory)
        } else {
            FireflyClient.getClient(AppPref(sharedPref).baseUrl,
                    accountManager.accessToken, cert, null, null)
        }

    }


    override fun isAsync() = true

    override fun isBundleValid(bundle: Bundle) = true

    override fun firePluginSetting(context: Context, bundle: Bundle) {
        accountManager = AuthenticatorManager(AccountManager.get(context))
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        customCa = CustomCa(("file://" + context.filesDir.path + "/user_custom.pem").toUri().toFile())
        val transactionTypeBundle = bundle.getString("transactionType") ?: ""
        val transactionDescription = bundle.getString("transactionDescription") ?: ""
        val transactionAmount = bundle.getString("transactionAmount") ?: ""
        val transactionDateTime = bundle.getString("transactionDateTime") ?: ""
        val transactionPiggyBank = bundle.getString("transactionPiggyBank")
        val transactionSourceAccount = bundle.getString("transactionSourceAccount")
        val transactionDestinationAccount = bundle.getString("transactionDestinationAccount") ?: ""
        val transactionCurrency = bundle.getString("transactionCurrency") ?: ""
        val transactionTags = bundle.getString("transactionTags")
        val transactionBudget = bundle.getString("transactionBudget")
        val transactionCategory = bundle.getString("transactionCategory")
        val fileUri = bundle.getString("fileUri")

        addTransaction(context, transactionTypeBundle, transactionDescription, transactionDateTime,
                transactionPiggyBank, transactionAmount, transactionSourceAccount,
                transactionDestinationAccount, transactionCurrency, transactionCategory,
                transactionTags, transactionBudget, fileUri?.toUri())
    }

    private fun addTransaction(context: Context, type: String, description: String,
                       date: String, piggyBankName: String?, amount: String,
                       sourceName: String?, destinationName: String?, currencyName: String,
                       category: String?, tags: String?, budgetName: String?, fileUri: Uri?){
        genericService()?.create(TransactionService::class.java)?.addTransaction(convertString(type),description, date ,piggyBankName,
                amount.replace(',', '.'),sourceName,destinationName,currencyName, category, tags, budgetName)?.enqueue(retrofitCallback({ response ->
            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                runBlocking(Dispatchers.IO){
                    responseBody.data.transactionAttributes?.transactions?.forEachIndexed { _, transaction ->
                        val transactionDb = AppDatabase.getInstance(context).transactionDataDao()
                        transactionDb.insert(transaction)
                        transactionDb.insert(TransactionIndex(response.body()?.data?.transactionId,
                                transaction.transaction_journal_id))
                    }
                }
            }
        }))
    }

    private fun convertString(type: String) = type.substring(0,1).toLowerCase() + type.substring(1).toLowerCase()

}