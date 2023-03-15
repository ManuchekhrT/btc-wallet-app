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
fun WalletTopBar() {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6
            )
        }

    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun WalletTopBarPreview() {
    BitcoinWalletTheme() {

        Surface(
            color = MaterialTheme.colors.surface
        ) {
            WalletTopBar()
        }
    }
}