package com.xl.launcher.xy.community

object CommunityHub { fun start() = Unit }
object ModpackSharing { fun share(id: String) = Unit }
object ProfileSharing { fun share(id: String) = Unit }
object ResourcePackSharing { fun share(id: String) = Unit }
object CreatorHub { fun open() = Unit }
object NewsHub { fun latest() = listOf<String>() }
object UpdateCenter { fun check() = Unit }
object KnowledgeBase { fun query(q: String) = listOf<String>() }
object ResearchCenter { fun run() = Unit }
