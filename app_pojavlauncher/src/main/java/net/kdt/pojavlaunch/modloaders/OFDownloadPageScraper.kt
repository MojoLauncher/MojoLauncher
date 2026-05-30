package net.kdt.pojavlaunch.modloaders

import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.HtmlNode
import org.htmlcleaner.TagNode
import org.htmlcleaner.TagNodeVisitor
import java.io.IOException
import java.net.URL

class OFDownloadPageScraper : TagNodeVisitor {
    private var mDownloadFullUrl: String? = null

    @Throws(IOException::class)
    private fun runInner(url: String?): String? {
        val htmlCleaner = HtmlCleaner()
        htmlCleaner.clean(URL(url)).traverse(this)
        return mDownloadFullUrl
    }

    override fun visit(parentNode: TagNode?, htmlNode: HtmlNode?): Boolean {
        if (isDownloadUrl(parentNode, htmlNode)) {
            val tagNode = htmlNode as TagNode
            var href = tagNode.getAttributeByName("href")
            if (!href.startsWith("https://")) href = "https://optifine.net/" + href
            this.mDownloadFullUrl = href
            return false
        }
        return true
    }

    fun isDownloadUrl(parentNode: TagNode?, htmlNode: HtmlNode?): Boolean {
        if (htmlNode !is TagNode) return false
        if (parentNode == null) return false
        val tagNode = htmlNode
        if (!(parentNode.getName() == "span"
                    && "Download" == parentNode.getAttributeByName("id"))
        ) return false
        return tagNode.getName() == "a" &&
                "onDownload()" == tagNode.getAttributeByName("onclick")
    }

    companion object {
        @Throws(IOException::class)
        fun run(urlInput: String?): String? {
            return OFDownloadPageScraper().runInner(urlInput)
        }
    }
}
