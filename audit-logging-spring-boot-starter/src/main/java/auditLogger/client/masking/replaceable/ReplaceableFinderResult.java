package auditLogger.client.masking.replaceable;

import auditLogger.client.masking.finder.FinderResult;

import java.util.Collection;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class ReplaceableFinderResult extends FinderResult {
    protected Collection<ReplaceableResult> replaceableResultsCollection;

    public Collection<ReplaceableResult> getReplaceableResultsCollection() {
        return replaceableResultsCollection;
    }

    public void setReplaceableResultsCollection(Collection<ReplaceableResult> replaceableResultsCollection) {
        this.replaceableResultsCollection = replaceableResultsCollection;
    }

    public static class ReplaceableResult{
        private int beginIndex;
        private int endIndex;

        public int getBeginIndex() {
            return beginIndex;
        }

        public void setBeginIndex(int beginIndex) {
            this.beginIndex = beginIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public void setEndIndex(int endIndex) {
            this.endIndex = endIndex;
        }
    }

}
