package com.solosailing.presentation.regatta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solosailing.data.remote.dto.RegattaDto
import com.solosailing.viewModel.PastRegattasListViewModel

@Composable
fun PastRegattasListScreen(
    navController: NavController,
    vm: PastRegattasListViewModel = hiltViewModel()
) {
    val regs = vm.regs.collectAsState(emptyList()).value

    LazyColumn {
        items(regs) { reg: RegattaDto ->
            ListItem(
                headlineContent = { Text(reg.name) },
                supportingContent = { Text("Creada: ${reg.createdAt}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("past/${reg.id}")
                    }
            )
            HorizontalDivider()
        }
    }
}