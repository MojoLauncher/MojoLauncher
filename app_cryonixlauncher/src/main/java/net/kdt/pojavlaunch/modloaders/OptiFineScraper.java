package net.kdt.pojavlaunch.modloaders;

import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.List;

public class OptiFineScraper implements DownloadUtils.ParseCallback<OptiFineUtils.OptiFineVersions> {
    private final OptiFineUtils.OptiFineVersions mOptiFineVersions;
    private List<OptiFineUtils.OptiFineVersion> mListInProgress;
    private String mGameVersion;

    public OptiFineScraper() {
        mOptiFineVersions = new OptiFineUtils.OptiFineVersions();
        mOptiFineVersions.gameVersions = new ArrayList<>();
        mOptiFineVersions.optifineVersions = new ArrayList<>();
    }

    @Override
    public OptiFineUtils.OptiFineVersions process(String input) throws DownloadUtils.ParseException {
        HtmlCleaner htmlCleaner = new HtmlCleaner();
        TagNode tagNode = htmlCleaner.clean(input);
        traverseTagNode(tagNode);
        insertVersionContent(null);
        if(mOptiFineVersions.optifineVersions.size() < 1 ||
            mOptiFineVersions.gameVersions.size() < 1) throw new DownloadUtils.ParseException(null);
        return mOptiFineVersions;
    }

    public void traverseTagNode(TagNode tagNode) {
        if(isDownloadLine(tagNode) && mGameVersion != null) {
            traverseDownloadLine(tagNode);
        } else if(isGameVersionTag(tagNode)) {
           insertVersionContent(tagNode);
        } else {
            for(TagNode tagNodes : tagNode.getChildTags()) {
                traverseTagNode(tagNodes);
            }
        }
    }

    private boolean isDownloadLine(TagNode tagNode) {
        return tagNode.getName().equals("tr") &&
                tagNode.hasAttribute("class") &&
                tagNode.getAttributeByName("class").startsWith("downloadLine");
    }

    private boolean isGameVersionTag(TagNode tagNode) {
        return tagNode.getName().equals("h2") &&
                tagNode.getText().toString().startsWith("Minecraft ");
    }

    private void traverseDownloadLine(TagNode tagNode) {
        OptiFineUtils.OptiFineVersion optiFineVersion = new OptiFineUtils.OptiFineVersion();
        optiFineVersion.gameVersion = mGameVersion;
        for(TagNode subNode : tagNode.getChildTags()) {
            if(!subNode.getName().equals("td")) continue;
            switch(subNode.getAttributeByName("class")) {
                case "colFile":
                    optiFineVersion.versionName = subNode.getText().toString();
                    break;
                case "colMirror":
                    optiFineVersion.downloadUrl = getLinkHref(subNode);
            }
        }
        mListInProgress.add(optiFineVersion);
    }
    private String getLinkHref(TagNode parent) {
        for(TagNode subNode : parent.getChildTags()) {
            if(subNode.getName().equals("a") && subNode.hasAttribute("href")) {
                return subNode.getAttributeByName("href").replace("http://", "https://");
            }
        }
        return null;
    }

    private void insertVersionContent(TagNode tagNode) {
        if(mListInProgress != null && mGameVersion != null) {
            mOptiFineVersions.gameVersions.add(mGameVersion);
            mOptiFineVersions.optifineVersions.add(mListInProgress);
        }
        if(tagNode != null) {
            mGameVersion = tagNode.getText().toString();
            mListInProgress = new ArrayList<>();
        }
    }
}
