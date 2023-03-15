package com.example.bitcoinwallet.presentation.screen.wallet

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bitcoinwallet.R
import com.example.bitcoinwallet.presentation.theme.BitcoinWalletTheme
import com.example.bitcoinwallet.presentation.theme.shimmerOnPrimary
import com.example.bitcoinwallet.util.Constants.BASE_URL
import org.bitcoinj.core.Coin

@Composable
fun BTCWalletScreen(
    viewModel: BTCWalletViewModel = hiltViewModel()
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val balance by viewModel.balance
    val myAddress by viewModel.myAddress
    val isLoaded by viewModel.isLoaded
    val downloadPercent by viewModel.downloadPercent

    val transition = rememberInfiniteTransition()
    val animateFloat by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutLinearInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )


    Scaffold(
        topBar = { WalletTopBar() },
        backgroundColor = MaterialTheme.colors.surface,
        bottomBar = {
            if (!isLoaded) {
                BTCWalletBottomBar(pct = downloadPercent)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .padding(paddingValues)
                .scrollable(
                    state = rememberScrollState(),
                    orientation = Orientation.Vertical
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Card(
                modifier = Modifier
                    .padding(bottom = 8.dp),
                backgroundColor = MaterialTheme.colors.primary,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.balance),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onPrimary
                    )
                    if (isLoaded) {
                        Text(
                            text = stringResource(id = R.string.tbtc_amount).format(balance),
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onPrimary
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth(0.7f)
                                .alpha(animateFloat),
                            shape = RoundedCornerShape(8.dp),
                            color = shimmerOnPrimary
                        ) {}
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // My address
                    Text(
                        text = stringResource(R.string.my_address),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onPrimary
                    )
                    if (isLoaded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = myAddress,
                                style = MaterialTheme.typography.subtitle1,
                                color = MaterialTheme.colors.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(9f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(myAddress))
                                    Toast.makeText(
                                        context,
                                        "The address has been copied to the clipboard.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
                                    contentDescription = stringResource(id = R.string.copy),
                                    tint = MaterialTheme.colors.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .height(24.dp)
                                .fillMaxWidth(0.9f)
                                .alpha(animateFloat),
                            shape = RoundedCornerShape(8.dp),
                            color = shimmerOnPrimary
                        ) {}
                    }

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SendCard(viewModel = viewModel)

        }


    }
}

@Composable
fun SendCard(
    viewModel: BTCWalletViewModel
) {
    val balance by viewModel.balance
    val amountToSend by viewModel.amountToSend
    val addressToSend by viewModel.addressToSend
    val isAddressCorrect by viewModel.isAddressCorrect
    val isAmountCorrect by viewModel.isAmountCorrect
    val isAmountEnough by viewModel.isAmountEnough
    val isLoaded by viewModel.isLoaded
    val txId by viewModel.txId
    val showConfirmationDialog = remember {
        mutableStateOf(false)
    }

    val showTransactionDialog = remember {
        mutableStateOf(false)
    }

    val amountFeeTotal by viewModel.amountFeeTotal

    if (showConfirmationDialog.value) {

        if (Coin.parseCoin(amountFeeTotal.third) <= Coin.parseCoin(balance)) {

            ConfirmationDialog(
                show = showConfirmationDialog,
                amount = amountFeeTotal.first,
                fee = amountFeeTotal.second,
                total = amountFeeTotal.third,
                address = addressToSend,
                onConfirm = {
                    viewModel.send()
                    showTransactionDialog.value = true
                }
            )
        } else {
            NotEnoughCoinsDialog(
                show = showConfirmationDialog,
                amount = amountFeeTotal.first,
                fee = amountFeeTotal.second,
                total = amountFeeTotal.third,
                address = addressToSend
            )
        }
    }

    if (showTransactionDialog.value) {
        TransactionDialog(
            show = showTransactionDialog,
            txId = txId,
            ref = "$BASE_URL$txId"
        )
    }

    Card(
        modifier = Modifier,
        backgroundColor = MaterialTheme.colors.background,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.send),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6
            )
            Column() {
                OutlinedTextField(
                    value = amountToSend,
                    onValueChange = { value ->
                        viewModel.updateAmountToSend(value)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.h6,
                    label = {
                        Text(text = stringResource(R.string.amount))
                    },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = isLoaded,
                    isError = (!isAmountCorrect || !isAmountEnough) && amountToSend != ""
                )
                if (!isAmountCorrect && amountToSend != "") {
                    Text(
                        text = stringResource(R.string.incorrect_amount),
                        color = MaterialTheme.colors.error
                    )
                } else if (!isAmountEnough && amountToSend != "") {
                    Text(
                        text = "Not enough coins.",
                        color = MaterialTheme.colors.error
                    )
                }
            }
            OutlinedTextField(
                value = addressToSend,
                onValueChange = { value ->
                    viewModel.updateAddressToSend(value)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.h6,
                label = {
                    Text(text = stringResource(R.string.address))
                },
                shape = MaterialTheme.shapes.medium,
                enabled = isLoaded,
                isError = !isAddressCorrect && addressToSend != "",
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    onClick = {
                        viewModel.calculateAmount()
                        showConfirmationDialog.value = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = isLoaded && isAddressCorrect && isAmountCorrect && isAmountEnough
                ) {
                    Text(
                        text = stringResource(R.string.send),
                        style = MaterialTheme.typography.h6
                    )
                }
            }
        }
    }
}


@Composable
fun ConfirmationDialog(
    show: MutableState<Boolean>,
    amount: String,
    fee: String,
    total: String,
    address: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            show.value = false
        },
        title = {
            Text(text = "Operation confirmation.")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.are_you_sure),
                    style = MaterialTheme.typography.subtitle2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.amount) + ":",
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            text = "$amount tBTC",
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.mining_fee) + ":",
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            text = "$fee tBTC",
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.to_address),
                    style = MaterialTheme.typography.subtitle2
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = address,
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.onBackground
                    )
                }
            }

        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {

                Button(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    onClick = {
                        onConfirm()
                        show.value = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.send) + " $total tBTC",
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.8f),
                    onClick = {
                        show.value = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
    )
}


@Composable
fun NotEnoughCoinsDialog(
    show: MutableState<Boolean>,
    amount: String,
    fee: String,
    total: String,
    address: String
) {
    AlertDialog(
        onDismissRequest = {
            show.value = false
        },
        title = {
            Text(text = "Not Enough Coins.")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.amount) + ":",
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            text = "$amount tBTC",
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.mining_fee) + ":",
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            text = "$fee tBTC",
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.error
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.total) + ":",
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            text = "$total tBTC",
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            }

        },
        confirmButton = {},
        dismissButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.6f),
                    onClick = {
                        show.value = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
    )
}


@Composable
fun TransactionDialog(
    show: MutableState<Boolean>,
    txId: String,
    ref: String
) {

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colors.onBackground,
            )
        ) {
            append(stringResource(id = R.string.your_tx_id))
        }


        pushStringAnnotation(tag = "txId", annotation = ref)
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colors.primary,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(txId)
        }
        pop()
    }

    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = {
            show.value = false
        },
        title = {
            Text(text = stringResource(R.string.funds_sent))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "txId",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let {
                                uriHandler.openUri(it.item)
                            }
                    })
            }
        },
        confirmButton = {},
        dismissButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(0.5f),
                    onClick = {
                        show.value = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
    )
}


@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WalletScreenPreview() {
    BitcoinWalletTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.surface
        ) {
            BTCWalletScreen()
        }

    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ConfirmationDialogPreview() {
    BitcoinWalletTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.surface
        ) {
            ConfirmationDialog(
                show = remember {
                    mutableStateOf(true)
                },
                amount = "0.0103",
                fee = "0.00006",
                total = "0.01036",
                address = "mvm9SQR9w4r1kCE8ZmSTozUkEm3c6L5xP9",
                onConfirm = {}
            )
        }

    }
}