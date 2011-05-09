package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

public class MagicPath {

    String mPath;
    MagicWatchType mType = MagicWatchType.Everything;

    public MagicPath(String actualPath, MagicWatchType type) {
        mType = type;
        mPath = actualPath;
    }

    public MagicPath(String backendConfigString) throws MagicPathParseException {
        // legacy...

        mType = MagicWatchType.matchTag(backendConfigString.charAt(0));
        if (mType == null) {
            throw new MagicPathParseException();
        }

        mPath = backendConfigString.substring(1);
    }

    public String getPath() {
        return mPath;
    }

    public MagicWatchType getType() {
        return mType;
    }

    public int getMaxDepth() {
        return 10000000;
    }

    public String toString() {
        return Character.toString(mType.getTag()) + mPath;
    }

    public void setType(MagicWatchType type) {
        mType = type;
    }
}