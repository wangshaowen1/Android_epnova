package com.kuaigeglobal.epnova.widget.dragfooterview.footer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.kuaigeglobal.epnova.widget.dragfooterview.DimenUtil;
import com.kuaigeglobal.epnova.widget.dragfooterview.DragContainer;
import com.nhsd.basic.utils.Logger;

/**
 * Created by Administrator on 2016/11/17.
 */
public class BezierFooterDrawer extends BaseFooterDrawer {
    private DrawParams drawParams;

    private Paint rectPaint;
    private Paint bezierPaint;
    private TextPaint textPaint;
    private Path bezierPath;
    private RectF dragRect;

    private float[] bezierParams;
    private float[] iconParams;
    private StaticLayout normalTextLayout;
    private StaticLayout eventTextLayout;
    private StaticLayout textLayout;

    private IconRotateController iconRotateController;
    private float rotateThreshold;

    private static class DrawParams {
        int textIconGap;
        String normalString;
        String eventString;
        public int textColor;
        float textSize;
        Drawable iconDrawable;
        float iconSize;
        float bezierDragThreshold;
        float rectFooterThick;
    }

    private BezierFooterDrawer(Builder builder) {
        footerRegion = new RectF();

        drawParams = new DrawParams();
        Context context = builder.context;
        drawParams.textSize = DimenUtil.sp2px(context, builder.textSize);
        drawParams.textIconGap = DimenUtil.dp2px(context, builder.textIconGap);
        drawParams.textColor = builder.textColor;
        drawParams.normalString = builder.normalString;
        drawParams.eventString = builder.eventString;
        drawParams.iconDrawable = builder.iconDrawable;
        drawParams.iconSize = DimenUtil.dp2px(context, builder.iconSize);
        drawParams.bezierDragThreshold = DimenUtil.dp2px(context, builder.bezierDragThreshold);
        drawParams.rectFooterThick = DimenUtil.dp2px(context, builder.rectFooterThick);

        rotateThreshold = drawParams.bezierDragThreshold * 0.9f;

        iconRotateController = new IconRotateController(rotateThreshold, builder.iconRotateDuration);

        footerColor = builder.footerColor;

        //store bezier params, params[0] = sx,params[1] = sy, params[2] = cx,params[3] = cy, params[4] = ex,params[5] = ey
        //sx:bezier start point x
        //sy:bezier start point y
        //cx:bezier control point x
        //cy:bezier control point y
        //ex:bezier end point x
        //ey:bezier end point y
        bezierParams = new float[6];

        //store icon position: left, top, right, bottom
        iconParams = new float[4];

        initPaints();
        initTextLayouts();
    }

    private void initTextLayouts() {
        normalTextLayout = createTextLayout(drawParams.normalString);
        String eventText = drawParams.eventString;
        eventTextLayout = eventText == null || eventText.isEmpty() || eventText.equals(drawParams.normalString)
                ? normalTextLayout : createTextLayout(eventText);
    }

    private StaticLayout createTextLayout(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        // 按触发区域宽度排版并缓存，拖动时只移动、裁剪，避免每帧重新换行。
        int textWidth = Math.max(1, (int) (rotateThreshold - drawParams.textIconGap * 2));
        return StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .build();
    }

    private void initPaints() {
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(footerColor);
        rectPaint.setStyle(Paint.Style.FILL);

        dragRect = new RectF();

        bezierPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezierPaint.setColor(footerColor);
        bezierPaint.setStyle(Paint.Style.FILL);

        bezierPath = new Path();

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(drawParams.textColor);
        textPaint.setTextSize(drawParams.textSize);
    }

    @Override
    public boolean shouldTriggerEvent(float dragDistance) {
        return dragDistance > rotateThreshold;
    }

    @Override
    public void updateDragState(@DragContainer.DragState int dragState) {
        super.updateDragState(dragState);
        if (dragState == DragContainer.RELEASE) {
            iconRotateController.reset();
        }
    }

    @Override
    public void drawFooter(Canvas canvas, float left, float top, float right, float bottom) {
        Logger.d("drawFooter:size " + "" + left + "," + top + "," + right + "," + bottom);
        super.drawFooter(canvas, left, top, right, bottom);
        textLayout = getDraggedDistance() > rotateThreshold ? eventTextLayout : normalTextLayout;
        drawRect(canvas);
        drawBezier(canvas);
        drawText(canvas);
        drawIcon(canvas);
    }

    private void drawRect(Canvas canvas) {
        if (getDraggedDistance() >= drawParams.rectFooterThick) {
            dragRect.set(footerRegion.right - drawParams.rectFooterThick, 0, footerRegion.right, footerRegion.bottom);
        } else {
            dragRect.set(footerRegion.left, 0, footerRegion.right, footerRegion.bottom);
        }
        canvas.drawRect(dragRect, rectPaint);
    }

    private void drawBezier(Canvas canvas) {
        if (shouldDrawBezier()) {
            setBezierParams();
            bezierPath.reset();
            bezierPath.moveTo(bezierParams[0], bezierParams[1]);
            bezierPath.quadTo(bezierParams[2], bezierParams[3], bezierParams[4], bezierParams[5]);
            canvas.drawPath(bezierPath, bezierPaint);
        }
    }

    private void setBezierParams() {
        float sx = footerRegion.right - drawParams.rectFooterThick;
        float sy = 0;
        float cy = (footerRegion.bottom - footerRegion.top) / 2;
        float cx;
        if (getDraggedDistance() >= drawParams.bezierDragThreshold) {
            cx = footerRegion.right - drawParams.bezierDragThreshold;
        } else {
            cx = footerRegion.left;
        }
        float ex = footerRegion.right - drawParams.rectFooterThick;
        float ey = footerRegion.bottom;

        bezierParams[0] = sx;
        bezierParams[1] = sy;
        bezierParams[2] = cx;
        bezierParams[3] = cy;
        bezierParams[4] = ex;
        bezierParams[5] = ey;
    }

    private boolean shouldDrawBezier() {
        return getDraggedDistance() >= drawParams.rectFooterThick;
    }

    private void drawIcon(final Canvas canvas) {
        if (drawParams.iconDrawable == null) {
            return;
        }

        setIconPosParams();
        iconRotateController.calculateRotateDegree(dragState, getDraggedDistance());

        canvas.save();
        canvas.clipRect(footerRegion);
        float degree = iconRotateController.getRotateDegree();
        canvas.rotate(degree, getIconRotateX(drawParams.iconSize), (iconParams[1] + iconParams[3]) / 2);
        drawParams.iconDrawable.setBounds((int) iconParams[0], (int) iconParams[1], (int) iconParams[2], (int) iconParams[3]);
        drawParams.iconDrawable.draw(canvas);

        canvas.restore();
    }

    private float getIconRotateX(float drawableSize) {
        float left = iconParams[0];
        return left + drawableSize / 2;
    }

    private float getContentTop() {
        float contentHeight = textLayout == null ? 0 : textLayout.getHeight();
        if (drawParams.iconDrawable != null) {
            contentHeight += drawParams.iconSize;
            if (textLayout != null) {
                contentHeight += drawParams.textIconGap;
            }
        }
        // 箭头与整段多行文案作为整体在提示区域中垂直居中。
        return footerRegion.centerY() - contentHeight / 2;
    }


    //自己改的新样式  图片在文字顶部
    private void setIconPosParams() {
        float top = getContentTop();
        float bottom = top + drawParams.iconSize;
        float left = footerRegion.centerX() - drawParams.iconSize / 2;
        float right = left + drawParams.iconSize;

        iconParams[0] = left;
        iconParams[1] = top;
        iconParams[2] = right;
        iconParams[3] = bottom;
    }

    private float getDraggedDistance() {
        return footerRegion.right - footerRegion.left;
    }

    //自己改的新样式  图片在文字顶部
    private void drawText(Canvas canvas) {
        if (textLayout == null) {
            return;
        }
        float x = footerRegion.centerX() - textLayout.getWidth() / 2f;
        float y = getContentTop();
        if (drawParams.iconDrawable != null) {
            y += drawParams.iconSize + drawParams.textIconGap;
        }
        canvas.save();
        canvas.clipRect(footerRegion);
        canvas.translate(x, y);
        textLayout.draw(canvas);
        canvas.restore();
    }

    public static class Builder {
        private static final int DEFAULT_FOOTER_HEIGHT = 20;
        private static final int DEFAULT_BEZIER_DRAG_THRESHOLD = 70;//100
        private static final int DEFAULT_ICON_SIZE = 10;
        private static final int DEFAULT_TEXT_ICON_GAP = 4;
        private static final int DEFAULT_TEXT_SIZE = 10;
        private static final int DEFAULT_ICON_ROTATE_DURATION = 100;
        //        private static final int DEFAULT_TEXT_COLOR = 0xff222222;
//        private static final String DEFAULT_NORMAL_STRING = "释放查看";
//        private static final String DEFAULT_EVENT_STRING = "查看更多";
        private static final int DEFAULT_TEXT_COLOR = 0xffb5b5b5;
        private static final String DEFAULT_NORMAL_STRING = "左滑更多";
        private static final String DEFAULT_EVENT_STRING = "左滑更多";

        private Drawable iconDrawable;
        private int iconRotateDuration = DEFAULT_ICON_ROTATE_DURATION;
        private int textIconGap = DEFAULT_TEXT_ICON_GAP;
        private String normalString = DEFAULT_NORMAL_STRING;
        private String eventString = DEFAULT_EVENT_STRING;
        private int textColor = DEFAULT_TEXT_COLOR;
        private float textSize = DEFAULT_TEXT_SIZE;
        private float iconSize = DEFAULT_ICON_SIZE;
        public float bezierDragThreshold = DEFAULT_BEZIER_DRAG_THRESHOLD;
        public float rectFooterThick = DEFAULT_FOOTER_HEIGHT;

        public final int footerColor;
        public final Context context;

        public Builder(Context context, int footerColor) {
            this.footerColor = footerColor;
            this.context = context;
        }

        public Builder setIconRotateDuration(int iconRotateDuration) {
            this.iconRotateDuration = iconRotateDuration;
            return this;
        }

        public Builder setBezierDragThreshold(float bezierDragThreshold) {
            this.bezierDragThreshold = bezierDragThreshold;
            return this;
        }

        public Builder setRectFooterThick(float rectFooterThick) {
            this.rectFooterThick = rectFooterThick;
            return this;
        }

        public Builder setTextIconGap(int textIconGap) {
            this.textIconGap = textIconGap;
            return this;
        }

        public Builder setNormalString(String normalString) {
            this.normalString = normalString;
            return this;
        }

        public Builder setEventString(String eventString) {
            this.eventString = eventString;
            return this;
        }

        public Builder setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder setTextSize(float textSize) {
            this.textSize = textSize;
            return this;
        }

        public Builder setIconDrawable(Drawable iconDrawable) {
            this.iconDrawable = iconDrawable;
            return this;
        }

        public Builder setIconSize(float iconSize) {
            this.iconSize = iconSize;
            return this;
        }

        public BezierFooterDrawer build() {
            return new BezierFooterDrawer(this);
        }
    }
}
