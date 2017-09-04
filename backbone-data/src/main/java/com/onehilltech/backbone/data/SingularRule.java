package com.onehilltech.backbone.data;

import java.util.regex.Pattern;

/**
 * Created by hilljh on 9/3/17.
 */
class SingularRule
{
  private final Pattern pattern_;

  private final String replacement_;

  public SingularRule (Pattern pattern, String replacement)
  {
    this.pattern_ = pattern;
    this.replacement_ = replacement;
  }

  public Pattern getPattern ()
  {
    return this.pattern_;
  }

  public String getReplacement ()
  {
    return this.replacement_;
  }
}
