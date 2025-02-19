package com.vitorpamplona.amethyst.ui.note

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.map
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.RelayBriefInfo
import com.vitorpamplona.amethyst.model.RelayInformation
import com.vitorpamplona.amethyst.service.Nip11Retriever
import com.vitorpamplona.amethyst.ui.actions.RelayInformationDialog
import com.vitorpamplona.amethyst.ui.components.RobohashFallbackAsyncImage
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.RelayIconFilter
import com.vitorpamplona.amethyst.ui.theme.Size13dp
import com.vitorpamplona.amethyst.ui.theme.Size15Modifier
import com.vitorpamplona.amethyst.ui.theme.Size15dp
import com.vitorpamplona.amethyst.ui.theme.StdStartPadding
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
public fun RelayBadgesHorizontal(baseNote: Note, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val expanded = remember { mutableStateOf(false) }

    RenderRelayList(baseNote, expanded, accountViewModel, nav)

    RenderExpandButton(baseNote, expanded) {
        ChatRelayExpandButton { expanded.value = true }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RenderRelayList(baseNote: Note, expanded: MutableState<Boolean>, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val noteRelays by baseNote.live().relayInfo.observeAsState()

    FlowRow(StdStartPadding) {
        val relaysToDisplay = remember(noteRelays, expanded.value) {
            if (expanded.value) noteRelays else noteRelays?.take(3)?.toImmutableList()
        }
        relaysToDisplay?.forEach {
            RenderRelay(it, accountViewModel, nav)
        }
    }
}

@Composable
fun RenderExpandButton(
    baseNote: Note,
    expanded: MutableState<Boolean>,
    content: @Composable () -> Unit
) {
    val showExpandButton by baseNote.live().relays.map {
        it.note.relays.size > 3
    }.observeAsState(baseNote.relays.size > 3)

    if (showExpandButton && !expanded.value) {
        content()
    }
}

@Composable
fun ChatRelayExpandButton(onClick: () -> Unit) {
    IconButton(
        modifier = Size15Modifier,
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Default.ChevronRight,
            null,
            modifier = Size15Modifier,
            tint = MaterialTheme.colors.placeholderText
        )
    }
}

@Composable
fun RenderRelay(relay: RelayBriefInfo, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    var relayInfo: RelayInformation? by remember { mutableStateOf(null) }

    if (relayInfo != null) {
        RelayInformationDialog(
            onClose = {
                relayInfo = null
            },
            relayInfo = relayInfo!!,
            relayBriefInfo = relay,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val ripple = rememberRipple(bounded = false, radius = Size15dp)

    val clickableModifier = remember(relay) {
        Modifier
            .padding(1.dp)
            .size(Size15dp)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple,
                onClick = {
                    accountViewModel.retrieveRelayDocument(
                        relay.url,
                        onInfo = {
                            relayInfo = it
                        },
                        onError = { url, errorCode, exceptionMessage ->
                            val msg = when (errorCode) {
                                Nip11Retriever.ErrorCode.FAIL_TO_ASSEMBLE_URL -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                                Nip11Retriever.ErrorCode.FAIL_TO_REACH_SERVER -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                                Nip11Retriever.ErrorCode.FAIL_TO_PARSE_RESULT -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                                Nip11Retriever.ErrorCode.FAIL_WITH_HTTP_STATUS -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                            }

                            scope.launch {
                                Toast
                                    .makeText(
                                        context,
                                        msg,
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        }
                    )
                }
            )
    }

    Box(
        modifier = clickableModifier
    ) {
        RenderRelayIcon(relay.favIcon)
    }
}

@Composable
fun RenderRelayIcon(iconUrl: String, size: Dp = Size13dp) {
    val backgroundColor = MaterialTheme.colors.background

    val iconModifier = remember {
        Modifier
            .size(size)
            .clip(shape = CircleShape)
            .background(backgroundColor)
    }

    RobohashFallbackAsyncImage(
        robot = iconUrl,
        model = iconUrl,
        contentDescription = stringResource(id = R.string.relay_icon),
        colorFilter = RelayIconFilter,
        modifier = iconModifier
    )
}
