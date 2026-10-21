package com.lumi.stickerize.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumi.stickerize.R
import com.lumi.stickerize.data.ThemePack
import com.lumi.stickerize.ui.theme.LumiBlue

/** Daftar paket tema siap pakai. Ketuk satu paket → kata & pose semua stiker langsung terisi. */
@Composable
fun ThemePackScreen(
    packs: List<ThemePack>,
    count: Int,
    selected: String?,
    onPick: (ThemePack) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    LightSystemBarIcons()
    FullScreenLayer {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "🎁 " + stringResource(R.string.theme_packs),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                }
            }
            Text(
                stringResource(R.string.theme_packs_help, packs.size, count),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(packs, key = { it.id }) { pack ->
                    val isSel = pack.id == selected
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                if (isSel) 2.dp else 1.dp,
                                if (isSel) LumiBlue else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onPick(pack) }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pack.emoji, fontSize = 26.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pack.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 17.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            pack.items.take(count).joinToString(" • ") { it.caption },
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
