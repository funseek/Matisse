package com.zhihu.matisse.internal.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.appbar.AppBarLayout;

public class SquareAppBarLayout extends AppBarLayout {

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
