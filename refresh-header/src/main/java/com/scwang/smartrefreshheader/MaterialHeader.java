package com.scwang.smartrefreshheader;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import com.scwang.smartrefreshheader.material.CircleImageView;
import com.scwang.smartrefreshheader.material.MaterialProgressDrawable;
import com.scwang.smartrefreshlayout.api.RefreshHeader;
import com.scwang.smartrefreshlayout.api.SizeObserver;
import com.scwang.smartrefreshlayout.constant.RefreshState;
import com.scwang.smartrefreshlayout.constant.SpinnerStyle;
import com.scwang.smartrefreshlayout.util.DensityUtil;

import static android.view.View.MeasureSpec.getSize;

/**
 * Material 主题下拉头
 * Created by SCWANG on 2017/6/2.
 */

public class MaterialHeader extends ViewGroup implements RefreshHeader, SizeObserver {

    // Maps to ProgressBar.Large style
    public static final int SIZE_LARGE = 0;
    // Maps to ProgressBar default style
    public static final int SIZE_DEFAULT = 1;

    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    private static final float MAX_PROGRESS_ANGLE = .8f;
    @VisibleForTesting
    private static final int CIRCLE_DIAMETER = 40;
    @VisibleForTesting
    private static final int CIRCLE_DIAMETER_LARGE = 56;

    private boolean mFinished;
    private int mCircleDiameter;
    private CircleImageView mCircleView;
    private MaterialProgressDrawable mProgress;

    /**
     * 贝塞尔背景
     */
    private int mWaveHeight;
    private int mHeadHeight;
    private Path mBezierPath;
    private Paint mBezierPaint;
    private boolean mShowBezierWave = false;

    //<editor-fold desc="MaterialHeader">
    public MaterialHeader(Context context) {
        this(context,null);
    }

    public MaterialHeader(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MaterialHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setMinimumHeight(DensityUtil.dp2px(100));

        mProgress = new MaterialProgressDrawable(context, this);
        mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
        mProgress.setAlpha(255);
        mProgress.setColorSchemeColors(0xff0099cc,0xffff4444,0xff669900,0xffaa66cc,0xffff8800);
        mCircleView = new CircleImageView(context,CIRCLE_BG_LIGHT);
        mCircleView.setImageDrawable(mProgress);
        mCircleView.setVisibility(View.GONE);
        addView(mCircleView);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);

        mBezierPath = new Path();
        mBezierPaint = new Paint();
        mBezierPaint.setAntiAlias(true);
        mBezierPaint.setStyle(Paint.Style.FILL);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MaterialHeader);
        mShowBezierWave = ta.getBoolean(R.styleable.MaterialHeader_srlShowBezierWave, mShowBezierWave);
        mBezierPaint.setColor(ta.getColor(R.styleable.WaterDropHeader_srlPrimaryColor, 0xff11bbff));
        if (ta.hasValue(R.styleable.WaterDropHeader_srlShadowRadius)) {
            int radius = ta.getDimensionPixelOffset(R.styleable.MaterialHeader_srlShadowRadius, 0);
            int color = ta.getColor(R.styleable.MaterialHeader_srlShadowColor, 0xff000000);
            mBezierPaint.setShadowLayer(radius, 0, 0, color);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
        ta.recycle();

    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        params.height = -3;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getSize(widthMeasureSpec), getSize(heightMeasureSpec));
        mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleDiameter, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleDiameter, MeasureSpec.EXACTLY));
//        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
//                resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() == 0) {
            return;
        }
        final int width = getMeasuredWidth();
        int circleWidth = mCircleView.getMeasuredWidth();
        int circleHeight = mCircleView.getMeasuredHeight();

        if (isInEditMode() && mHeadHeight > 0) {
            int circleTop = mHeadHeight - circleHeight / 2;
            mCircleView.layout((width / 2 - circleWidth / 2), circleTop,
                    (width / 2 + circleWidth / 2), circleTop + circleHeight);

            mProgress.showArrow(true);
            mProgress.setStartEndTrim(0f, MAX_PROGRESS_ANGLE);
            mProgress.setArrowScale(1);
            mCircleView.setAlpha(1f);
            mCircleView.setVisibility(VISIBLE);
        } else {
            mCircleView.layout((width / 2 - circleWidth / 2), -mCircleDiameter,
                    (width / 2 + circleWidth / 2), circleHeight - mCircleDiameter);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mShowBezierWave) {
            //重置画笔
            mBezierPath.reset();
            mBezierPath.lineTo(0, mHeadHeight);
            //绘制贝塞尔曲线
            mBezierPath.quadTo(getMeasuredWidth() / 2, mHeadHeight + mWaveHeight * 1.9f, getMeasuredWidth(), mHeadHeight);
            mBezierPath.lineTo(getMeasuredWidth(), 0);
            canvas.drawPath(mBezierPath, mBezierPaint);
        }
        super.dispatchDraw(canvas);
    }

    //</editor-fold>

    /**
     * One of DEFAULT, or LARGE.
     */
    public void setSize(int size) {
        if (size != SIZE_LARGE && size != SIZE_DEFAULT) {
            return;
        }
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == SIZE_LARGE) {
            mCircleDiameter = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);
        }
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        mCircleView.setImageDrawable(null);
        mProgress.updateSizes(size);
        mCircleView.setImageDrawable(mProgress);
    }


    //<editor-fold desc="RefreshHeader">
    @Override
    public void onSizeDefined(int height, int extendHeight) {
        if (isInEditMode()) {
            mWaveHeight = mHeadHeight = height / 2;
        }
    }

    @Override
    public void onPullingDown(float percent, int offset, int headHeight, int extendHeight) {
        if (mShowBezierWave) {
            mHeadHeight = Math.min(offset, headHeight);
            mWaveHeight = Math.max(0, offset - headHeight);
            postInvalidate();
        }

        float originalDragPercent = 1f * offset / headHeight;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(offset) - headHeight;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, (float) headHeight * 2)
                / (float) headHeight);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float strokeStart = adjustedPercent * .8f;
        mProgress.showArrow(true);
        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
        mProgress.setArrowScale(Math.min(1f, adjustedPercent));

        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
        mProgress.setProgressRotation(rotation);
        mCircleView.setAlpha(Math.min(1f, originalDragPercent*2));

        float targetY = offset / 2 + mCircleDiameter / 2;
        mCircleView.setTranslationY(Math.min(offset, targetY));//setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop, true /* requires update */);
    }

    @Override
    public void onReleasing(float percent, int offset, int headHeight, int extendHeight) {
        if (!mProgress.isRunning() && !mFinished) {
            onPullingDown(percent, offset, headHeight, extendHeight);
        } else {
            if (mShowBezierWave) {
                mHeadHeight = Math.min(offset, headHeight);
                mWaveHeight = Math.max(0, offset - headHeight);
                postInvalidate();
            }
        }
    }

    @Override
    public void startAnimator(int headHeight, int extendHeight) {
        mProgress.start();
        if ((int) mCircleView.getTranslationY() != headHeight / 2 + mCircleDiameter / 2) {
            mCircleView.animate().translationY(headHeight / 2 + mCircleDiameter / 2);
        }
    }

    @Override
    public void onStateChanged(RefreshState state) {
        switch (state) {
            case None:
                break;
            case PullDownRefresh:
                mFinished = false;
                mCircleView.setVisibility(VISIBLE);
                mCircleView.setScaleX(1);
                mCircleView.setScaleY(1);
                break;
            case ReleaseRefresh:
                break;
            case Refreshing:
                break;
        }
    }

    @Override
    public void onFinish() {
        mProgress.stop();
        mCircleView.animate().scaleX(0).scaleY(0);
        mFinished = true;
    }

    @Override
    public void setPrimaryColors(int... colors) {
        if (colors.length > 0) {
            mBezierPaint.setColor(colors[0]);
        }
        //mProgress.setColorSchemeColors(colors);
    }

    public void setColorSchemeColors(int... colors) {
        mProgress.setColorSchemeColors(colors);
    }

    @NonNull
    @Override
    public View getView() {
        return this;
    }

    @Override
    public SpinnerStyle getSpinnerStyle() {
        return SpinnerStyle.FixedFront;
    }
    //</editor-fold>

}