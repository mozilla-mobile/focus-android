package org.mozilla.focus.helpers;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.view.View;
import mozilla.components.concept.engine.HitResult;
import org.hamcrest.Matcher;
import org.mozilla.focus.R;
import org.mozilla.focus.webview.SystemWebView;

public class WebViewFakeLongPress implements ViewAction {
    public static ViewAction injectHitResult(HitResult hitResult) {
        return new WebViewFakeLongPress(hitResult);
    }

    private HitResult hitResult;

    private WebViewFakeLongPress(HitResult hitResult) {
        this.hitResult = hitResult;
    }

    @Override
    public Matcher<View> getConstraints() {
        return ViewMatchers.withId(R.id.webview);
    }

    @Override
    public String getDescription() {
        return "Long pressing webview";
    }

    @Override
    public void perform(UiController uiController, View view) {
        final SystemWebView webView = (SystemWebView) view;

        webView.getCallback()
                .onLongPress(hitResult);
    }
}
