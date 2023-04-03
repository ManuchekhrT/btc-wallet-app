package com.example.bitcoinwallet.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.bitcoinj.core.*
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.json.JSONArray
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executor

class WalletRepository(
    private val context: Context
) {

    private val params = TestNet3Params.get()
    private lateinit var transaction: Transaction
    private lateinit var onSent: (txid: String) -> Unit
    private lateinit var wallet: Wallet

    companion object {
        private const val TAG = "WalletApi"
    }

    fun startWallet(
        balance: (balance: String) -> Unit,
        myAddress: (address: String) -> Unit,
        onSent: (txid: String) -> Unit,
        onDownloadProgress: (pct: Int) -> Unit
    ) {
        this.onSent = onSent
        setBtcSDKThread()

        wallet = Wallet.createDeterministic(params, ScriptBuilder.createP2PKHOutputScript(ECKey()).scriptType)

        wallet.addCoinsReceivedEventListener { wallet1, tx, prevBalance, newBalance ->
            balance(wallet.balance.toPlainString())
            Log.d(TAG, "Received coins: ${newBalance - prevBalance}, now you have $newBalance")
        }
        wallet.addCoinsSentEventListener { wallet12, tx, prevBalance, newBalance ->
            balance(wallet.balance.toPlainString())
            Log.d(TAG, "Sent coins: ${prevBalance - newBalance}, now you have $newBalance")
            Log.d(TAG, "Fee was: ${tx.fee}")
        }

        balance(getBalance())
        myAddress(getCurrentAddress())

        onDownloadProgress(100)
    }

    private fun setBtcSDKThread() {
        val handler = Handler(Looper.getMainLooper())
        val runInUIThread = Executor { runnable ->
            // For Android: handler was created in an Activity.onCreate method.
            handler.post(runnable)
        }
        Threading.USER_THREAD = runInUIThread
    }

    private fun getBalance(): String {
        return wallet.balance.toPlainString()
    }

    private fun getCurrentAddress(): String {
        return wallet.currentReceiveAddress().toString()
    }

    fun createTransaction(
        toAddress: String,
        amount: String
    ): Triple<String, String, String> {
        val sendRequest = SendRequest.emptyWallet(Address.fromString(params, toAddress))
        val amountToSend = amount.toBigDecimal().multiply(BigDecimal.valueOf(100_000_000L))
        val feePerByte = 1L // 1 satoshi per byte
        val feeToSend = BigDecimal.valueOf(feePerByte * 100)
        val valueToSend = Coin.valueOf(amountToSend.toLong())
        sendRequest.feePerKb = Coin.valueOf(feeToSend.toLong())
        sendRequest.changeAddress = wallet.currentReceiveAddress()
        for (output in getUnspentOutputs(getCurrentAddress())) {
            sendRequest.tx.addInput(output)
            val outPoint = TransactionOutPoint(params, output.index.toLong(), output.parentTransactionHash)
            val input = sendRequest.tx.addSignedInput(outPoint, output.scriptPubKey, createKey())
            input.sequenceNumber = TransactionInput.NO_SEQUENCE - 2
        }
        sendRequest.tx.addOutput(valueToSend, Address.fromString(params, toAddress))
        sendRequest.ensureMinRequiredFee = false
        sendRequest.signInputs = true
        wallet.completeTx(sendRequest)
        transaction = sendRequest.tx

        return Triple(
            Coin.parseCoin(amount).toPlainString(),
            sendRequest.tx.fee.toPlainString(),
            (Coin.parseCoin(amount) + sendRequest.tx.fee).toPlainString()
        )
    }

    fun broadcastTransaction() {
        val txHex = Utils.HEX.encode(transaction.bitcoinSerialize())
        val url = URL("https://blockstream.info/api/tx")
        val postData = "hex=$txHex"
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            doOutput = true
            outputStream.write(postData.toByteArray())
            inputStream.bufferedReader().use {
                it.forEachLine { aLine ->
                    println(aLine)
                }
            }
        }
        onSent(transaction.txId.toString())
    }

    private fun getUnspentOutputs(address: String): List<TransactionOutput> {
        val url = URL("https://blockstream.info/api/address/$address/utxo")
        val json = url.readText()
        val outputs = mutableListOf<TransactionOutput>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val outputJson = jsonArray.getJSONObject(i)
            val value = outputJson.getLong("value")
            val scriptBytes = Utils.HEX.decode(outputJson.getString("scriptpubkey"))
            val script = Script(scriptBytes)
            outputs.add(
                TransactionOutput(
                    params,
                    null,
                    Coin.valueOf(value),
                    Address.fromString(
                        params,
                        script.getToAddress(params).toString()
                    )
                )
            )
        }
        return outputs
    }

    private fun createKey(): ECKey {
        return ECKey()
    }
}