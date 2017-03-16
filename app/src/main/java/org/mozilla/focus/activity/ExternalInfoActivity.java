package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;

import org.mozilla.focus.customtabs.CustomTabsHelper;
import org.mozilla.focus.utils.SupportUtils;

/**
 * This is not an actual activity - this is just a helper to show an information page in a new
 * activity.
 */
public class ExternalInfoActivity {

    private static void launchURL(final Context context, final String url) {
        final String ctPackage = CustomTabsHelper.getPackageNameToUse(context);

        if (ctPackage != null) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabIntent = builder.build();

            customTabIntent.intent.setPackage(ctPackage);
            customTabIntent.launchUrl(context, Uri.parse(url));
        } else {
            // Fallback to InfoActivity if there's really no custom tabs support in existence.
            final Intent intent = InfoActivity.getIntentFor(context, url, "");
            context.startActivity(intent);
        }
    }

    public static void launchManifesto(final Context context) {
        final String manifestoURL = SupportUtils.getManifestoURL();
        launchURL(context, manifestoURL);
    }
}
