package app.ctrlyati.objecteditorlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Yati on 03/04/2559.
 */
public class EditorLayout extends RelativeLayout {

    private static final String TAG = "ObjectEditorLayout";
    private final boolean mIsShowSelecting;
    private final boolean mIsSupportMultiTouch;

    private TouchState mTouchState = TouchState.IDLE;

    private static final int CLICK_RADIUS = 20;

    private int mSmallestObjectSize;

    private float mMultiTouchStartSize = 0f;
    private float[] mObjectStartSize = new float[]{ 0f, 0f };

    private double mMultiTouchStartRotation = 0f;
    private float mObjectStartRotation = 0;

    private float[] mMultiTouchStartPosition = new float[]{
            0f, 0f
    };
    private float[] mTouchStartPosition = new float[]{ 0f, 0f };

    private float[] mObjectStartPosition = new float[]{ 0f, 0f };
    private EditorObjectWrapper mSelectingChild;

    private EditorObjectWrapper mTouchStartObject;
    private ScaleGestureDetector mScaleGestureDetector;

    private enum TouchState {
        IDLE,
        DOWN,
        DRAG,
        MULTI_DOWN,
        MULTI_DRAG,
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

        mIsShowSelecting =
                typedArray.getBoolean(R.styleable.EditorObjectWrapper_editor_function, true);
        mIsSupportMultiTouch =
                typedArray.getBoolean(R.styleable.EditorLayout_multitouch_support, true);

        typedArray.recycle();

        mSmallestObjectSize =
                context.getResources().getDimensionPixelSize(R.dimen.smallest_object_size);

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

        if (!isEnabled()) {
            mSelectingChild = null;
            return false;
        }

        if (event.getPointerCount() < 2) {

            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            event.getPointerCoords(event.getPointerCount() - 1, pointerCoords);

            if (mTouchState == TouchState.IDLE) {

                if (event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_MOVE) {

                    // when the first touch down
                    // and make sure if dragging while state lost

                    mTouchStartPosition[0] = pointerCoords.x;
                    mTouchStartPosition[1] = pointerCoords.y;

                    //if (mSelectingChild != null) {
                    //mObjectStartPosition[0] = mSelectingChild.getX();
                    //mObjectStartPosition[1] = mSelectingChild.getY();
                    //} else {

                    mTouchStartObject =
                            getChildAtCoords(mTouchStartPosition[0], mTouchStartPosition[1]);
                    //}

                    if (mSelectingChild != null) {
                        mSelectingChild.showBorder(false);
                    }
                    mSelectingChild = mTouchStartObject;

                    if (mSelectingChild != null) {
                        //mSelectingChild.showBorder(true);
                        mObjectStartPosition[0] = mSelectingChild.getX();
                        mObjectStartPosition[1] = mSelectingChild.getY();

                        mTouchState = TouchState.DOWN;
                        return true;
                    }
                }
            } else if (mTouchState == TouchState.DOWN) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    float radiusX = Math.abs(pointerCoords.x - mTouchStartPosition[0]);
                    float radiusY = Math.abs(pointerCoords.y - mTouchStartPosition[1]);

                    if (CLICK_RADIUS >= Math.sqrt(radiusX * radiusX + radiusY * radiusY)) {

                        // trying to called it 'click'

                        if (mSelectingChild != null && !mIsShowSelecting) {
                            mSelectingChild.showBorder(false);
                        }

                        mSelectingChild =
                                getChildAtCoords(mTouchStartPosition[0], mTouchStartPosition[1]);

                        if (mSelectingChild == null) {

                            // click on nothing
                            mTouchState = TouchState.IDLE;
                            return false;
                        }

                        // clicking done

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

                        // start drag

                        mTouchState = TouchState.DRAG;
                        return true;
                    }
                } else {

                    // lost state, make it idle

                    mTouchState = TouchState.IDLE;
                }
            } else if (mTouchState == TouchState.DRAG) {

                if (event.getAction() == MotionEvent.ACTION_MOVE) {

                    // drag

                    float dragX = pointerCoords.x;
                    float dragY = pointerCoords.y;

                    // add this line to the if below to make it drag when it's start
                    // touching on the object that you want to drag
                    //if (mSelectingChild != mTouchStartObject) {
                    //    if (mSelectingChild != null) {
                    //        mSelectingChild.showBorder(false);
                    //    }
                    //    mSelectingChild = mTouchStartObject;
                    //    mSelectingChild.showBorder(true);
                    //}

                    if (mSelectingChild != null) {

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

                    mTouchState = TouchState.IDLE;
                    return true;
                }
            } else {

                mTouchState = TouchState.IDLE;
            }
        } else if (mIsSupportMultiTouch && event.getPointerCount() == 2) {

            if (mTouchState != TouchState.MULTI_DOWN && mTouchState != TouchState.MULTI_DRAG) {
                mTouchState = TouchState.MULTI_DOWN;

                return true;
            } else if (mTouchState == TouchState.MULTI_DOWN) {

                if (mSelectingChild != null) {
                    mObjectStartPosition[0] = mSelectingChild.getX();
                    mObjectStartPosition[1] = mSelectingChild.getY();
                }

                MotionEvent.PointerCoords pointerCoordsA = new MotionEvent.PointerCoords();
                MotionEvent.PointerCoords pointerCoordsB = new MotionEvent.PointerCoords();
                event.getPointerCoords(0, pointerCoordsA);
                event.getPointerCoords(1, pointerCoordsB);

                mMultiTouchStartPosition[0] = (pointerCoordsA.x + pointerCoordsB.x) / 2f;
                mMultiTouchStartPosition[1] = (pointerCoordsA.y + pointerCoordsB.y) / 2f;

                // rotation

                double dy = pointerCoordsA.y - pointerCoordsB.y;
                double dx = pointerCoordsA.x - pointerCoordsB.x;
                mMultiTouchStartRotation = Math.atan2(dy, dx);
                if (mSelectingChild != null) {
                    mObjectStartRotation = mSelectingChild.getRotation();
                }

                // scale & size

                mMultiTouchStartSize = Double.valueOf(Math.sqrt(dx * dx + dy * dy)).floatValue();
                if (mSelectingChild != null) {
                    mObjectStartSize[0] = mSelectingChild.getWidth();
                    mObjectStartSize[1] = mSelectingChild.getHeight();
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    mTouchState = TouchState.MULTI_DRAG;
                }

                return true;
            } else {
                // if (mTouchState == TouchState.MULTI_DRAG) is not necessary

                if (mSelectingChild != null) {

                    int sw = getWidth() < getHeight() ? getWidth() : getHeight();

                    MotionEvent.PointerCoords pointerCoordsA = new MotionEvent.PointerCoords();
                    MotionEvent.PointerCoords pointerCoordsB = new MotionEvent.PointerCoords();
                    event.getPointerCoords(0, pointerCoordsA);
                    event.getPointerCoords(1, pointerCoordsB);

                    float[] newTouchPosition = new float[]{
                            (pointerCoordsA.x + pointerCoordsB.x) / 2f,
                            (pointerCoordsA.y + pointerCoordsB.y) / 2f
                    };

                    float newX = mObjectStartPosition[0] + (newTouchPosition[0]
                            - mMultiTouchStartPosition[0]);
                    float newY = mObjectStartPosition[1] + (newTouchPosition[1]
                            - mMultiTouchStartPosition[1]);

                    double dy = pointerCoordsA.y - pointerCoordsB.y;
                    double dx = pointerCoordsA.x - pointerCoordsB.x;
                    double newRotation = Math.atan2(dy, dx);

                    float dr = Double.valueOf(mObjectStartRotation
                            + (newRotation - mMultiTouchStartRotation) * 180 / Math.PI)
                            .floatValue();

                    float whRatio = mObjectStartSize[0] / mObjectStartSize[1];

                    float newSize = Double.valueOf(Math.sqrt(dx * dx + dy * dy)).floatValue();
                    float ds = newSize - mMultiTouchStartSize;

                    float dsx = mObjectStartSize[0] + (ds * whRatio);
                    float dsy = mObjectStartSize[1] + (ds / whRatio);

                    dsx = dsx > mSmallestObjectSize ? dsx : mSmallestObjectSize;
                    dsy = dsy > mSmallestObjectSize ? dsy : mSmallestObjectSize;

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

                    if ((availableFunctions & EditorObjectWrapper.FUNCTION_ROTATION)
                            == EditorObjectWrapper.FUNCTION_ROTATION) {

                        mSelectingChild.setRotation(dr);
                    }

                    if ((availableFunctions & EditorObjectWrapper.FUNCTION_SCALE_X)
                            == EditorObjectWrapper.FUNCTION_SCALE_X) {

                        mSelectingChild.setWidth(dsx < sw ? Math.round(dsx) : sw);
                    }

                    if ((availableFunctions & EditorObjectWrapper.FUNCTION_SCALE_Y)
                            == EditorObjectWrapper.FUNCTION_SCALE_Y) {

                        mSelectingChild.setHeight(dsy < sw ? Math.round(dsy) : sw);
                    }
                }

                return true;
            }
        }

//        return super.onTouchEvent(event);
        return false;
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

    public File captureView(File oldFile) {

        //Create a Bitmap with the same dimensions
        Bitmap image =
                Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
        //Draw the view inside the Bitmap
        this.draw(new Canvas(image));

        //Store to sdcard
        try {
            String filename = "edited_" + oldFile.getName();
            String path = oldFile.getParent();
            File file = new File(path, filename);
            FileOutputStream out = new FileOutputStream(file);

            image.compress(Bitmap.CompressFormat.PNG, 90, out); //Output

            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
