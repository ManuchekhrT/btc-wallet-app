package com.example.bitcoinwallet.presentation.screen.wallet

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.bitcoinwallet.R
import com.example.bitcoinwallet.presentation.theme.BitcoinWalletTheme

@Composable
fun BTCWalletBottomBar(pct: Int) {
    val text = if (pct == 0)
        stringResource(R.string.connecting_to_peers)
    else
        stringResource(id = R.string.current_percent).format(pct)
    TopAppBar(
        backgroundColor = MaterialTheme.colors.background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1
            )
        }

    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun WalletBottomBarPreview() {
    BitcoinWalletTheme() {

        Surface(
            color = MaterialTheme.colors.surface
        ) {
            BTCWalletBottomBar(40)
        }
    }
}