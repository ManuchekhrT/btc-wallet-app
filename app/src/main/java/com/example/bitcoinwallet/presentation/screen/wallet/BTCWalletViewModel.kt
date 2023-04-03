package com.example.bitcoinwallet.presentation.screen.wallet

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.bitcoinwallet.data.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.bitcoinj.core.Coin
import javax.inject.Inject

@HiltViewModel
class BTCWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    companion object {
        private const val TAG = "WalletViewModel"
    }

    private val _balance = mutableStateOf("")
    val balance: State<String> = _balance

    private val _myAddress = mutableStateOf("")
    val myAddress: State<String> = _myAddress

    private val _amountToSend = mutableStateOf("")
    val amountToSend: State<String> = _amountToSend

    private val _addressToSend = mutableStateOf("")
    val addressToSend: State<String> = _addressToSend

    private val _isLoaded = mutableStateOf(false)
    val isLoaded: State<Boolean> = _isLoaded

    private val _isAmountCorrect = mutableStateOf(false)
    val isAmountCorrect: State<Boolean> = _isAmountCorrect

    private val _isAmountEnough = mutableStateOf(true)
    val isAmountEnough: State<Boolean> = _isAmountEnough

    private val _isAddressCorrect = mutableStateOf(false)
    val isAddressCorrect: State<Boolean> = _isAddressCorrect

    private val _downloadPercent = mutableStateOf(0)
    val downloadPercent: State<Int> = _downloadPercent

    private val _txId = mutableStateOf("dfgs435")
    val txId: State<String> = _txId

    private val _amountFeeTotal = mutableStateOf(Triple("", "", ""))
    val amountFeeTotal: State<Triple<String, String, String>> = _amountFeeTotal

    init {
        start()
    }

    fun updateAmountToSend(value: String) {
        _amountToSend.value = value
        _isAmountCorrect.value = value.toDoubleOrNull()?.let { true } ?: false
        _isAmountEnough.value = isAmountCorrect.value &&
                Coin.parseCoin(value) <= Coin.parseCoin(balance.value)
        Log.d(TAG, "_isAmountCorrect: ${_isAmountCorrect.value}")
    }

    fun updateAddressToSend(value: String) {
        _addressToSend.value = value
        _isAddressCorrect.value = value.matches(
            Regex("[A-Za-z0-9]*")
        )
        Log.d(TAG, "_isAddressCorrect: ${_isAddressCorrect.value}")
    }

    private fun start() {
        walletRepository.startWallet(
            balance = {
                _balance.value = it
                _isLoaded.value = true
            },
            myAddress = {
                _myAddress.value = it
            },
            onSent = { txid ->
                _txId.value = txid
            },
            onDownloadProgress = { pct ->
                _downloadPercent.value = 100
            }
        )
    }

    fun calculateAmount() {
        _amountFeeTotal.value = walletRepository.createTransaction(
            toAddress = addressToSend.value,
            amount = amountToSend.value
        )
    }

    fun send() {
        walletRepository.broadcastTransaction()
        _addressToSend.value = ""
        _amountToSend.value = ""
    }
}