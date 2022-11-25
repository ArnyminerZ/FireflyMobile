package xyz.hisname.fireflyiii.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.ui.theme.setContentThemed
import xyz.hisname.fireflyiii.util.extension.toastError

class AuthActivityCompose : AppCompatActivity() {
    private val authActivityViewModel: AuthActivityViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentThemed {
            AuthActivityContents(authActivityViewModel)
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
@ExperimentalPagerApi
@ExperimentalMaterial3Api
fun AuthActivityContents(authActivityViewModel: AuthActivityViewModel = viewModel()) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/emansih/FireflyMobile/wiki/Authentication".toUri(),
                    )
                    if (browserIntent.resolveActivity(context.packageManager) != null)
                        context.startActivity(browserIntent)
                    else
                        context.toastError(context.getString(R.string.no_browser_installed))
                }) {
                    Icon(Icons.Rounded.Help, stringResource(R.string.image_desc_auth_help))
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = 48.dp)
                    .fillMaxSize(),
            ) {
                Text(
                    stringResource(R.string.auth_sign_in_using),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    FilterChip(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        label = { Text(stringResource(R.string.auth_sign_in_pat)) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        label = { Text(stringResource(R.string.auth_sign_in_oauth)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalPager(
                    count = 2,
                    state = pagerState,
                    userScrollEnabled = false,
                ) {
                    Text("Page: $it")
                }
            }

            val isLoading by authActivityViewModel.isLoading.observeAsState(false)
            AnimatedVisibility(
                visible = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
            ) {
                LinearProgressIndicator()
            }
        }
    }
}
