//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.mozilla.focus.widget;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.view.View;
import android.widget.FrameLayout;
import org.mozilla.focus.R.dimen;
import org.mozilla.focus.R.drawable;

public class SubActionButton extends FrameLayout {
  public static final int THEME_LIGHT = 0;
  public static final int THEME_DARK = 1;
  public static final int THEME_LIGHTER = 2;
  public static final int THEME_DARKER = 3;

  public SubActionButton(Activity activity, LayoutParams layoutParams, int theme, Drawable backgroundDrawable, View contentView, LayoutParams contentParams) {
    super(activity);
    this.setLayoutParams(layoutParams);
    if (backgroundDrawable == null) {
      if (theme == 0) {
        backgroundDrawable = activity.getResources().getDrawable(drawable.button_sub_action_selector);
      } else if (theme == 1) {
        backgroundDrawable = activity.getResources().getDrawable(drawable.button_sub_action_dark_selector);
      } else if (theme == 2) {
        backgroundDrawable = activity.getResources().getDrawable(drawable.button_action_selector);
      } else {
        if (theme != 3) {
          throw new RuntimeException("Unknown SubActionButton theme: " + theme);
        }

        backgroundDrawable = activity.getResources().getDrawable(drawable.button_action_dark_selector);
      }
    } else {
      backgroundDrawable = backgroundDrawable.mutate().getConstantState().newDrawable();
    }

    this.setBackgroundResource(backgroundDrawable);
    if (contentView != null) {
      this.setContentView(contentView, contentParams);
    }

    this.setClickable(true);
  }

  public void setContentView(View contentView, LayoutParams params) {
    if (params == null) {
      params = new LayoutParams(-2, -2, 17);
      int margin = this.getResources().getDimensionPixelSize(dimen.sub_action_button_content_margin);
      params.setMargins(margin, margin, margin, margin);
    }

    contentView.setClickable(false);
    this.addView(contentView, params);
  }

  public void setContentView(View contentView) {
    this.setContentView(contentView, (LayoutParams)null);
  }

  private void setBackgroundResource(Drawable drawable) {
    if (VERSION.SDK_INT >= 16) {
      this.setBackground(drawable);
    } else {
      this.setBackgroundDrawable(drawable);
    }

  }

  public static class Builder {
    private Activity activity;
    private LayoutParams layoutParams;
    private int theme;
    private Drawable backgroundDrawable;
    private View contentView;
    private LayoutParams contentParams;

    public Builder(Activity activity) {
      this.activity = activity;
      int size = activity.getResources().getDimensionPixelSize(dimen.sub_action_button_size);
      LayoutParams params = new LayoutParams(size, size, 51);
      this.setLayoutParams(params);
      this.setTheme(0);
    }

    public SubActionButton.Builder setLayoutParams(LayoutParams params) {
      this.layoutParams = params;
      return this;
    }

    public SubActionButton.Builder setTheme(int theme) {
      this.theme = theme;
      return this;
    }

    public SubActionButton.Builder setBackgroundDrawable(Drawable backgroundDrawable) {
      this.backgroundDrawable = backgroundDrawable;
      return this;
    }

    public SubActionButton.Builder setContentView(View contentView) {
      this.contentView = contentView;
      return this;
    }

    public SubActionButton.Builder setContentView(View contentView, LayoutParams contentParams) {
      this.contentView = contentView;
      this.contentParams = contentParams;
      return this;
    }

    public SubActionButton build() {
      return new SubActionButton(this.activity, this.layoutParams, this.theme, this.backgroundDrawable, this.contentView, this.contentParams);
    }
  }
}
