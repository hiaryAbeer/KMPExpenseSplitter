package com.abeerapps.kmpexpensesplitter.feature.registration

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.FieldContainer
import com.abeerapps.kmpexpensesplitter.core.domain.model.Currency
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.ic_chevron_up
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.ic_currency
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.ic_search
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_currency_no_results
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_currency_search_placeholder
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_currency_sheet_title
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_currency_value
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The "Default currency" field. Tapping it opens [CurrencyPickerSheet]. */
@Composable
internal fun CurrencyField(
    selected: Currency,
    isSheetVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Chevron points down when closed, up while the sheet is open (as in the design).
    val chevronRotation by animateFloatAsState(if (isSheetVisible) 0f else 180f)

    FieldContainer(
        leadingIcon = painterResource(Res.drawable.ic_currency),
        highlighted = isSheetVisible,
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.DropdownList, onClick = onClick)
            .testTag(RegistrationTestTags.CURRENCY_FIELD),
    ) {
        Text(
            text = stringResource(Res.string.registration_currency_value, selected.code.value, selected.name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_up),
            contentDescription = null, // The field reads as a drop-down list.
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).rotate(chevronRotation),
        )
    }
}

/**
 * Full-height bottom sheet listing every currency, with search by code, name,
 * country or symbol. Selecting a row animates the sheet away, then reports it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyPickerSheet(
    selected: Currency,
    query: String,
    results: List<Currency>,
    onQueryChange: (String) -> Unit,
    onSelect: (CurrencyCode) -> Unit,
    onDismiss: () -> Unit,
) {
    // Fully expanded: a half sheet would be covered by the keyboard while searching.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Open with the current selection in view (a few rows of context above it).
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = results.indexOf(selected)
            .let { if (it > 2) it - 2 else 0 },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag(RegistrationTestTags.CURRENCY_SHEET),
    ) {
        Column(Modifier.fillMaxHeight().imePadding()) {
            Text(
                text = stringResource(Res.string.registration_currency_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(Res.string.registration_currency_search_placeholder),
                leadingIcon = painterResource(Res.drawable.ic_search),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.padding(horizontal = 24.dp).testTag(RegistrationTestTags.CURRENCY_SEARCH),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (results.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.registration_currency_no_results, query.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f).selectableGroup(),
                ) {
                    items(results, key = { it.code.value }) { currency ->
                        CurrencyRow(
                            currency = currency,
                            isSelected = currency == selected,
                            onClick = {
                                focusManager.clearFocus()
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    onSelect(currency.code)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * One currency: symbol | name | code. Row spacing, type and
 * colors follow the Figma currency list.
 */
@Composable
private fun CurrencyRow(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            // Design uses 32dp for "$"-style symbols; the full catalog has up to 5 chars
            // ("F CFA", "MOP$"), so 44dp keeps every name aligned in one column.
            modifier = Modifier.width(44.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = currency.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = currency.code.value,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
