package android.support.design.widget;

public class HeaderBehaviorHelper {

    public static void setHeaderTopBottomOffset(CoordinatorLayout coordinatorLayout,
                                                AppBarLayout appBarLayout,
                                                int offset) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        behavior.setHeaderTopBottomOffset(coordinatorLayout, appBarLayout,offset);
    }

}
