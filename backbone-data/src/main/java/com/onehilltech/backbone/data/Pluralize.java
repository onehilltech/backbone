package com.onehilltech.backbone.data;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @class Pluralize
 *
 * Partial port for pluralize NodeJS module (https://github.com/blakeembrey/pluralize).
 */
public class Pluralize
{
  private final ArrayList <SingularRule> singularRules_ = new ArrayList<> ();

  private static Pluralize instance_;

  /**
   * Get the singleton instance.
   *
   * @return Pluralize object
   */
  public static Pluralize getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new Pluralize ();
    return instance_;
  }

  /**
   * Default constructor.
   */
  public Pluralize ()
  {
    for (String [] rule : SINGULARIZATION_RULES)
      this.addSingularRule (rule[0], rule[1]);
  }

  /**
   * Add a singular rule.
   *
   * @param regex
   * @param replacement
   */
  public void addSingularRule (String regex, String replacement)
  {
    Pattern pattern = Pattern.compile (regex, Pattern.CASE_INSENSITIVE);
    this.addSingularRule (pattern, replacement);
  }

  /**
   * Add a singular rule.
   *
   * @param pattern
   * @param replacement
   */
  public void addSingularRule (Pattern pattern, String replacement)
  {
    this.singularRules_.add (new SingularRule (pattern, replacement));
  }

  /**
   * Convert a string to its signular form.
   * @param s
   * @return
   */
  public String singular (String s)
  {
    for (int i = this.singularRules_.size () - 1; i >= 0; -- i)
    {
      SingularRule rule = this.singularRules_.get (i);
      Matcher matcher = rule.getPattern ().matcher (s);

      if (matcher.find ())
        return matcher.replaceAll (rule.getReplacement ());
    }

    return s;
  }

  // Built-in singularization rules

  private static final String [][] SINGULARIZATION_RULES = {
      {"s$", ""},
      {"(ss)$", "$1"},
      {"(wi|kni|(?:after|half|high|low|mid|non|night|[^\\w]|^)li)ves$", "$1fe"},
      {"(ar|(?:wo|[ae])l|[eo][ao])ves$", "$1f"},
      {"ies$", "y"},
      {"\\b([pl]|zomb|(?:neck|cross)?t|coll|faer|food|gen|goon|group|lass|talk|goal|cut)ies$", "$1ie"},
      {"\\b(mon|smil)ies$", "$1ey"},
      {"(m|l)ice$", "$1ouse"},
      {"(seraph|cherub)im$", "$1"},
      {"(x|ch|ss|sh|zz|tto|go|cho|alias|[^aou]us|tlas|gas|(?:her|at|gr)o|ris)(?:es)?$", "$1"},
      {"(analy|ba|diagno|parenthe|progno|synop|the|empha|cri)(?:sis|ses)$", "$1sis"},
      {"(movie|twelve|abuse|e[mn]u)s$", "$1"},
      {"(test)(?:is|es)$", "$1is"},
      {"(alumn|syllab|octop|vir|radi|nucle|fung|cact|stimul|termin|bacill|foc|uter|loc|strat)(?:us|i)$", "$1us"},
      {"(agend|addend|millenni|dat|extrem|bacteri|desiderat|strat|candelabr|errat|ov|symposi|curricul|quor)a$", "$1um"},
      {"(apheli|hyperbat|periheli|asyndet|noumen|phenomen|criteri|organ|prolegomen|hedr|automat)a$", "$1on"},
      {"(alumn|alg|vertebr)ae$", "$1a"},
      {"(cod|mur|sil|vert|ind)ices$", "$1ex"},
      {"(matr|append)ices$", "$1ix"},
      {"(pe)(rson|ople)$", "$1rson"},
      {"(child)ren$", "$1"},
      {"(eau)x?$", "$1"},
      {"men$", "man"}
  };

}
