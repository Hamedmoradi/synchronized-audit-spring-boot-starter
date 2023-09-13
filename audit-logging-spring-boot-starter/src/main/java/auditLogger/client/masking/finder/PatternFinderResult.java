package auditLogger.client.masking.finder;

import java.util.regex.Matcher;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class PatternFinderResult extends FinderResult{

    protected Matcher matcher;

    public Matcher getMatcher(){
        return this.matcher;
    }

    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }
}
