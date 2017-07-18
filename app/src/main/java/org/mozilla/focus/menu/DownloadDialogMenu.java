package org.mozilla.focus.menu;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.web.Download;
import org.mozilla.focus.web.IWebView;

public class DownloadDialogMenu {

    public static void show(final @NonNull Context context, final @NonNull IWebView.Callback callback, final @NonNull Download download) {

        String fileName = URLUtil.guessFileName(
                download.getUrl(), download.getContentDisposition(), download.getMimeType());

        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogStyle);
        builder.setCancelable(true);
        builder.setTitle(context.getString(R.string.download_dialog_title));

        final View dialogView = LayoutInflater.from(context).inflate(R.layout.download_dialog, null);
        builder.setView(dialogView);

        final ImageView downloadDialogIcon = (ImageView) dialogView.findViewById(R.id.download_dialog_icon);
        final TextView downloadDialogMessage = (TextView) dialogView.findViewById(R.id.download_dialog_file_name);
        final Button downloadDialogCancelButton = (Button) dialogView.findViewById(R.id.download_dialog_cancel);
        final Button downloadDialogDownloadButton = (Button) dialogView.findViewById(R.id.download_dialog_download);
        final TextView downloadDialogWarningMessage = (TextView) dialogView.findViewById(R.id.download_dialog_warning);

        downloadDialogIcon.setImageResource(R.drawable.ic_insert_drive_file_white_24px);
        downloadDialogMessage.setText(fileName);
        downloadDialogCancelButton.setText(context.getString(R.string.download_dialog_action_cancel));
        downloadDialogDownloadButton.setText(context.getString(R.string.download_dialog_action_download));
        downloadDialogWarningMessage.setText(getSpannedTextFromHtml(context, R.string.download_dialog_warning, R.string.app_name));

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // This even is only sent when the back button is pressed, or when a user
                // taps outside of the dialog:
                TelemetryWrapper.downloadDialogCancelEvent();
            }
        });

        final AlertDialog alert = builder.create();

        setCancelButtonOnClickListener(downloadDialogCancelButton, alert);
        setDownloadButtonOnClickListener(callback, downloadDialogDownloadButton, alert, download);

        alert.show();
    }

    private static void setDownloadButtonOnClickListener(final @NonNull IWebView.Callback callback, Button button, final AlertDialog dialog, final Download download) {
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        callback.onDownloadDialogConfirmed(download);
                        TelemetryWrapper.downloadDialogDownloadEvent();
                    }
                });
    }

    private static void setCancelButtonOnClickListener(Button button, final AlertDialog dialog) {
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        TelemetryWrapper.downloadDialogCancelEvent();
                    }
                });
    }

    public static Spanned getSpannedTextFromHtml(Context context, int text, int replaceString) {
        if (Build.VERSION.SDK_INT >= 24) {
            return (Html.fromHtml(String
                    .format(context.getText(text)
                            .toString(), context.getString(replaceString)), Html.FROM_HTML_MODE_LEGACY));
        } else {
            return (Html.fromHtml(String
                    .format(context.getText(text)
                            .toString(), context.getString(replaceString))));
        }
    }
}
