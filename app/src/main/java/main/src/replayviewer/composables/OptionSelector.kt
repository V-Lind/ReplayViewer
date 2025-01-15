package main.src.replayviewer.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> OptionSelector(
    options: List<T>,
    selectedOption: MutableState<T>,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit = {},
    groupingOptions: ((T) -> String)? = null,
    colorRules: ((T) -> Color)? = null,
) {
    val groupedOptions = groupingOptions?.let { options.groupBy(it) }?.toSortedMap(compareBy {
        when (it) {
            "16:9" -> 1
            "4:3" -> 2
            else -> 3
        }
    }) ?: mapOf("" to options)

    LazyColumn(modifier = modifier) {
        item {
            groupedOptions.forEach { (group, options) ->
                if (group.isNotEmpty()) {
                    Text(text = group, modifier = Modifier.padding(8.dp))
                }
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    options.forEach { option ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    selectedOption.value = option
                                    onSelect(option)
                                }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = option.toString(),
                                color = if (selectedOption.value == option) Color.Green else Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}