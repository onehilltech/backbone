package com.onehilltech.backbone.materialize;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

public class MaterialEditText extends AppCompatEditText
{
  private final Paint linePaint_ = new Paint ();

  private String errorMessage_;
  private String label_;

  private final Paint textPaint_ = new Paint ();
  private final Paint labelPaint_ = new Paint ();

  public MaterialEditText (Context context)
  {
    super (context);
    this.init ();
  }

  public MaterialEditText (Context context, AttributeSet attrs)
  {
    super (context, attrs);
    this.init ();

    this.textPaint_.setTextSize (this.getResources ().getDimension (R.dimen.material_text_field_input_size));
    this.labelPaint_.setTextSize (this.getResources ().getDimension (R.dimen.material_text_field_label_size));
  }

  public MaterialEditText (Context context, AttributeSet attrs, int defStyleAttr)
  {
    super (context, attrs, defStyleAttr);
    this.init ();
  }

  public String getErrorMessage ()
  {
    return this.errorMessage_;
  }

  public void setErrorMessage (String errorMessage)
  {
    this.errorMessage_ = errorMessage;
  }

  private void init ()
  {
    this.linePaint_.setColor (Color.RED);
    this.linePaint_.setStrokeWidth (2.0f);

    // Hide the default material background because we need to write below the
    // underline.

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
      this.setBackground(null);
    else
      this.setBackgroundDrawable (null);
  }

  @Override
  protected void onDraw (Canvas canvas)
  {
    canvas.drawLine (0, this.getHeight (), this.getWidth (), this.getHeight (), this.linePaint_);

    super.onDraw (canvas);
  }

  @Override
  protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
  {
    super.onMeasure (widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  protected void onSizeChanged (int w, int h, int oldw, int oldh)
  {
    super.onSizeChanged (w, h, oldw, oldh);
  }

  private float getLabelTextHeight ()
  {
    Paint.FontMetrics metrics = this.labelPaint_.getFontMetrics ();
    return metrics.descent - metrics.ascent;
  }

  private float getInputTextHeight ()
  {
    Paint.FontMetrics metrics = this.textPaint_.getFontMetrics ();
    return metrics.descent - metrics.ascent;
  }
}
