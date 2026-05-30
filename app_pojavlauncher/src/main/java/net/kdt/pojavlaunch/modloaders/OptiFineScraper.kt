package net.kdt.pojavlaunch.modloaders

import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersions
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.DownloadUtils.ParseCallback
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.TagNode

class OptiFineScraper : ParseCallback<OptiFineVersions?> {
    private val mOptiFineVersions = OptiFineVersions().apply {
        minecraftVersions = ArrayList()
        optifineVersions = ArrayList()
    }
    private var mListInProgress: MutableList<OptiFineVersion?>? = null
    private var mMinecraftVersion: String? = null

    @Throws(DownloadUtils.ParseException::class)
    override fun process(input: String?): OptiFineVersions {
        val htmlCleaner = HtmlCleaner()
        val tagNode = htmlCleaner.clean(input)
        traverseTagNode(tagNode)
        insertVersionContent(null)
        if (mOptiFineVersions.optifineVersions.isNullOrEmpty() ||
            mOptiFineVersions.minecraftVersions.isNullOrEmpty()
        ) throw DownloadUtils.ParseException(null)
        return mOptiFineVersions
    }

    fun traverseTagNode(tagNode: TagNode) {
        if (isDownloadLine(tagNode) && mMinecraftVersion != null) {
            traverseDownloadLine(tagNode)
        } else if (isMinecraftVersionTag(tagNode)) {
            insertVersionContent(tagNode)
        } else {
            for (tagNodes in tagNode.getChildTags()) {
                traverseTagNode(tagNodes)
            }
        }
    }

    private fun isDownloadLine(tagNode: TagNode): Boolean {
        return tagNode.getName() == "tr" &&
                tagNode.hasAttribute("class") &&
                tagNode.getAttributeByName("class").startsWith("downloadLine")
    }

    private fun isMinecraftVersionTag(tagNode: TagNode): Boolean {
        return tagNode.getName() == "h2" &&
                tagNode.getText().toString().startsWith("Minecraft ")
    }

    private fun traverseDownloadLine(tagNode: TagNode) {
        val optiFineVersion = OptiFineVersion()
        optiFineVersion.minecraftVersion = mMinecraftVersion
        for (subNode in tagNode.getChildTags()) {
            if (subNode.getName() != "td") continue
            when (subNode.getAttributeByName("class")) {
                "colFile" -> optiFineVersion.versionName = subNode.getText().toString()
                "colMirror" -> optiFineVersion.downloadUrl = getLinkHref(subNode)
            }
        }
        mListInProgress!!.add(optiFineVersion)
    }

    private fun getLinkHref(parent: TagNode): String? {
        for (subNode in parent.getChildTags()) {
            if (subNode.getName() == "a" && subNode.hasAttribute("href")) {
                return subNode.getAttributeByName("href").replace("http://", "https://")
            }
        }
        return null
    }

    private fun insertVersionContent(tagNode: TagNode?) {
        if (mListInProgress != null && mMinecraftVersion != null) {
            mOptiFineVersions.minecraftVersions?.add(mMinecraftVersion)
            mOptiFineVersions.optifineVersions?.add(mListInProgress)
        }
        if (tagNode != null) {
            mMinecraftVersion = tagNode.getText().toString()
            mListInProgress = ArrayList()
        }
    }
}
