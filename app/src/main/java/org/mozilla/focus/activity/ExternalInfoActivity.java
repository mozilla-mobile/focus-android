package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;

import org.mozilla.focus.R;
import org.mozilla.focus.customtabs.CustomTabsHelper;
import org.mozilla.focus.utils.SupportUtils;

/**
 * This is not an actual activity - this is just a helper to show an information page in a new
 * activity.
 */
public class ExternalInfoActivity {

    public static void launchURL(final Context context, final String url) {
        final String ctPackage = CustomTabsHelper.getPackageNameToUse(context);

        if (ctPackage != null) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(context.getResources().getColor(R.color.colorPrimary));

            CustomTabsIntent customTabIntent = builder.build();

            customTabIntent.intent.setPackage(ctPackage);
            customTabIntent.launchUrl(context, Uri.parse(url));
        } else {
            // Fallback to InfoActivity if there's really no custom tabs support in existence.
            // We use no title, since it's hard or impossible to determine the title for most
            // pages (moreover there's little value in doing so, since this should happen to
            // a minimal number of users).
            final Intent intent = InfoActivity.getIntentFor(context, url, "");
            context.startActivity(intent);
        }
    }

    public static void launchManifesto(final Context context) {
        final String manifestoURL = SupportUtils.getManifestoURL();
        launchURL(context, manifestoURL);
    }

    public static void launchDefaultBrowserInfo(final Context context) {
        final String url = "https://support.mozilla.org/kb/make-firefox-default-browser-android?utm_source=inproduct&amp;utm_medium=settings&amp;utm_campaign=mobileandroid";
        launchURL(context, url);
    }

    public static void launchSumoPage(final Context context, final String sumoTag) {
        final String url = SupportUtils.getSumoURLForTopic(context, sumoTag);
        launchURL(context, url);
    }
}
