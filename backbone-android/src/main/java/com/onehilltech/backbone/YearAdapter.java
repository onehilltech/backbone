package com.onehilltech.backbone;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Locale;

public class YearAdapter extends BaseAdapter
{
  private int firstYear_;

  private int lastYear_;

  private LayoutInflater inflater_;

  private int resource_;

  private int dropDownResource_;

  public YearAdapter (Context context, int resource, int firstYear, int lastYear)
  {
    this.resource_ = resource;
    this.inflater_ = LayoutInflater.from (context);
    this.firstYear_ = firstYear;
    this.lastYear_ = lastYear;
  }

  @Override
  public View getDropDownView (int i, View view, ViewGroup viewGroup)
  {
    TextView textView =
        view != null ?
            (TextView)view :
            (TextView)this.inflater_.inflate (this.dropDownResource_, viewGroup, false);

    textView.setText (String.format (Locale.ENGLISH, "%d", (this.firstYear_ + i)));

    return textView;
  }
  
  @Override
  public int getCount ()
  {
    return this.lastYear_ - this.firstYear_ + 1;
  }

  @Override
  public Object getItem (int i)
  {
    return this.firstYear_ + i;
  }

  @Override
  public long getItemId (int i)
  {
    return this.firstYear_ + i;
  }

  @Override
  public boolean hasStableIds ()
  {
    return true;
  }

  @Override
  public View getView (int i, View view, ViewGroup viewGroup)
  {
    TextView textView =
        view != null ?
            (TextView)view :
            (TextView)this.inflater_.inflate (this.resource_, viewGroup, false);

    textView.setText (String.format (Locale.ENGLISH, "%d", (this.firstYear_ + i)));

    return textView;
  }

  @Override
  public int getItemViewType (int i)
  {
    return 0;
  }

  @Override
  public int getViewTypeCount ()
  {
    return 1;
  }

  @Override
  public boolean isEmpty ()
  {
    return false;
  }

  public void setDropDownViewResource(@LayoutRes int resource)
  {
    this.dropDownResource_ = resource;
  }
}
