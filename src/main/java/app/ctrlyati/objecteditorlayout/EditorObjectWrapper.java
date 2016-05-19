package app.ctrlyati.objecteditorlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Yati on 06/04/2559.
 */
public class EditorObjectWrapper extends FrameLayout {

    public static final int FUNCTION_NONE = 0b00000;

    public static final int FUNCTION_MOVE_X = 0b00001;
    public static final int FUNCTION_MOVE_Y = 0b00010;
    public static final int FUNCTION_ROTATION = 0b00100;
    public static final int FUNCTION_SCALE_X = 0b01000;
    public static final int FUNCTION_SCALE_Y = 0b10000;

    public static final int FUNCTION_MOVE = 0b00011;
    public static final int FUNCTION_SCALE = 0b11000;
    public static final int FUNCTION_ALL = 0b11111;

    private int mAvailableFunction = FUNCTION_ALL;

    private Drawable mBorder;

    //
    // Construction
    //

    public EditorObjectWrapper(Context context) {
        this(context, null);
    }

    public EditorObjectWrapper(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditorObjectWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.EditorObjectWrapper, defStyleAttr,
                        0);

        mAvailableFunction =
                typedArray.getInt(R.styleable.EditorObjectWrapper_editor_function, FUNCTION_ALL);


        mBorder = typedArray.getDrawable(R.styleable.EditorObjectWrapper_border_drawable);
        if(mBorder == null){
            mBorder = ContextCompat.getDrawable(getContext(), R.drawable.border);
        }

        typedArray.recycle();
    }

    //
    // getter & setter
    //

    public Drawable getBorder() {
        return mBorder;
    }

    public void setBorder(Drawable border) {
        mBorder = border;
    }

    public int getAvailableFunctions() {
        return mAvailableFunction;
    }

    public void setAvailableFunction(int availableFunction) {
        mAvailableFunction = availableFunction;
    }

    public void showBorder(boolean visible) {

        if (visible) {
            Drawable border =
                    DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.border));
            //DrawableCompat.setTintMode(border, PorterDuff.Mode.SRC_ATOP);
            //DrawableCompat.setTint(border, mBorderColor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                setBackground(border);
            } else {
                setBackgroundDrawable(border);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                setBackground(null);
            } else {
                setBackgroundDrawable(null);
            }
        }
    }

    public void setHeight(int height) {

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = height;
        setLayoutParams(layoutParams);

        FrameLayout.LayoutParams childLayoutParams =
                (FrameLayout.LayoutParams) getChild().getLayoutParams();
        childLayoutParams.height = height;
        getChild().setLayoutParams(childLayoutParams);
    }

    public void setWidth(int width) {

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = width;
        setLayoutParams(layoutParams);

        FrameLayout.LayoutParams childLayoutParams =
                (FrameLayout.LayoutParams) getChild().getLayoutParams();
        childLayoutParams.width = width;
        getChild().setLayoutParams(childLayoutParams);
    }

    public View getChild() {
        return getChildAt(0);
    }
}
