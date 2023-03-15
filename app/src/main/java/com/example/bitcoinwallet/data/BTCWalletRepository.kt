package com.example.bitcoinwallet.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.bitcoinwallet.util.Constants
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.SendRequest
import java.io.File
import java.util.*
import java.util.concurrent.Executor


class BTCWalletRepository (
    private val context: Context
) {

    companion object {
        private const val TAG = "WalletApi"
    }

    private val parameters = TestNet3Params.get()
    private lateinit var walletAppKit: WalletAppKit


    private lateinit var onSent: (txid: String) -> Unit

    fun startWallet(
        balance: (balance: String) -> Unit,
        myAddress: (address: String) -> Unit,
        onSent: (txid: String) -> Unit,
        onDownloadProgress: (pct: Int) -> Unit
    ) {
        this.onSent = onSent
        Log.d(TAG, "startWallet: ")
        setBtcSDKThread()

        Log.d(TAG, "startWallet: cache dir: ${context.cacheDir.absolutePath}")

        val am = context.assets

        val walletFile = File(context.cacheDir, "wallet_test.wallet")
        if (!walletFile.exists()) {
            val inputStreamWallet = am.open("wallet_test.wallet")
            inputStreamWallet.use { input ->
                walletFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val spvchainFile = File(context.cacheDir, "wallet_test.spvchain")
        if (!spvchainFile.exists()) {
            val inputStream = am.open("wallet_test.spvchain")
            inputStream.use { input ->
                spvchainFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }


        walletAppKit = object : WalletAppKit(
            parameters,
            context.cacheDir,
            "wallet_test"
        ) {
            override fun onSetupCompleted() {
                super.onSetupCompleted()
                if (wallet().keyChainGroupSize < 1) wallet().importKey(ECKey())
                wallet().addCoinsReceivedEventListener { wallet1, tx, prevBalance, newBalance ->
                    balance(wallet().balance.toPlainString())
                    Log.d(TAG, "Received coins: ${newBalance - prevBalance}, now you have $newBalance")
                }
                wallet().addCoinsSentEventListener { wallet12, tx, prevBalance, newBalance ->
                    balance(wallet().balance.toPlainString())
                    Log.d(TAG, "Sent coins: ${prevBalance - newBalance}, now you have $newBalance")
                    Log.d(TAG, "Fee was: ${tx.fee}")
                }
                Log.d(TAG, "My address = " + wallet().freshReceiveAddress())
            }

        }
        walletAppKit.setDownloadListener(object : DownloadProgressTracker() {
            override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
                super.progress(pct, blocksSoFar, date)
                Log.d(TAG, "progress: $pct %")
                onDownloadProgress(pct.toInt())
            }
            override fun doneDownload() {
                super.doneDownload()
                Log.d(TAG, "doneDownload")
                val addr = walletAppKit.wallet().freshReceiveAddress().toString()
                val bal = walletAppKit.wallet().balance.toPlainString()
                myAddress(addr)
                balance(bal)
                Log.d(TAG, "My address = $addr")
                Log.d(TAG, "My balance = $bal")
            }
        })
        walletAppKit.setBlockingStartup(false)
        walletAppKit.startAsync()
        walletAppKit.awaitRunning()
//        walletAppKit.peerGroup().fastCatchupTimeSecs = walletAppKit.wallet().earliestKeyCreationTime
    }

    private fun setBtcSDKThread() {
        val handler = Handler(Looper.getMainLooper())
        val runInUIThread = Executor { runnable ->
            // For Android: handler was created in an Activity.onCreate method.
            handler.post(runnable)
        }
        Threading.USER_THREAD = runInUIThread
    }

    fun calculateAmount(recipientAddress: String, amount: String): Triple<String, String, String> {
        try {
            val request =
                SendRequest.to(Address.fromString(parameters, recipientAddress), Coin.parseCoin(amount))
            request.feePerKb = Coin.parseCoin(Constants.MINER_FEE)
            walletAppKit.wallet().completeTx(request)
            return Triple(
                Coin.parseCoin(amount).toPlainString(),
                request.tx.fee.toPlainString(),
                (Coin.parseCoin(amount) + request.tx.fee).toPlainString()
            )
        }
        catch (e: InsufficientMoneyException) {
            e.missing
            return Triple(
                Coin.parseCoin(amount).toPlainString(),
                (walletAppKit.wallet().balance - Coin.parseCoin(amount) + e.missing).toPlainString(),
                (walletAppKit.wallet().balance + e.missing).toPlainString()
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        return Triple("", "", "")
    }

    fun send(recipientAddress: String, amount: String) {
        if (walletAppKit.wallet().balance.isLessThan(Coin.parseCoin(amount))) {
            Log.d(TAG, "send: Not enough coins")
            return
        }
        val request =
            SendRequest.to(Address.fromString(parameters, recipientAddress), Coin.parseCoin(amount))
        request.feePerKb = Coin.parseCoin(Constants.MINER_FEE)
        try {
            walletAppKit.wallet().completeTx(request)
            walletAppKit.wallet().commitTx(request.tx)
            walletAppKit.peerGroup().broadcastTransaction(request.tx).broadcast()
            onSent(request.tx.txId.toString())
            Log.d(TAG, "send: Request complete")
            Log.d(TAG, "txid: ${request.tx.txId}")

        } catch (e: InsufficientMoneyException) {
            e.printStackTrace()
            Log.d(TAG, "send: error: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "send: error: ${e.message}")
        }
    }

}