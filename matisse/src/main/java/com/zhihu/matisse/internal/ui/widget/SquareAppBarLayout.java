package com.zhihu.matisse.internal.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

public class SquareAppBarLayout extends android.support.design.widget.AppBarLayout {

    public SquareAppBarLayout(Context context) {
        super(context);
    }

    public SquareAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
