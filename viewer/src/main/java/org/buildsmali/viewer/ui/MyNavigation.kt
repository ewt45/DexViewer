package org.buildsmali.viewer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.buildsmali.viewer.MyViewModel


@Serializable
object DexViewer

@Serializable
object TextViewer

/**
 * @param viewModel 不知为何，使用了NavHost之后，似乎HomeScreen的组合作用域变了，导致其内部自己获取的viewModel
 * 和activity中的不是同一个。只好从参数中把activity的传进来了
 */
@Composable
fun MyApp(viewModel: MyViewModel = viewModel()) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = DexViewer,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<DexViewer> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigationToTextViewer = {
                        viewModel.extractAndDecompileClasses()
                        navController.navigate(TextViewer)
                    }
                )
            }

            composable<TextViewer> {
                TextContentScreen (
                    viewModel = viewModel,
                    onNavigationToDexViewer = {
                    navController.popBackStack()
                })
            }
        }
    }

}