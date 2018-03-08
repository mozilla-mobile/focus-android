package org.mozilla.focus.utils;

import android.view.View;
import android.view.WindowInsets;

/**
 * Created by Fer on 08/03/2018.
 */

public class StatusBarUtils {
    public static int STATUS_BAR_SIZE = -1;

    public interface StatusBarHeightListener {
        void onStatusBarHeightFetched(int statusBarHeight);
    }

    public static void getStatusBarHeight(View view, final StatusBarHeightListener listener) {
        if (STATUS_BAR_SIZE > 0)
            listener.onStatusBarHeightFetched(STATUS_BAR_SIZE);

        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                STATUS_BAR_SIZE = insets.getSystemWindowInsetTop();
                listener.onStatusBarHeightFetched(STATUS_BAR_SIZE);
                return insets;
            }
        });
    }
}
