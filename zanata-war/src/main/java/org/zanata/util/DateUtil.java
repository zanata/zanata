/**
 * 
 */
package org.zanata.util;

import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.ocpsoft.prettytime.PrettyTime;


/**
 * 
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 * 
 */
public class DateUtil
{
   private final static String DATE_TIME_SHORT_PATTERN = "dd/MM/yy HH:mm";
   private final static String TIME_SHORT_PATTERN = "hh:mm:ss";

   /**
    * Format date to dd/MM/yy hh:mm a
    * @param date
    * @return
    */
   public static String formatShortDate(Date date)
   {
      if(date != null)
      {
         DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_TIME_SHORT_PATTERN);
         return fmt.print(new DateTime(date));
      }
      return null;
   }
   
   /**
    * Format date to hh:mm:ss
    * @param date
    * @return
    */
   public static String formatTime(Date date)
   {
      if(date != null)
      {
         DateTimeFormatter fmt = DateTimeFormat.forPattern(TIME_SHORT_PATTERN);
         return fmt.print(new DateTime(date));
      }
      return null;
   }
   
   /**
    * Return readable string of time different compare between 'then' and current time
    * e.g 10 minutes ago, 1 hour ago
    * @param from
    * @param then
    * @return
    */
   public static String getHowLongAgoDescription(Date then)
   {
      Locale locale = Locale.getDefault();
      PrettyTime p = new PrettyTime(locale);
      return p.formatUnrounded(then);
   }
}
