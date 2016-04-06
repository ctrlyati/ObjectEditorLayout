package app.ctrlyati.objecteditorlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by Yati on 03/04/2559.
 */
public class EditorLayout extends RelativeLayout {

    private static final String TAG = "ObjectEditorLayout";
    private final boolean mShowSelecting;

    private TouchState mTouchState = TouchState.IDLE;

    private static final int CLICK_RADIUS = 20;

    private float[] mTouchStartPosition = new float[]{ 0f, 0f };
    private float[] mObjectStartPosition = new float[]{ 0f, 0f };

    private EditorObjectWrapper mSelectingChild;
    private EditorObjectWrapper mTouchStatObject;

    private ScaleGestureDetector mScaleGestureDetector;

    private enum TouchState {
        IDLE,
        DOWN,
        DRAG,
    }

    public EditorLayout(Context context) {
        this(context, null);
    }

    public EditorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.EditorObjectWrapper, defStyleAttr,
                        0);

        mShowSelecting =
                typedArray.getBoolean(R.styleable.EditorObjectWrapper_editor_function, true);

        typedArray.recycle();

        mScaleGestureDetector = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.OnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        Log.d(TAG, "zoom ongoing, scale: " + detector.getScaleFactor());
                        return true;
                    }

                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        return false;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {

                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //if (mScaleGestureDetector.onTouchEvent(event)) {
        //mTouchState = TouchState.IDLE;
        //}

        if (event.getPointerCount() < 2) {

            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            event.getPointerCoords(event.getPointerCount() - 1, pointerCoords);

            if (mTouchState == TouchState.IDLE) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    mTouchStartPosition[0] = pointerCoords.x;
                    mTouchStartPosition[1] = pointerCoords.y;

                    if (mSelectingChild != null) {
                        mObjectStartPosition[0] = mSelectingChild.getX();
                        mObjectStartPosition[1] = mSelectingChild.getY();
                    }

                    mTouchStatObject =
                            getChildAtCoords(mTouchStartPosition[0], mTouchStartPosition[1]);

                    mTouchState = TouchState.DOWN;
                    Log.d(TAG, "down...("
                            + mTouchStartPosition[0]
                            + ","
                            + mTouchStartPosition[1]
                            + ")");
                    return true;
                }
            } else if (mTouchState == TouchState.DOWN) {

                if (event.getAction() == MotionEvent.ACTION_UP) {

                    float radiusX = Math.abs(pointerCoords.x - mTouchStartPosition[0]);
                    float radiusY = Math.abs(pointerCoords.y - mTouchStartPosition[1]);

                    if (CLICK_RADIUS >= Math.sqrt(radiusX * radiusX + radiusY * radiusY)) {

                        if (mSelectingChild != null && mShowSelecting) {
                            mSelectingChild.showBorder(false);
                        }

                        mSelectingChild =
                                getChildAtCoords(mTouchStartPosition[0], mTouchStartPosition[1]);

                        if (mSelectingChild == null) {
                            mTouchState = TouchState.IDLE;
                            return false;
                        }

                        if (mShowSelecting) {
                            mSelectingChild.showBorder(true);
                        }

                        Log.d(TAG, "click...("
                                + mTouchStartPosition[0]
                                + ","
                                + mTouchStartPosition[1]
                                + ")");

                        mTouchStartPosition = new float[]{ 0f, 0f };

                        mTouchState = TouchState.IDLE;
                        return true;
                    } else {

                        mTouchState = TouchState.IDLE;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

                    float radiusX = Math.abs(pointerCoords.x - mTouchStartPosition[0]);
                    float radiusY = Math.abs(pointerCoords.y - mTouchStartPosition[1]);

                    if (CLICK_RADIUS < Math.sqrt(radiusX * radiusX + radiusY * radiusY)) {

                        Log.d(TAG, "start drag...("
                                + mTouchStartPosition[0]
                                + ","
                                + mTouchStartPosition[1]
                                + ")");
                        mTouchState = TouchState.DRAG;
                        return true;
                    }
                } else {
                    mTouchState = TouchState.IDLE;
                }
            } else if (mTouchState == TouchState.DRAG) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {

                    float dragX = pointerCoords.x;
                    float dragY = pointerCoords.y;

                    if (mSelectingChild == mTouchStatObject && mSelectingChild != null) {

                        float newX = mObjectStartPosition[0] + (dragX - mTouchStartPosition[0]);
                        float newY = mObjectStartPosition[1] + (dragY - mTouchStartPosition[1]);

                        //mSelectingChild.setX(dragX - mSelectingChild.getWidth()/2);
                        //mSelectingChild.setY(dragY - mSelectingChild.getHeight()/2);

                        int availableFunctions = mSelectingChild.getAvailableFunctions();

                        if ((availableFunctions & EditorObjectWrapper.FUNCTION_MOVE_X)
                                == EditorObjectWrapper.FUNCTION_MOVE_X) {
                            mSelectingChild.setX(newX);
                        }

                        if ((availableFunctions & EditorObjectWrapper.FUNCTION_MOVE_Y)
                                == EditorObjectWrapper.FUNCTION_MOVE_Y) {
                            mSelectingChild.setY(newY);
                        }

                        return true;
                    }

                    return false;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {

                    Log.d(TAG, "stop drag...("
                            + mTouchStartPosition[0]
                            + ","
                            + mTouchStartPosition[1]
                            + ")");

                    mTouchState = TouchState.IDLE;
                    return true;
                }
            } else {

                mTouchState = TouchState.IDLE;
            }
        } else {

        }

        return super.onTouchEvent(event);
    }

    private EditorObjectWrapper getChildAtCoords(float pointerX, float pointerY) {
        for (int i = getChildCount() - 1; i >= 0; i--) {

            if (!(getChildAt(i) instanceof EditorObjectWrapper)) {
                continue;
            }

            EditorObjectWrapper child = (EditorObjectWrapper) getChildAt(i);

            float childX = child.getX();
            float childY = child.getY();

            float ccx = childX + child.getWidth() / 2;
            float ccy = childY + child.getHeight() / 2;

            float dx = Math.abs(ccx - pointerX);
            float dy = Math.abs(ccy - pointerY);

            if (dx < child.getWidth() / 2 && dy < child.getHeight() / 2) {
                Log.d(TAG, "getChildAtCoords " + i);

                removeView(child);
                addView(child);

                return child;
            }
        }
        return null;
    }

    public void deselectAll() {

        mTouchState = TouchState.IDLE;
        mSelectingChild = null;

        for (int i = 0; i < getChildCount(); i++) {
            if (!(getChildAt(i) instanceof EditorObjectWrapper)) {
                continue;
            }

            ((EditorObjectWrapper) getChildAt(i)).showBorder(false);
        }
    }

    //
    // add views
    //

    @Override
    public void addView(View child) {

        if (!(child instanceof EditorObjectWrapper)) {

            EditorObjectWrapper wrapper = new EditorObjectWrapper(getContext());
            wrapper.addView(child);

            super.addView(wrapper);
        } else {
            super.addView(child);
        }
    }

    @Override
    public void addView(View child, int index) {

        if (!(child instanceof EditorObjectWrapper)) {

            EditorObjectWrapper wrapper = new EditorObjectWrapper(getContext());
            wrapper.addView(child);

            super.addView(wrapper, index);
        } else {

            super.addView(child, index);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {

        if (!(child instanceof EditorObjectWrapper)) {

            EditorObjectWrapper wrapper = new EditorObjectWrapper(getContext());
            wrapper.addView(child, params);

            super.addView(wrapper, index, params);
        } else {
            super.addView(child, index, params);
        }
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (!(child instanceof EditorObjectWrapper)) {

            EditorObjectWrapper wrapper = new EditorObjectWrapper(getContext());
            wrapper.addView(child, params);

            super.addView(wrapper, params);
        } else {
            super.addView(child, params);
        }
    }

    @Override
    public void addView(View child, int width, int height) {

        if (!(child instanceof EditorObjectWrapper)) {

            EditorObjectWrapper wrapper = new EditorObjectWrapper(getContext());
            wrapper.addView(child, width, height);

            super.addView(wrapper, width, height);
        } else {
            super.addView(child, width, height);
        }
    }
}
