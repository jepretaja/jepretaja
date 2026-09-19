package com.jepretaja.app.ui.screens.creatordashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.grid.LazyGridState

/**
 * Keadaan drag untuk LazyVerticalGrid.
 *
 * Posisi item selalu dibaca dari layoutInfo terbaru. Setelah item ditukar,
 * indeks dan offset langsung diperbarui agar gerakan berikutnya tidak memakai
 * koordinat yang sudah usang.
 */
internal class PenggeserGrid(
    private val gridState: LazyGridState,
    private val onPindah: (Int, Int) -> Unit,
) {
    var indexDiseret by mutableStateOf<Int?>(null)
        private set
    var geser by mutableStateOf(Offset.Zero)
        private set

    fun mulai(posisi: Offset) {
        val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            posisi.x.toInt() in info.offset.x..(info.offset.x + info.size.width) &&
                posisi.y.toInt() in info.offset.y..(info.offset.y + info.size.height)
        }
        indexDiseret = item?.index
        geser = Offset.Zero
    }

    fun seret(jarak: Offset) {
        val dari = indexDiseret ?: return
        geser += jarak
        val info = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == dari } ?: return
        val pusatX = info.offset.x + info.size.width / 2f + geser.x
        val pusatY = info.offset.y + info.size.height / 2f + geser.y
        val tujuan = gridState.layoutInfo.visibleItemsInfo.firstOrNull { target ->
            target.index != dari &&
                pusatX in target.offset.x.toFloat()..(target.offset.x + target.size.width).toFloat() &&
                pusatY in target.offset.y.toFloat()..(target.offset.y + target.size.height).toFloat()
        } ?: return
        onPindah(dari, tujuan.index)
        indexDiseret = tujuan.index
        geser = Offset.Zero
    }

    fun selesai() {
        indexDiseret = null
        geser = Offset.Zero
    }
}

@Composable
internal fun rememberPenggeser(
    gridState: LazyGridState,
    onPindah: (Int, Int) -> Unit,
): PenggeserGrid {
    val pindahTerbaru by rememberUpdatedState(onPindah)
    return remember(gridState) { PenggeserGrid(gridState) { a, b -> pindahTerbaru(a, b) } }
}