package com.solosailing.presentation.regatta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solosailing.navigation.Routes
import com.solosailing.viewModel.LiveRegattasListViewModel


@Composable
fun LiveRegattasListScreen(
    navController: NavController,
    vm: LiveRegattasListViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { vm.load() }
    val regs = vm.active.collectAsState(emptyList()).value

    LazyColumn {
        items(regs) { reg ->
            ListItem(
                headlineContent = { Text(reg.name) },
                supportingContent = { Text("Creada: ${reg.createdAt}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        vm.open(reg.id) {
                            navController.navigate("${Routes.LIVE_REGATTAS}/${reg.id}")
                        }
                    }
            )
            HorizontalDivider()
        }
    }
}