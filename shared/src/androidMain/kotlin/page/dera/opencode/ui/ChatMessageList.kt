package page.dera.opencode.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import page.dera.opencode.api.LegacyWithParts

private const val VIEW_TYPE_MESSAGE = 0
private const val VIEW_TYPE_INDICATOR = 1

private class ChatMessageAdapter : RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {

    var parentCompositionContext: CompositionContext? = null
    var messages: List<LegacyWithParts> = emptyList()
    var streamingText: Map<String, String> = emptyMap()
    var projectRoot: String = ""
    var chatPainters: ChatPainters? = null
    var markdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState> = mutableMapOf()
    var parsedSegments: Map<String, List<DocumentSegment>> = emptyMap()
    var isSending: Boolean = false
    var isRunning: Boolean = false

    fun updateData(
        newMessages: List<LegacyWithParts>,
        newStreamingText: Map<String, String>,
        newProjectRoot: String,
        newChatPainters: ChatPainters,
        newMarkdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState>,
        newParsedSegments: Map<String, List<DocumentSegment>>,
        newIsSending: Boolean,
        newIsRunning: Boolean,
        parentContext: CompositionContext
    ) {
        val changed = this.messages.size != newMessages.size ||
                this.streamingText.size != newStreamingText.size ||
                this.isSending != newIsSending ||
                this.isRunning != newIsRunning ||
                this.messages != newMessages

        this.parentCompositionContext = parentContext
        this.messages = newMessages
        this.streamingText = newStreamingText
        this.projectRoot = newProjectRoot
        this.chatPainters = newChatPainters
        this.markdownCache = newMarkdownCache
        this.parsedSegments = newParsedSegments
        this.isSending = newIsSending
        this.isRunning = newIsRunning

        if (changed) {
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < messages.size) VIEW_TYPE_MESSAGE else VIEW_TYPE_INDICATOR
    }

    override fun getItemCount(): Int {
        val hasIndicator = isSending || isRunning
        return messages.size + (if (hasIndicator) 1 else 0)
    }

    override fun getItemId(position: Int): Long {
        if (position < messages.size) {
            return messages[position].info.id.hashCode().toLong()
        }
        return -1L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val composeView = ComposeView(parent.context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parentCtx = parentCompositionContext
        if (parentCtx != null) {
            holder.composeView.setParentCompositionContext(parentCtx)
        }
        holder.paintersState.value = chatPainters
        holder.projectRootState.value = projectRoot
        holder.streamingTextState.value = streamingText
        holder.markdownCacheState.value = markdownCache
        holder.parsedSegmentsState.value = parsedSegments
        if (position < messages.size) {
            holder.isIndicatorState.value = false
            holder.msgState.value = messages[position]
        } else {
            holder.isIndicatorState.value = true
            holder.msgState.value = null
        }
    }

    class ViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        val msgState = mutableStateOf<LegacyWithParts?>(null)
        val streamingTextState = mutableStateOf<Map<String, String>>(emptyMap())
        val projectRootState = mutableStateOf("")
        val paintersState = mutableStateOf<ChatPainters?>(null)
        val markdownCacheState = mutableStateOf<MutableMap<String, com.mikepenz.markdown.model.MarkdownState>>(mutableMapOf())
        val parsedSegmentsState = mutableStateOf<Map<String, List<DocumentSegment>>>(emptyMap())
        val isIndicatorState = mutableStateOf(false)

        init {
            composeView.setContent {
                val painters = paintersState.value ?: return@setContent
                if (isIndicatorState.value) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        SendingIndicator()
                    }
                } else {
                    val msg = msgState.value ?: return@setContent
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        MessageCard(
                            msg = msg,
                            streamingText = streamingTextState.value,
                            projectRoot = projectRootState.value,
                            chatPainters = painters,
                            markdownCache = markdownCacheState.value,
                            parsedSegments = parsedSegmentsState.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal actual fun ChatMessageList(
    modifier: Modifier,
    messages: List<LegacyWithParts>,
    streamingText: Map<String, String>,
    projectRoot: String,
    chatPainters: ChatPainters,
    markdownCache: MutableMap<String, com.mikepenz.markdown.model.MarkdownState>,
    parsedSegments: Map<String, List<DocumentSegment>>,
    isSending: Boolean,
    isRunning: Boolean
) {
    val parentContext = rememberCompositionContext()
    val adapter = remember { 
        ChatMessageAdapter().apply {
            setHasStableIds(true)
        }
    }
    var prevCount by remember { mutableStateOf(0) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                clipToPadding = false
                val lm = LinearLayoutManager(context).apply {
                    stackFromEnd = true
                }
                layoutManager = lm
                this.adapter = adapter
            }
        },
        update = { recyclerView ->
            val count = messages.size
            val shouldScroll = (prevCount == 0 && count > 0) || (count > prevCount)
            adapter.updateData(
                newMessages = messages,
                newStreamingText = streamingText,
                newProjectRoot = projectRoot,
                newChatPainters = chatPainters,
                newMarkdownCache = markdownCache,
                newParsedSegments = parsedSegments,
                newIsSending = isSending,
                newIsRunning = isRunning,
                parentContext = parentContext
            )
            if (shouldScroll && adapter.itemCount > 0) {
                recyclerView.post {
                    recyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
            prevCount = count
        }
    )
}
