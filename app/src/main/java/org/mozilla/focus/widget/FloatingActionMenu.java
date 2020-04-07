package org.mozilla.focus.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.FrameLayout.LayoutParams;
import org.mozilla.focus.R.dimen;
import java.util.ArrayList;
import java.util.Iterator;

public class FloatingActionMenu {
  private View mainActionView;
  private int startAngle;
  private int endAngle;
  private int radius;
  private ArrayList<FloatingActionMenu.Item> subActionItems;
  private MenuAnimationHandler animationHandler;
  private FloatingActionMenu.MenuStateChangeListener stateChangeListener;
  private boolean animated;
  private boolean open;

  public View getMainActionView() {
    return mainActionView;
  }

  public int getStartAngle() {
    return startAngle;
  }

  public int getEndAngle() {
    return endAngle;
  }

  public FloatingActionMenu(View mainActionView, int startAngle, int endAngle, int radius, ArrayList<FloatingActionMenu.Item> subActionItems, MenuAnimationHandler animationHandler, boolean animated, FloatingActionMenu.MenuStateChangeListener stateChangeListener) {
    this.mainActionView = mainActionView;
    this.startAngle = startAngle;
    this.endAngle = endAngle;
    this.radius = radius;
    this.subActionItems = subActionItems;
    this.animationHandler = animationHandler;
    this.animated = animated;
    this.open = false;
    this.stateChangeListener = stateChangeListener;
    this.mainActionView.setClickable(true);
    this.mainActionView.setOnClickListener(new FloatingActionMenu.ActionViewClickListener());
    if (animationHandler != null) {
      animationHandler.setMenu(this);
    }

    Iterator i$ = subActionItems.iterator();

    while(true) {
      FloatingActionMenu.Item item;
      do {
        if (!i$.hasNext()) {
          return;
        }

        item = (FloatingActionMenu.Item)i$.next();
      } while(item.width != 0 && item.height != 0);

      ((ViewGroup)this.getActivityContentView()).addView(item.view);
      item.view.setAlpha(0.0F);
      item.view.post(new FloatingActionMenu.ItemViewQueueListener(item));
    }
  }

  public void open(boolean animated) {
    Point center = this.getActionViewCenter();
    this.calculateItemPositions();
    int i;
    LayoutParams params;
    if (animated && this.animationHandler != null) {
      if (this.animationHandler.isAnimating()) {
        return;
      }

      for(i = 0; i < this.subActionItems.size(); ++i) {
        if (((FloatingActionMenu.Item)this.subActionItems.get(i)).view.getParent() != null) {
          throw new RuntimeException("All of the sub action items have to be independent from a parent.");
        }

        params = new LayoutParams(((FloatingActionMenu.Item)this.subActionItems.get(i)).width, ((FloatingActionMenu.Item)this.subActionItems.get(i)).height, 51);
        params.setMargins(center.x - ((FloatingActionMenu.Item)this.subActionItems.get(i)).width / 2, center.y - ((FloatingActionMenu.Item)this.subActionItems.get(i)).height / 2, 0, 0);
        ((ViewGroup)this.getActivityContentView()).addView(((FloatingActionMenu.Item)this.subActionItems.get(i)).view, params);
      }

      this.animationHandler.animateMenuOpening(center);
    } else {
      for(i = 0; i < this.subActionItems.size(); ++i) {
        params = new LayoutParams(((FloatingActionMenu.Item)this.subActionItems.get(i)).width, ((FloatingActionMenu.Item)this.subActionItems.get(i)).height, 51);
        params.setMargins(((FloatingActionMenu.Item)this.subActionItems.get(i)).x, ((FloatingActionMenu.Item)this.subActionItems.get(i)).y, 0, 0);
        ((FloatingActionMenu.Item)this.subActionItems.get(i)).view.setLayoutParams(params);
        ((ViewGroup)this.getActivityContentView()).addView(((FloatingActionMenu.Item)this.subActionItems.get(i)).view, params);
      }
    }

    this.open = true;
    if (this.stateChangeListener != null) {
      this.stateChangeListener.onMenuOpened(this);
    }

  }

  public void close(boolean animated) {
    if (animated && this.animationHandler != null) {
      if (this.animationHandler.isAnimating()) {
        return;
      }

      this.animationHandler.animateMenuClosing(this.getActionViewCenter());
    } else {
      for(int i = 0; i < this.subActionItems.size(); ++i) {
        ((ViewGroup)this.getActivityContentView()).removeView(((FloatingActionMenu.Item)this.subActionItems.get(i)).view);
      }
    }

    this.open = false;
    if (this.stateChangeListener != null) {
      this.stateChangeListener.onMenuClosed(this);
    }

  }

  public void toggle(boolean animated) {
    if (this.open) {
      this.close(animated);
    } else {
      this.open(animated);
    }

  }

  public boolean isOpen() {
    return this.open;
  }

  public void updateItemPositions() {
    if (this.isOpen()) {
      this.calculateItemPositions();

      for(int i = 0; i < this.subActionItems.size(); ++i) {
        LayoutParams params = new LayoutParams(((FloatingActionMenu.Item)this.subActionItems.get(i)).width, ((FloatingActionMenu.Item)this.subActionItems.get(i)).height, 51);
        params.setMargins(((FloatingActionMenu.Item)this.subActionItems.get(i)).x, ((FloatingActionMenu.Item)this.subActionItems.get(i)).y, 0, 0);
        ((FloatingActionMenu.Item)this.subActionItems.get(i)).view.setLayoutParams(params);
      }

    }
  }

  private Point getActionViewCoordinates() {
    int[] coords = new int[2];
    this.mainActionView.getLocationOnScreen(coords);
    Rect activityFrame = new Rect();
    this.getActivityContentView().getWindowVisibleDisplayFrame(activityFrame);
    coords[0] -= this.getScreenSize().x - this.getActivityContentView().getMeasuredWidth();
    coords[1] -= activityFrame.height() + activityFrame.top - this.getActivityContentView().getMeasuredHeight();
    return new Point(coords[0], coords[1]);
  }

  public Point getActionViewCenter() {
    Point point = this.getActionViewCoordinates();
    point.x += this.mainActionView.getMeasuredWidth() / 2;
    point.y += this.mainActionView.getMeasuredHeight() / 2;
    return point;
  }

  private void calculateItemPositions() {
    Point center = this.getActionViewCenter();
    RectF area = new RectF((float)(center.x - this.radius), (float)(center.y - this.radius), (float)(center.x + this.radius), (float)(center.y + this.radius));
    Path orbit = new Path();
    orbit.addArc(area, (float)this.startAngle, (float)(this.endAngle - this.startAngle));
    PathMeasure measure = new PathMeasure(orbit, false);
    int divisor;
    if (Math.abs(this.endAngle - this.startAngle) < 360 && this.subActionItems.size() > 1) {
      divisor = this.subActionItems.size() - 1;
    } else {
      divisor = this.subActionItems.size();
    }

    for(int i = 0; i < this.subActionItems.size(); ++i) {
      float[] coords = new float[]{0.0F, 0.0F};
      measure.getPosTan((float)i * measure.getLength() / (float)divisor, coords, (float[])null);
      ((FloatingActionMenu.Item)this.subActionItems.get(i)).x = (int)coords[0] - ((FloatingActionMenu.Item)this.subActionItems.get(i)).width / 2;
      ((FloatingActionMenu.Item)this.subActionItems.get(i)).y = (int)coords[1] - ((FloatingActionMenu.Item)this.subActionItems.get(i)).height / 2;
    }

  }

  public int getRadius() {
    return this.radius;
  }

  public ArrayList<FloatingActionMenu.Item> getSubActionItems() {
    return this.subActionItems;
  }

  public View getActivityContentView() {
    return ((Activity)this.mainActionView.getContext()).getWindow().getDecorView().findViewById(16908290);
  }

  private Point getScreenSize() {
    Point size = new Point();
    ((Activity)this.mainActionView.getContext()).getWindowManager().getDefaultDisplay().getSize(size);
    return size;
  }

  public void setStateChangeListener(FloatingActionMenu.MenuStateChangeListener listener) {
    this.stateChangeListener = listener;
  }

  public void setStartAngle(int startAngle) {
    this.startAngle = startAngle;
  }

  public void setEndAngle(int endAngle) {
    this.endAngle = endAngle;
  }

  public static class Builder {
    private int startAngle;
    private int endAngle;
    private int radius;
    private View actionView;
    private ArrayList<FloatingActionMenu.Item> subActionItems = new ArrayList();
    private MenuAnimationHandler animationHandler;
    private boolean animated;
    private FloatingActionMenu.MenuStateChangeListener stateChangeListener;

    public Builder(Activity activity) {
      this.radius = activity.getResources().getDimensionPixelSize(dimen.action_menu_radius);
      this.startAngle = 180;
      this.endAngle = 270;
      this.animationHandler = new DefaultAnimationHandler();
      this.animated = true;
    }

    public FloatingActionMenu.Builder setStartAngle(int startAngle) {
      this.startAngle = startAngle;
      return this;
    }

    public FloatingActionMenu.Builder setEndAngle(int endAngle) {
      this.endAngle = endAngle;
      return this;
    }

    public FloatingActionMenu.Builder setRadius(int radius) {
      this.radius = radius;
      return this;
    }

    public FloatingActionMenu.Builder addSubActionView(View subActionView, int width, int height) {
      this.subActionItems.add(new FloatingActionMenu.Item(subActionView, width, height));
      return this;
    }

    public FloatingActionMenu.Builder addSubActionView(View subActionView) {
      return this.addSubActionView(subActionView, 0, 0);
    }

    public FloatingActionMenu.Builder addSubActionView(int resId, Context context) {
      LayoutInflater inflater = (LayoutInflater)context.getSystemService("layout_inflater");
      View view = inflater.inflate(resId, (ViewGroup)null, false);
      view.measure(0, 0);
      return this.addSubActionView(view, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    public FloatingActionMenu.Builder setAnimationHandler(MenuAnimationHandler animationHandler) {
      this.animationHandler = animationHandler;
      return this;
    }

    public FloatingActionMenu.Builder enableAnimations() {
      this.animated = true;
      return this;
    }

    public FloatingActionMenu.Builder disableAnimations() {
      this.animated = false;
      return this;
    }

    public FloatingActionMenu.Builder setStateChangeListener(FloatingActionMenu.MenuStateChangeListener listener) {
      this.stateChangeListener = listener;
      return this;
    }

    public FloatingActionMenu.Builder attachTo(View actionView) {
      this.actionView = actionView;
      return this;
    }

    public FloatingActionMenu build() {
      return new FloatingActionMenu(this.actionView, this.startAngle, this.endAngle, this.radius, this.subActionItems, this.animationHandler, this.animated, this.stateChangeListener);
    }
  }

  public interface MenuStateChangeListener {
    void onMenuOpened(FloatingActionMenu var1);

    void onMenuClosed(FloatingActionMenu var1);
  }

  public static class Item {
    public int x;
    public int y;
    public int width;
    public int height;
    public View view;

    public Item(View view, int width, int height) {
      this.view = view;
      this.width = width;
      this.height = height;
      this.x = 0;
      this.y = 0;
    }
  }

  private class ItemViewQueueListener implements Runnable {
    private static final int MAX_TRIES = 10;
    private FloatingActionMenu.Item item;
    private int tries;

    public ItemViewQueueListener(FloatingActionMenu.Item item) {
      this.item = item;
      this.tries = 0;
    }

    public void run() {
      if (this.item.view.getMeasuredWidth() == 0 && this.tries < 10) {
        this.item.view.post(this);
      } else {
        this.item.width = this.item.view.getMeasuredWidth();
        this.item.height = this.item.view.getMeasuredHeight();
        this.item.view.setAlpha(1.0F);
        ((ViewGroup)FloatingActionMenu.this.getActivityContentView()).removeView(this.item.view);
      }
    }
  }

  public class ActionViewClickListener implements OnClickListener {
    public ActionViewClickListener() {
    }

    public void onClick(View v) {
      FloatingActionMenu.this.toggle(FloatingActionMenu.this.animated);
    }
  }
}
