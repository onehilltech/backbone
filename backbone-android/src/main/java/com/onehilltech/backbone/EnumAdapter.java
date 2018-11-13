package com.onehilltech.backbone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.EnumSet;

import androidx.annotation.NonNull;

/**
 * Adapter designed for Java Enum types.
 *
 * @param <E>
 */
public class EnumAdapter <E extends Enum <E>>
    extends BaseAdapter
{
  private final LayoutInflater inflater_;

  private E [] values_;

  private int layoutId_;

  private Translator <E, String> valueTranslator_;

  /**
   * Create an EnumAdapter object from an Java Enum type.
   *
   * @param context
   * @param enumClass
   * @param layoutId
   * @param <E>
   * @return
   */
  @SuppressWarnings ("unchecked")
  public static <E extends Enum <E>> EnumAdapter <E> fromEnums (Context context, Class <E> enumClass, int layoutId)
  {
    try
    {
      Method valuesMethod = enumClass.getMethod ("values");
      E [] values = (E [])valuesMethod.invoke (null);

      return new EnumAdapter <> (context, enumClass, values, layoutId);
    }
    catch (Exception e)
    {
      throw new IllegalStateException ("Failed to create Enum adapter", e);
    }
  }

  @SuppressWarnings ("unchecked")
  public static <E extends Enum <E>> EnumAdapter <E> fromEnums (Context context, Class <E> enumClass, EnumSet <E> enums, int layoutId)
  {
    return new EnumAdapter <> (context, enumClass, (E [])enums.toArray (), layoutId);
  }

  public EnumAdapter (Context context, Class<E> enumClass, E [] values, int layoutId)
  {
    this.inflater_ = LayoutInflater.from (context);
    this.values_ = values;
    this.layoutId_ = layoutId;
  }

  /**
   * The the value translator to be used for the enums.
   *
   * @param translator
   */
  public void setValueTranslator (Translator <E, String> translator)
  {
    this.valueTranslator_ = translator;
  }

  @Override
  public E getItem (int i)
  {
    return this.values_[i];
  }

  @Override
  public int getCount ()
  {
    return this.values_.length;
  }

  @Override
  public long getItemId (int i)
  {
    return i;
  }

  @Override
  public View getView (int i, View view, @NonNull ViewGroup viewGroup)
  {
    TextView tv =
        view != null ?
            (TextView)view :
            (TextView)this.inflater_.inflate (this.layoutId_, viewGroup, false);

    E e = this.values_[i];
    String val = this.valueTranslator_ == null ? e.toString () : this.valueTranslator_.translate (e);
    tv.setText (val);

    return tv;
  }
}
