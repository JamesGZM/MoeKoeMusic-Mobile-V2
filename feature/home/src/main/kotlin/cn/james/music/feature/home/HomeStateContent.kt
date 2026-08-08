package cn.james.music.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.action.MoeButton

@Composable
internal fun HomeLoading(onSearch: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { HomeHeader(onSearch) }
        item { HomeRadioHero() }
        item { HomeQuickEntries() }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.home_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun HomeMessage(
    onSearch: () -> Unit,
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { HomeHeader(onSearch) }
        item { HomeRadioHero() }
        item { HomeQuickEntries() }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoeButton(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(stringResource(R.string.home_retry))
                }
            }
        }
    }
}

@Composable
internal fun homeProblemMessage(problem: HomeProblemUi): String =
    stringResource(
        when (problem) {
            HomeProblemUi.SessionInitialization -> R.string.home_problem_session
            HomeProblemUi.Storage -> R.string.home_problem_storage
            HomeProblemUi.Offline -> R.string.home_problem_offline
            HomeProblemUi.Timeout -> R.string.home_problem_timeout
            HomeProblemUi.Connection -> R.string.home_problem_connection
            HomeProblemUi.ServiceUnavailable -> R.string.home_problem_unavailable
            HomeProblemUi.Rejected -> R.string.home_problem_rejected
            HomeProblemUi.Protocol -> R.string.home_problem_protocol
        },
    )
