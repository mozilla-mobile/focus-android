package org.mozilla.focus.webview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import org.mozilla.focus.R;

public class HttpAuthenticationDialog {

    private final Context mContext;

    private final String mHost;
    private final String mRealm;

    private AlertDialog mDialog;
    private TextView mUsernameView;
    private TextView mPasswordView;

    private OkListener mOkListener;
    private CancelListener mCancelListener;


    public HttpAuthenticationDialog(Context context, String host, String realm) {
        mContext = context;
        mHost = host;
        mRealm = realm;
        createDialog();
    }

    private String getUsername() {
        return mUsernameView.getText().toString();
    }

    private String getPassword() {
        return mPasswordView.getText().toString();
    }

    public void setOkListener(OkListener okListener) {
        mOkListener = okListener;
    }

    public void setCancelListener(CancelListener cancelListener) {
        mCancelListener = cancelListener;
    }


    public void show() {
        mDialog.show();
        mUsernameView.requestFocus();
    }


    private void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_http_auth, null);
        mUsernameView = view.findViewById(R.id.httpAuthUsername);
        mPasswordView = view.findViewById(R.id.httpAuthPassword);
        mPasswordView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });

        mDialog = new AlertDialog.Builder(mContext)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setView(view)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mOkListener != null) {
                            mOkListener.onOk(mHost, mRealm, getUsername(), getPassword());
                        }
                    }
                })
                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mCancelListener != null) mCancelListener.onCancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        if (mCancelListener != null) mCancelListener.onCancel();
                    }
                })
                .create();

        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    public interface OkListener {
        void onOk(String host, String realm, String username, String password);
    }

    public interface CancelListener {
        void onCancel();
    }
}