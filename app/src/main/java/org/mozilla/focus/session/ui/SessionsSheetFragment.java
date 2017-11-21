/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.session.SessionManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.OneShotOnPreDrawListener;

public class SessionsSheetFragment extends LocaleAwareFragment implements View.OnClickListener {
    public static final String FRAGMENT_TAG = "tab_sheet";

    private static final int ANIMATION_DURATION = 200;

    private View backgroundView;
    private View cardView;
    private boolean isAnimating;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sessionssheet, container, false);

        backgroundView = view.findViewById(R.id.background);
        backgroundView.setOnClickListener(this);

        cardView = view.findViewById(R.id.card);
        cardView.getViewTreeObserver().addOnPreDrawListener(new OneShotOnPreDrawListener(cardView) {
            @Override
            protected void onPreDraw(View view) {
                playAnimation(false);
            }
        });

        final SessionsAdapter sessionsAdapter = new SessionsAdapter(this);
        SessionManager.getInstance().getSessions().observe(this, sessionsAdapter);

        final RecyclerView sessionView = view.findViewById(R.id.sessions);
        sessionView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        sessionView.setAdapter(sessionsAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(sessionView);

        return view;
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT ) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        public static final float ALPHA_FULL = 1.0f;

        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View itemView = viewHolder.itemView;

                Paint paint = new Paint();
                Bitmap icon;

                if (!(dX > 0)) {
                    icon = getBitmapFromVectorDrawable (getContext(), R.drawable.ic_delete);

                    int color = getContext().getResources().getColor(R.color.photonMagenta80);
                    paint.setColor(color);

                    c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                            (float) itemView.getRight(), (float) itemView.getBottom(), paint);

                    c.drawBitmap(icon,
                            (float) itemView.getRight() - convertDpToPx(16) - icon.getWidth(),
                            (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight()) / 2,
                            paint);
                }

                final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);

            } else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }

        private int convertDpToPx(int dp) {
            return Math.round(dp * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
            if (direction == ItemTouchHelper.LEFT) {
                final int deleteIndex = viewHolder.getAdapterPosition();
                SessionManager.getInstance().removeSessionByPosition(deleteIndex);
            }
        }
    };

    private Animator playAnimation(final boolean reverse) {
        isAnimating = true;

        final int offset = getResources().getDimensionPixelSize(R.dimen.floating_action_button_size) / 2;
        final int cx = cardView.getMeasuredWidth() - offset;
        final int cy = cardView.getMeasuredHeight() - offset;

        // The final radius is the diagonal of the card view -> sqrt(w^2 + h^2)
        final float fullRadius = (float) Math.sqrt(
                Math.pow(cardView.getWidth(), 2) + Math.pow(cardView.getHeight(), 2));

        final Animator sheetAnimator = ViewAnimationUtils.createCircularReveal(
                cardView, cx, cy, reverse ? fullRadius : 0, reverse ? 0 : fullRadius);
        sheetAnimator.setDuration(ANIMATION_DURATION);
        sheetAnimator.setInterpolator(new AccelerateInterpolator());
        sheetAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                cardView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;

                cardView.setVisibility(reverse ? View.GONE : View.VISIBLE);
            }
        });
        sheetAnimator.start();

        backgroundView.setAlpha(reverse ? 1f : 0f);
        backgroundView.animate()
                .alpha(reverse ? 0f : 1f)
                .setDuration(ANIMATION_DURATION)
                .start();

        return sheetAnimator;
    }

    /* package */ Animator animateAndDismiss() {
        final Animator animator = playAnimation(true);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                final MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .remove(SessionsSheetFragment.this)
                            .commit();
                }
            }
        });

        return animator;
    }

    public boolean onBackPressed() {
        animateAndDismiss();

        TelemetryWrapper.closeTabsTrayEvent();
        return true;
    }

    @Override
    public void applyLocale() {}

    @Override
    public void onClick(View view) {
        if (isAnimating) {
            // Ignore touched while we are animating
            return;
        }

        switch (view.getId()) {
            case R.id.background:
                animateAndDismiss();

                TelemetryWrapper.closeTabsTrayEvent();
                break;

            default:
                throw new IllegalStateException("Unhandled view in onClick()");
        }
    }
}
