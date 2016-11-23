package com.lapism.searchview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.HeaderBehaviorHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.lapism.searchview.SearchView;

@SuppressWarnings("unused")
public class FloatingSearchViewBehaviour extends CoordinatorLayout.Behavior<SearchView> {

    private final String TAG = getClass().getSimpleName();

    private AppBarLayout appBarLayout;
    private AppBarLayout.Behavior appBarBehavior;
    private ValueAnimator valueAnimator;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private boolean isScrolling;

    public FloatingSearchViewBehaviour() {
        super();
    }

    @SuppressWarnings("unused")
    public FloatingSearchViewBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void fixStateListAnimator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // A bug that makes the floating search view disappear
            this.appBarLayout.setStateListAnimator(null);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // CRASH ON LOLIPOP
//            this.appBarLayout.setElevation(0);
        }
    }

    /***
     * If search view is open, we want it to handle all touchs events
     * @param parent
     */
    private void setTouchListener(final CoordinatorLayout parent) {
        parent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (searchView.isSearchOpen()) {
                    parent.requestDisallowInterceptTouchEvent(true);
                    return recyclerView.onTouchEvent(event);
                } else {
                    parent.requestDisallowInterceptTouchEvent(false);
                    return view.onTouchEvent(event);
                }
            }
        });
    }

    private void configureRecyclerView() {
        this.recyclerView = (RecyclerView) searchView.findViewById(com.lapism.searchview.R.id.recyclerView_result);
        this.recyclerView.setNestedScrollingEnabled(false);

    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, SearchView child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            this.searchView = child;
            this.appBarLayout = (AppBarLayout)dependency;
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                    appBarLayout.getLayoutParams();
            this.appBarBehavior = (AppBarLayout.Behavior) params.getBehavior();
            this.configureRecyclerView();
            this.fixStateListAnimator();
            this.setTouchListener(parent);
            return true;
        }
        return super.layoutDependsOn(parent, child, dependency);
    }


    @Override
    public void onNestedPreScroll(CoordinatorLayout parent, SearchView child, View target,
                                  int dx, int dy, int[] consumed) {
        // We can just ignore down scroll
        if (dy >= 0 || dy > -10 || this.isScrolling) {
            return;
        }
        this.isScrolling = true;
        if (needsToAdjustSearchBar() && !isRunningAnimation()) {
            int offset = getMinExpandHeight();
            this.getValueAnimator(parent, child, -offset).start();
        }
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, SearchView child, View target) {
        this.isScrolling = false;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                       SearchView child, View directTargetChild, View target,
                                       int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target,
                        nestedScrollAxes);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent,
                                          SearchView child, View dependency) {
        if (needsToAdjustSearchBar()) {
            float offset = getMinExpandHeight() + appBarBehavior.getTopAndBottomOffset();
            child.setY(offset);
            return true;
        }
        return super.onDependentViewChanged(parent, child, dependency);
    }

    // Convenience

    private int getStatusBarHeight() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            return 0;
        }
        int result = 0;
        int resourceId = searchView.getContext().getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = searchView.getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private ValueAnimator getValueAnimator(final CoordinatorLayout parent,
                                           SearchView searchView, int offset) {
        if (valueAnimator == null) {
            valueAnimator = ValueAnimator.ofInt();
        } else if (valueAnimator.isRunning()) {
            return valueAnimator;
        }
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                HeaderBehaviorHelper.setHeaderTopBottomOffset(parent, appBarLayout,
                        (int) animation.getAnimatedValue());
            }
        });

        valueAnimator.setIntValues(appBarBehavior.getTopAndBottomOffset(), offset);
        return valueAnimator;
    }

    private boolean isRunningAnimation() {
        return valueAnimator != null && valueAnimator.isRunning();
    }

    private boolean needsToAdjustSearchBar() {
        float y = Math.abs(appBarBehavior.getTopAndBottomOffset());
        return y > getMinExpandHeight();
    }

    private int getMinExpandHeight() {
        return appBarLayout.getTotalScrollRange() - searchView.getMinimumHeight()
                -(getStatusBarHeight() / 2);
    }
}
