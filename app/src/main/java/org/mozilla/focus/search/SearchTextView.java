package org.mozilla.focus.search;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import org.mozilla.focus.utils.StringUtils;

public class SearchTextView extends TextView {

    private String originalText;

    public SearchTextView(Context context) {
        super(context);
    }

    public SearchTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSpanText(String s) {
        originalText = s;
        setText(StringUtils.createSpannableSearchHint(getContext(), s));
    }

    @Nullable
    public String getOriginalText() {
        return originalText;
    }
}
