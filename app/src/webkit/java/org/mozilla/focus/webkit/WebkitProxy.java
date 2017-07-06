package org.mozilla.focus.webkit;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Proxy;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.WebView;

public class WebkitProxy {

    private final static String DEFAULT_HOST = "localhost";//"127.0.0.1";
    private final static int DEFAULT_PORT = 8118;

    private final static int REQUEST_CODE = 0;

    private final static String TAG = "OrbotHelpher";

    public static boolean setProxy(Context ctx, String host, int port) throws Exception
    {

        return setWebkitProxyLollipop(ctx, host, port);

    }


    @TargetApi(21)
    public static boolean resetLollipopProxy(String appClass, Context appContext) {

        return setWebkitProxyLollipop(appContext,null,0);
    }

    // http://stackanswers.com/questions/25272393/android-webview-set-proxy-programmatically-on-android-l
    @TargetApi(21) // for android.util.ArrayMap methods
    @SuppressWarnings("rawtypes")
    private static boolean setWebkitProxyLollipop(Context appContext, String host, int port)
    {
        if (host != null) {
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port + "");
            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", port + "");
        }
        else
        {
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
            System.setProperty("https.proxyHost","");
            System.setProperty("https.proxyPort", "");
        }

        try {
            Class applictionClass = Class.forName("android.app.Application");
            Field mLoadedApkField = applictionClass.getDeclaredField("mLoadedApk");
            mLoadedApkField.setAccessible(true);
            Object mloadedApk = mLoadedApkField.get(appContext);
            Class loadedApkClass = Class.forName("android.app.LoadedApk");
            Field mReceiversField = loadedApkClass.getDeclaredField("mReceivers");
            mReceiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) mReceiversField.get(mloadedApk);
            for (Object receiverMap : receivers.values())
            {
                for (Object receiver : ((ArrayMap) receiverMap).keySet())
                {
                    Class clazz = receiver.getClass();
                    if (clazz.getName().contains("ProxyChangeListener"))
                    {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
                        onReceiveMethod.invoke(receiver, appContext, intent);
                    }
                }
            }
            return true;
        }
        catch (ClassNotFoundException e)
        {
            Log.d("ProxySettings","Exception setting WebKit proxy on Lollipop through ProxyChangeListener: " + e.toString());
        }
        catch (NoSuchFieldException e)
        {
            Log.d("ProxySettings","Exception setting WebKit proxy on Lollipop through ProxyChangeListener: " + e.toString());
        }
        catch (IllegalAccessException e)
        {
            Log.d("ProxySettings","Exception setting WebKit proxy on Lollipop through ProxyChangeListener: " + e.toString());
        }
        catch (NoSuchMethodException e)
        {
            Log.d("ProxySettings","Exception setting WebKit proxy on Lollipop through ProxyChangeListener: " + e.toString());
        }
        catch (InvocationTargetException e)
        {
            Log.d("ProxySettings","Exception setting WebKit proxy on Lollipop through ProxyChangeListener: " + e.toString());
        }
        return false;
    }

    private static boolean sendProxyChangedIntent(Context ctx, String host, int port)
    {

        try
        {
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (proxyPropertiesClass != null)
            {
                Constructor c = proxyPropertiesClass.getConstructor(String.class, Integer.TYPE,
                        String.class);

                if (c != null)
                {
                    c.setAccessible(true);
                    Object properties = c.newInstance(host, port, null);

                    Intent intent = new Intent(android.net.Proxy.PROXY_CHANGE_ACTION);
                    intent.putExtra("proxy",(Parcelable)properties);
                    ctx.sendBroadcast(intent);

                }

            }
        } catch (Exception e)
        {
            Log.e("ProxySettings",
                    "Exception sending Intent ",e);
        } catch (Error e)
        {
            Log.e("ProxySettings",
                    "Exception sending Intent ",e);
        }

        return false;

    }

    public static void resetProxy(Context ctx,String appClass) throws Exception {


        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");

        resetLollipopProxy(appClass, ctx);

    }





    public static AlertDialog initOrbot(Activity activity,
                                        CharSequence stringTitle,
                                        CharSequence stringMessage,
                                        CharSequence stringButtonYes,
                                        CharSequence stringButtonNo,
                                        CharSequence stringDesiredBarcodeFormats) {
        Intent intentScan = new Intent("org.torproject.android.START_TOR");
        intentScan.addCategory(Intent.CATEGORY_DEFAULT);

        try {
            activity.startActivityForResult(intentScan, REQUEST_CODE);
            return null;
        } catch (ActivityNotFoundException e) {
            return showDownloadDialog(activity, stringTitle, stringMessage, stringButtonYes,
                    stringButtonNo);
        }
    }

    private static AlertDialog showDownloadDialog(final Activity activity,
                                                  CharSequence stringTitle,
                                                  CharSequence stringMessage,
                                                  CharSequence stringButtonYes,
                                                  CharSequence stringButtonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(stringTitle);
        downloadDialog.setMessage(stringMessage);
        downloadDialog.setPositiveButton(stringButtonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:org.torproject.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                activity.startActivity(intent);
            }
        });
        downloadDialog.setNegativeButton(stringButtonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }



}

