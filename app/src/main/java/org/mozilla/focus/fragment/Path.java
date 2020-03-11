package org.mozilla.focus.fragment;

import android.os.Environment;

public class Path {
  public static String DIRECTORY_DOWNLOADS = "Download";

  public static String parser(String path) {
    int root = path.lastIndexOf("/0");
    String result = path.substring(root);
    result = result.replace("/0/","");
    return result;
  }
}
