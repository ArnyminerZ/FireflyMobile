package xyz.hisname.fireflyiii.ui.transaction.list

import androidx.paging.*
import xyz.hisname.fireflyiii.Constants
import xyz.hisname.fireflyiii.data.local.dao.TransactionDataDao
import xyz.hisname.fireflyiii.data.remote.firefly.api.TransactionService
import xyz.hisname.fireflyiii.repository.models.transaction.TransactionIndex
import xyz.hisname.fireflyiii.repository.models.transaction.Transactions
import xyz.hisname.fireflyiii.util.DateTimeUtil

class TransactionPagingSource(private val transactionService: TransactionService?,
                              private val transactionDao: TransactionDataDao,
                              private val startDate: String,
                              private val endDate: String,
                              private val transactionType: String): PagingSource<Int, Transactions>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transactions> {
        val paramKey = params.key
        val previousKey = if(paramKey != null){
            if(paramKey - 1 == 0){
                null
            } else {
                paramKey - 1
            }
        } else {
            null
        }
        try {
            val networkCall = transactionService?.getPaginatedTransactions(startDate, endDate,
                    convertString(transactionType), params.key ?: 1)
            val responseBody = networkCall?.body()
            if (responseBody != null && networkCall.isSuccessful) {
                if (params.key == 1) {
                    transactionDao.deleteTransactionsByDate(DateTimeUtil.getStartOfDayInCalendarToEpoch(startDate),
                            DateTimeUtil.getEndOfDayInCalendarToEpoch(endDate), transactionType)
                }
                responseBody.data.forEach { data ->
                    data.transactionAttributes?.transactions?.forEach { transactions ->
                        transactionDao.insert(transactions)
                    }
                    transactionDao.insert(TransactionIndex(data.transactionId,
                            data.transactionAttributes?.transactions?.get(0)?.transaction_journal_id))
                }
            }
            val pagination = responseBody?.meta?.pagination
            if(pagination != null){
                val nextKey = if(pagination.current_page < pagination.total_pages){
                    pagination.current_page + 1
                } else {
                    null
                }
                return LoadResult.Page(transactionDao.getTransactionByDate(DateTimeUtil.getStartOfDayInCalendarToEpoch(startDate),
                        DateTimeUtil.getEndOfDayInCalendarToEpoch(endDate),convertString(transactionType)),
                        previousKey, nextKey)
            } else {
                val numberOfRows = transactionDao.getTransactionByDateCount(DateTimeUtil.getStartOfDayInCalendarToEpoch(startDate),
                        DateTimeUtil.getEndOfDayInCalendarToEpoch(endDate),convertString(transactionType))
                val nextKey = if(params.key ?: 1 < (numberOfRows / Constants.PAGE_SIZE)){
                    params.key ?: 1 + 1
                } else {
                    null
                }
                return LoadResult.Page(transactionDao.getTransactionByDate(DateTimeUtil.getStartOfDayInCalendarToEpoch(startDate),
                        DateTimeUtil.getEndOfDayInCalendarToEpoch(endDate),convertString(transactionType)),
                        previousKey, nextKey)
            }
        } catch (exception: Exception){
            val numberOfRows = transactionDao.getTransactionByDateCount(DateTimeUtil.getStartOfDayInCalendarToEpoch(startDate),
                    DateTimeUtil.getEndOfDayInCalendarToEpoch(endDate),convertString(transactionType))
            val nextKey = if(params.key ?: 1 < (numberOfRows / Constants.PAGE_SIZE)){
                params.key ?: 1 + 1
            } else {
                null
            }
            return LoadResult.Page(transactionDao.getTransactionByDate(DateTimeUtil.getStartOfDayInCalendarToEpoch(startDate),
                    DateTimeUtil.getEndOfDayInCalendarToEpoch(endDate),convertString(transactionType)),
                    previousKey, nextKey)
        }
    }

    override val keyReuseSupported = true

    private fun convertString(type: String) = type.substring(0,1).toLowerCase() + type.substring(1).toLowerCase()
}