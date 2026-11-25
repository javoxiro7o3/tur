package uz.uydanimabor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class Page(val emoji: String, val title: String, val text: String)

private val pages = listOf(
    Page(
        "🥕",
        "Uyingizda nima bor?",
        "Mahsulot ustiga bir marta bosing — «bor». Ikkinchi marta bossangiz — «tez ishlatish kerak» (toʻq sariq). Uchinchi bosish belgini olib tashlaydi.",
    ),
    Page(
        "🍲",
        "Mos taomlarni toping",
        "Ilova belgilangan mahsulotlardan nimalar pishirish mumkinligini koʻrsatadi. Tez buziladigan mahsulotlarni ishlatadigan taomlar birinchi chiqadi.",
    ),
    Page(
        "🛒",
        "Pishiring va xarid qiling",
        "Retseptda porsiyani oʻzgartiring, taymerdan foydalaning. Yetishmagan 1–2 ta mahsulotni bitta tugma bilan xarid roʻyxatiga qoʻshing.",
    ),
)

/** F-13: birinchi tanishuv — 3 ta qisqa ekran. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) { Text("Oʻtkazib yuborish") }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
                val p = pages[i]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        p.emoji,
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                    Spacer(Modifier.size(24.dp))
                    Text(
                        p.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(p.text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics { contentDescription = "${pager.currentPage + 1}-ekran, jami ${pages.size}" },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (i == pager.currentPage) 12.dp else 8.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxSize(),
                        ) {}
                    }
                }
            }
            Button(
                onClick = {
                    if (pager.currentPage == pages.lastIndex) onDone()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(min = 56.dp),
            ) {
                Text(if (pager.currentPage == pages.lastIndex) "Boshlash" else "Keyingi")
            }
        }
    }
}
