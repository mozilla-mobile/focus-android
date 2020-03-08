/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;

import android.util.Log;

import org.mozilla.focus.R;

public class FloatingEraseButton extends FloatingActionButton {
    private boolean keepHidden;

    public FloatingEraseButton(Context context) {
        super(context);
    }

    public FloatingEraseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingEraseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // range of movement
    private int rangeHeight;
    private int rangeWidth;
    // the start position of button
    private int startX;
    private int startY;
    // state of dragging
    private boolean isDrag;

    public void updateSessionsCount(int tabCount) {
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        final FloatingActionButtonBehavior behavior = (FloatingActionButtonBehavior) params.getBehavior();
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);

        keepHidden = tabCount != 1;

        if (behavior != null) {
            if (accessibilityManager != null && accessibilityManager.isTouchExplorationEnabled()) {
                // Always display erase button if Talk Back is enabled
                behavior.setEnabled(false);
            } else {
                behavior.setEnabled(!keepHidden);
            }
        }

        if (keepHidden) {
            setVisibility(View.GONE);
        }
    }

    @Override
    protected void onFinishInflate() {
        if (!keepHidden) {
            this.setVisibility(View.VISIBLE);
            this.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fab_reveal));
        }

        super.onFinishInflate();
    }

    @Override
    public void setVisibility(int visibility) {
        if (keepHidden && visibility == View.VISIBLE) {
            // There are multiple callbacks updating the visibility of the button. Let's make sure
            // we do not show the button if we do not want to.
            return;
        }

        if (visibility == View.VISIBLE) {
            show();
        } else {
            hide();
        }
    }

    // override onToucheVent so that button can listen the touch inputs (press/unpressed/drag)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // catch the touch position
        int rawX = (int) event.getRawX();
        int rawY = (int) event.getRawY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // press the button
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                isDrag = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                // save the start location of button
                startX = rawX;
                startY = rawY;
                // Gets the parent of this button which is the range of movement.
                ViewGroup parent;
                if (getParent() != null) {
                    parent = (ViewGroup) getParent();
                    // get the range of height and width
                    rangeHeight = parent.getHeight();
                    rangeWidth = parent.getWidth();
                }
                break;
            // dragging the button
            case MotionEvent.ACTION_MOVE:
                // if the range is valid then start drag the button else break
                if (rangeHeight <= 0 || rangeWidth == 0) {
                    isDrag = false;
                    break;
                } else {
                    isDrag = true;
                }
                // calculate the distance of x and y from start location
                int disX = rawX - startX;
                int disY = rawY - startY;
                int distance = (int) Math.sqrt(disX * disX + disY * disY);
                // special case if the distance is 0 end dragging set the state to false
                if (distance == 0) {
                    isDrag = false;
                    break;
                }
                // button size included
                float x = getX() + disX;
                float y = getY() + disY;
                // test if reached the edge: left up right down
                if (x < 0) {
                    x = 0;
                } else if (x > rangeWidth - getWidth()) {
                    x = rangeWidth - getWidth();
                }
                if (getY() < 0) {
                    y = 0;
                } else if (getY() + getHeight() > rangeHeight - 50) {
                    y = rangeHeight - getHeight() - 50;
                }
                // Set the position of the button after dragging
                setX(x);
                setY(y);
                // update the start position during dragging
                startX = rawX;
                startY = rawY;
                // Send a INFO log message and log the exception.
                // Log.i (tag, msg)
                // tag: Used to identify the source of a log message.
                // It usually identifies the class or activity where the log call occurs.
                Log.i("aa", "isDrag=" + isDrag + "getX=" + getX() + ";getY=" + getY() + ";parentWidth=" + rangeWidth);
                break;
            // unpressed button

            default:
                break;
        }
        // if drag then update session otherwise pass
        return super.onTouchEvent(event);
    }
}