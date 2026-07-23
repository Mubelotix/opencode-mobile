package page.dera.opencode

import page.dera.opencode.api.OpencodeApi
import page.dera.opencode.api.SessionInfo

data class Tab(
    val id: String,
    val screen: TabScreen
)

sealed class TabScreen {
    data class ProjectSelection(val api: OpencodeApi) : TabScreen()
    data class Explorer(val api: OpencodeApi) : TabScreen()
    data class ProjectDetail(val api: OpencodeApi, val directory: String, val name: String) : TabScreen()
    data class SessionDetail(val api: OpencodeApi, val directory: String, val projectName: String, val session: SessionInfo) : TabScreen()
}

fun Tab.title(): String = when (val s = screen) {
    is TabScreen.ProjectSelection -> "New tab"
    is TabScreen.Explorer -> "Browse"
    is TabScreen.ProjectDetail -> s.name
    is TabScreen.SessionDetail -> s.session.title.ifEmpty { "Session" }
}
