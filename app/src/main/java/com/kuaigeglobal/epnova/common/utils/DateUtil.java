package com.kuaigeglobal.epnova.common.utils;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;

import com.nhsd.basic.utils.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * handle date and datetime
 * <p>
 * Notice: All of the time operating here, please bsn
 */
public class DateUtil {
    private static final long ONE_MINUTE = 60;
    private static final long ONE_HOUR = 3600;
    private static final long ONE_DAY = 86400;
    private static final long ONE_MONTH = 2592000;
    private static final long ONE_YEAR = 31104000;

    public static final String FORMAT_CN_DATE = "MM月dd日 HH:mm";//2016-11-20 16:08:43
    public static final String FORMAT_CN_DATE2 = "MM月dd日 EEEE HH:mm";//4月5日 星期五 12:00
    public static final String FORMAT_CN_DATE4 = "MM月dd日 E HH:mm";//4月5日 周五 12:00
    public static final String FORMAT_CN_DATE3 = "yyyy年MM月dd日 E HH:mm";//4月5日 周五 12:00
    public static final String FORMAT_DATETIME = "yyyy-MM-dd HH:mm:ss";//2016-11-20 16:08:43
    public static final String FORMAT_DATETIME2 = "yyyy/MM/dd HH:mm:ss";//2016-11-20 16:08:43
    public static final String FORMAT_DATETIME_BIAS = "yyyy-MM-dd HH:mm";//2016-11-20 16:08:43
    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_DATE2 = "yyyy.MM.dd";
    public static final String FORMAT_DATE3 = "yyyy/MM/dd";
    public static final String FORMAT_DATE4 = "yyyy/MM/dd HH:mm";
    public static final String FORMAT_DATE5 = "yyyy.MM.dd HH:mm";
    public static final String FORMAT_MONTH_DAY = "MM月dd日";
    public static final String FORMAT_MONTH_DAY_TIME = "MM月dd日 HH:mm";
    public static final String FORMAT_CN_DAY = "yyyy年MM月dd日";
    private static final int MINUTE = 1000 * 60;
    private static final String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};//定义数组

    public static String convertDateTimeToDate(String dateTime) {
        return format(parse(dateTime, DateUtil.FORMAT_DATETIME), DateUtil.FORMAT_DATE);
    }

    /**
     * formate date
     *
     * @param d
     * @param format
     * @return
     */
    public static String format(Date d, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(d);
    }

    /**
     * formate time
     *
     * @param time
     * @param format
     * @return
     */
    public static String format(long time, String format) {
        return format(new Date(time), format);
    }

    public static Date parse(String s, String format) {
        try {
            if (TextUtils.isEmpty(s)) return null;
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.parse(s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static int getYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR);
    }

    public static int getMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH);
    }

    public static int getDayOfMOnth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_MONTH);
    }


    public static int getMonth(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.MONTH);
    }

    public static int getDayOfMOnth(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static boolean isToday(long dateTime) {
        return isSameDay(dateTime,System.currentTimeMillis());
    }

    public static boolean isSameDay(long dateTimeA,long dateTimeB) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateTimeA);
        int year=calendar.get(Calendar.YEAR);
        int today = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTimeInMillis(dateTimeB);
        int saveDay = calendar.get(Calendar.DAY_OF_YEAR);
        int saveYear=calendar.get(Calendar.YEAR);

        if (year==saveYear&&today == saveDay) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isBeforeYesterday(long dateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return dateTime < calendar.getTimeInMillis();
    }

    //是不是昨天
    public static boolean isYesterday(long dateTime) {
        Calendar today = Calendar.getInstance();
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTimeInMillis(dateTime);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        timeCalendar.set(Calendar.HOUR_OF_DAY, 0);
        timeCalendar.set(Calendar.MINUTE, 0);
        timeCalendar.set(Calendar.SECOND, 0);
        int tmp = 86400000;

        long day1 = timeCalendar.getTimeInMillis() / tmp;
        long day2 = today.getTimeInMillis() / tmp;

        if (day2 - day1 == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据日期获取星期几
     *
     * @param time String类型的日期(eg:2019-2-20)
     * @return 星期几
     */
    public static String dateToWeek(Date time) {
        Calendar cal = Calendar.getInstance(); // 获得一个日历
        cal.setTime(time);//将日历设置为指定日期
        return dateToWeek(cal);
    }

    public static String dateToWeek(Calendar cal) {
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;//获取指定日期对应的数值
        if (w < 0)
            w = 0;
        return weekDays[w];
    }

    public static String getWeekDay(long time) {
        if (isNextWeek(time, 1)) {
            return "下" + dateToWeek(new Date(time));
        } else if (isNextWeek(time, 2)) {
            return "下下" + dateToWeek(new Date(time));
        } else {
            return dateToWeek(new Date(time));
        }
    }

    public static String getWeekDayWxMiniFormat(long time) {
//        if (isNextWeek(time, 1) || isNextWeek(time, 2)) {
        return format(time, "MM月dd号 E HH:mm");
//        } else {
//            return dateToWeek(new Date(time)) + format(time, "HH:mm");
//        }
    }

    /**
     * 判断输入的日期是否在下周区间
     */
    public static boolean isNextWeek(long date, int nextWeekCount) {
        // 设置时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        int dayOfWeek = calendar1.get(Calendar.DAY_OF_WEEK) - 1;
        int offset1 = 1 - dayOfWeek;
        int offset2 = 7 - dayOfWeek;
        calendar1.add(Calendar.DATE, offset1 + 7 * nextWeekCount);
        calendar2.add(Calendar.DATE, offset2 + 7 * nextWeekCount);
        // 下周星期一
        String monday = sdf.format(calendar1.getTime());
        // 下周星期日
        String sunday = sdf.format(calendar2.getTime());

        String format = sdf.format(date);
        try {
            Date parse = sdf.parse(format);
            Date mon = sdf.parse(monday);
            Date sun = sdf.parse(sunday);
            return parse.compareTo(mon) >= 0 && sun.compareTo(parse) >= 0;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取几天前或者几天后的日期
     *
     * @param dateTime 基准日期
     * @param n        (eg: -3 前三天 ;+5 后五天)
     * @return
     * @throws ParseException
     */
    public static String getAfterOrAgoDay(String dateTime, int n) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//定义SimpleDateFormat对象
        Calendar cal = Calendar.getInstance();//定义Calendar对象
        try {
            cal.setTime(sdf.parse(dateTime));//将日期转化为date类型
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cal.add(Calendar.DATE, n); //设置要获取的日期
        String time = sdf.format(cal.getTime());//格式化
        return time;
    }

    public static Date getAfterOrAgoToday(int n) {
        Calendar cal = Calendar.getInstance();//定义Calendar对象
        cal.add(Calendar.DATE, n); //设置要获取的日期
        return cal.getTime();
    }

    /**
     * 最近联系人 界面所显示时间
     *
     * @param date
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    @SuppressWarnings("deprecation")
    public static String parseTimeForDateList(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = null;
        Date currentTime = new Date();
        if (currentTime.getYear() != date.getYear()) {
            formatter = new SimpleDateFormat("yyyy年M月dd日", Locale.getDefault());
        } else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate()) {
            formatter = new SimpleDateFormat("今天 HH:mm", Locale.getDefault());
        } else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate() + 1) {
            formatter = new SimpleDateFormat("昨天 HH:mm", Locale.getDefault());
        } /*else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate() + 2) {
            formatter = new SimpleDateFormat("前天 a hh:mm", Locale.getDefault());
        }*/ else {
            formatter = new SimpleDateFormat("M月dd日 HH:mm", Locale.getDefault());
        }
        return formatter.format(date);
    }

    /**
     * 格式化日期
     * @return 今天、昨天、具体日期
     */
    public static String formatMessageDate(long millis) {
        //当前时间
        Calendar now = Calendar.getInstance();
        //发布时间
        Calendar other = Calendar.getInstance();
        other.setTimeInMillis(millis);
        //今年
        if (now.get(Calendar.YEAR) == other.get(Calendar.YEAR)) {
            int nowDay = now.get(Calendar.DAY_OF_YEAR);
            int otherDay = other.get(Calendar.DAY_OF_YEAR);
            //今天
            if (nowDay == otherDay) {
                return "今天";
            } else if (nowDay == otherDay + 1) {
                //昨天
                return "昨天";
            } else if (nowDay == otherDay + 2) {
                //昨天
                return "前天";
            } else {
                return new SimpleDateFormat("MM月dd日", Locale.getDefault()).format(millis);
            }
        }
        return new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(millis);
    }
    public static String parseTimeForCurrentChat(Long timeMillis) {
        String result="";
        if(timeMillis!=null) {
             result= parseTimeForCurrentChat(new Date(timeMillis));
        }
        Logger.i("AiChatComposerAdapter","time millis="+result);
        return result;
    }
    /**
     * 当前对话所显示的时间
     *
     * @param date
     * @return
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("SimpleDateFormat")
    public static String parseTimeForCurrentChat(Date date) {
        SimpleDateFormat formatter = null;
        Date currentTime = new Date();
        Calendar formatDate = Calendar.getInstance();
        formatDate.setTime(date);
        Calendar current = Calendar.getInstance();


        if (currentTime.getYear() != date.getYear()) {
            formatter = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        } else {
            formatter = new SimpleDateFormat("M月dd日 HH:mm", Locale.getDefault());
//            if (currentTime.getMonth() == date.getMonth()) {
            if (currentTime.getDate() == date.getDate()) {
                formatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
//                    long minutes = (currentTime.getTime() - date.getTime()) / MINUTE;
//                    if (minutes < 60)
//                        if (minutes == 0)
//                            return "1分钟内";
//                        else
//                            return minutes + "分钟前";
                // return

            } else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate() + 1) {
                formatter = new SimpleDateFormat("昨天 HH:mm", Locale.getDefault());
            } else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate() + 2) {
                formatter = new SimpleDateFormat("前天 HH:mm", Locale.getDefault());
            } else if (formatDate.get(Calendar.WEEK_OF_YEAR) == current.get(Calendar.WEEK_OF_YEAR)) {
                formatter = new SimpleDateFormat("E HH:mm", Locale.getDefault());
            }/* else if (currentTime.getDate() == date.getDate() + 1) {
                formatter = new SimpleDateFormat("昨天 HH:mm", Locale.getDefault());
            } else if (currentTime.getDate() <= date.getDate() + 7) {
                formatter = new SimpleDateFormat("E HH:mm", Locale.getDefault());
            }*/
//            }
        }
        return formatter.format(date);
    }

    public static String parseTimeForPosting(long dateTime) {
        return parseTimeForPosting(dateTime,true);
    }

    /**
     * 当前对话所显示的时间
     * 60s内显示刚刚
     * 60分钟内显示分钟数 如45 分钟前
     * 24小时内 显示小时数 如12小时前
     * 24小时以上 显示日期 如 01-02
     * 大于24小时且去年以上的 显示 2020-01-02
     *
     * @param dateTime
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String parseTimeForPosting(long dateTime, boolean withSuffix) {
        SimpleDateFormat formatter = null;
        Calendar other = Calendar.getInstance();
        other.setTimeInMillis(dateTime);
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.YEAR) != other.get(Calendar.YEAR)) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        }/* else if (isYesToday(dateTime)) {
            formatter = new SimpleDateFormat("昨日HH:mm", Locale.getDefault());
        }*/ else if (now.get(Calendar.MONTH) == other.get(Calendar.MONTH) && now.getTimeInMillis() - other.getTimeInMillis() < DateUtils.WEEK_IN_MILLIS) {
            return fromToday(dateTime) + (withSuffix ? "发布" : "");
        } else {
            formatter = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        }
        return formatter.format(other.getTime()) + (withSuffix ? "发布" : "");
    }

    /**
     * 当前对话所显示的时间
     * 60s内显示刚刚
     * 60分钟内显示分钟数 如45 分钟前
     * 24小时内 显示小时数 如12小时前
     * 24小时以上 显示日期 如 01-02
     * 大于24小时且去年以上的 显示 2020-01-02
     *
     * @param dateTime
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String parseTimeForComment(long dateTime) {
        SimpleDateFormat formatter = null;
        Date date = new Date(dateTime);
        Date currentTime = new Date();
        if (currentTime.getYear() != date.getYear()) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        } /*else if (isYesToday(dateTime)) {
            formatter = new SimpleDateFormat("昨日HH:mm", Locale.getDefault());
        }*/ else if (currentTime.getMonth() == date.getMonth() && ((currentTime.getTime() - date.getTime()) / 1000 / ONE_HOUR) < 24) {
            return fromToday(dateTime);
        } else {
            formatter = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        }
        return formatter.format(date);
    }

    /**
     * 当前对话所显示的时间
     * 60s内显示刚刚
     * 60分钟内显示分钟数 如45 分钟前
     * 24小时内 显示小时数 如12小时前
     * 24小时以上 显示日期 如 01-02
     * 大于24小时且去年以上的 显示 2020-01-02
     *
     * @param dateTime
     * @return
     */
    public static String parseTimeForRecommendPosting(long dateTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateTime);

        long time = dateTime / 1000;
        long now = new Date().getTime() / 1000;
        long ago = now - time;
        if (ago <= ONE_MINUTE) {
            return "刚刚活跃过";
        } else if (ago <= ONE_HOUR) {
            return ago / ONE_MINUTE + "分钟前活跃过";
        } else if (isToday(dateTime)) {
            return "今日活跃过";
        } else {
            return "最近活跃过";
        }
    }

    public static String parseTimeForGroup(long dateTime) {
        return parseTimeForGroup(dateTime, " ");
    }

    public static String parseTimeForGroup(long dateTime, String space) {
        SimpleDateFormat formatter;
        Calendar tomorrow = Calendar.getInstance();
        Calendar instance = Calendar.getInstance();
        int currentTime = instance.get(Calendar.YEAR) - 1900;
        int today = instance.get(Calendar.DAY_OF_YEAR);
        tomorrow.add(Calendar.DATE, 1); //设置为明天
        instance.setTimeInMillis(dateTime);
        int dateTimeC = instance.get(Calendar.YEAR) - 1900;
        boolean isToday;
        boolean isTomorrow;
        if (currentTime != dateTimeC) {
            formatter = new SimpleDateFormat(FORMAT_CN_DATE3, Locale.getDefault());
        } else {
            isToday = instance.get(Calendar.DAY_OF_YEAR) == today;
            if (isToday) {
                formatter = new SimpleDateFormat("MM月dd日" + space + "今天" + space + "HH:mm", Locale.getDefault());
            } else {
                isTomorrow = tomorrow.get(Calendar.DAY_OF_YEAR) == instance.get(Calendar.DAY_OF_YEAR);
                if (isTomorrow) {
                    formatter = new SimpleDateFormat("MM月dd日" + space + "明天" + space + "HH:mm", Locale.getDefault());
                } else {
                    formatter = new SimpleDateFormat("MM月dd日" + space + "E" + space + "HH:mm", Locale.getDefault());
                }
            }
        }
        return formatter.format(new Date(instance.getTimeInMillis()));
    }

    public static String parseTimeForHotShopGroup(long dateTime, String space) {
        SimpleDateFormat formatter;
        Calendar tomorrow = Calendar.getInstance();
        Calendar instance = Calendar.getInstance();
        int currentTime = instance.get(Calendar.YEAR) - 1900;
        int today = instance.get(Calendar.DAY_OF_YEAR);
        tomorrow.add(Calendar.DATE, 1); //设置为明天
        instance.setTimeInMillis(dateTime);
        int dateTimeC = instance.get(Calendar.YEAR) - 1900;
        boolean isTomorrow;

        boolean isToday = instance.get(Calendar.DAY_OF_YEAR) == today;
        if (isToday) {
            formatter = new SimpleDateFormat("今天" + space + "HH:mm", Locale.getDefault());
        } else {
            isTomorrow = tomorrow.get(Calendar.DAY_OF_YEAR) == instance.get(Calendar.DAY_OF_YEAR);
            if (isTomorrow) {
                formatter = new SimpleDateFormat("明天" + space + "HH:mm", Locale.getDefault());
            } else {
                formatter = new SimpleDateFormat("MM月dd日" + space + "HH:mm", Locale.getDefault());
            }
        }
        return formatter.format(new Date(instance.getTimeInMillis()));
    }


    public static String secToTimeMis(long time) {
        return secToTime((int) (time / 1000));
    }
    /**
     * 将以秒为单位的时间转换为时分秒即 00:00:00格式
     *
     * @param time 秒数
     * @return
     */
    public static String secToTime(int time) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (time <= 0)
            return "00:00";
        else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }
    public static String secToTime(long time) {
        return secToTime((int) time);
    }
    public static String timeDifference(String beginTime) {

        DateFormat df = new SimpleDateFormat(FORMAT_DATETIME);
        long diff = 0;
        try {
            Date d1 = new Date(System.currentTimeMillis());
            Date d2 = df.parse(beginTime);
            diff = 24 * 1000 * 60 * 60 - (d1.getTime() - d2.getTime());
        } catch (Exception e) {
        }
        long hours = (diff / (1000 * 60 * 60));
        long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
        if (hours <= 0 && minutes <= 0) {
            return null;
        } else {
            return hours + "小时" + minutes + "分";
        }
    }

    private static String unitFormat(int i) {
        String retStr;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }


    public static int getAge(Date birthDay) {
        if (birthDay == null) {
            return 0;
        }
        Calendar cal = Calendar.getInstance();
        if (cal.before(birthDay)) { //出生日期晚于当前时间，无法计算
            return 0;
        }
        int yearNow = cal.get(Calendar.YEAR);  //当前年份
        int monthNow = cal.get(Calendar.MONTH);  //当前月份
        int dayOfMonthNow = cal.get(Calendar.DAY_OF_MONTH); //当前日期
        cal.setTime(birthDay);
        int yearBirth = cal.get(Calendar.YEAR);
        int monthBirth = cal.get(Calendar.MONTH);
        int dayOfMonthBirth = cal.get(Calendar.DAY_OF_MONTH);
        int age = yearNow - yearBirth;   //计算整岁数
        if (monthNow <= monthBirth) {
            if (monthNow == monthBirth) {
                if (dayOfMonthNow <= dayOfMonthBirth) age--;//当前日期在生日之前，年龄减一
            } else {
                age--;//当前月份在生日之前，年龄减一
            }
        }
        return age;
    }

    public static String getConstellation(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return getConstellation(c);
    }

    public static String getConstellation(Calendar date) {
        if (date == null) {
            return null;
        }
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);
        Logger.i("const_", "month=" + month + ",day:" + day);
        String constellation = "";
        if (month == 1 && day >= 20 || month == 2 && day <= 18) {
            constellation = "水瓶座";
        }
        if (month == 2 && day >= 19 || month == 3 && day <= 20) {
            constellation = "双鱼座";
        }
        if (month == 3 && day >= 21 || month == 4 && day <= 19) {
            constellation = "白羊座";
        }
        if (month == 4 && day >= 20 || month == 5 && day <= 20) {
            constellation = "金牛座";
        }
        if (month == 5 && day >= 21 || month == 6 && day <= 21) {
            constellation = "双子座";
        }
        if (month == 6 && day >= 22 || month == 7 && day <= 22) {
            constellation = "巨蟹座";
        }
        if (month == 7 && day >= 23 || month == 8 && day <= 22) {
            constellation = "狮子座";
        }
        if (month == 8 && day >= 23 || month == 9 && day <= 22) {
            constellation = "处女座";
        }
        if (month == 9 && day >= 23 || month == 10 && day <= 23) {
            constellation = "天秤座";
        }
        if (month == 10 && day >= 24 || month == 11 && day <= 22) {
            constellation = "天蝎座";
        }
        if (month == 11 && day >= 23 || month == 12 && day <= 21) {
            constellation = "射手座";
        }
        if (month == 12 && day >= 22 || month == 1 && day <= 19) {
            constellation = "摩羯座";
        }
        return constellation;
    }

    public static boolean isSameYear(long dateTimeA, long dateTimeB) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateTimeA);
        int yearA = calendar.get(Calendar.YEAR);
        calendar.setTimeInMillis(dateTimeB);
        int yearB = calendar.get(Calendar.YEAR);
        return yearA == yearB;
    }

    public static String formatMomentDate(@Nullable Long millis) {
        if (millis == null) {
            return "";
        }
        if (isToday(millis)) {
            return "今天";
        }
        if (isSameYear(millis, System.currentTimeMillis())) {
            return format(millis, " MM月|dd日");
        }
        return format(millis, " MM月 yyyy年|dd日");
    }

    /**
     * "yyyy-MM-dd HH:mm:ss"
     *
     * @return
     */
    public static String getDateTime() {
        return getFormatDateTime("yyyy-MM-dd");
    }

    /**
     * "yyyy-MM-dd HH:mm"
     *
     * @return
     */
    public static String getDateTime(long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(timeStamp);
        return sdf.format(date);
    }

    /**
     * "yyyy-MM-dd HH:mm"
     *
     * @return
     */
    public static String getDateTime2(long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date(timeStamp);
        return sdf.format(date);
    }

    /**
     * 获取指定格式的当前时间
     *
     * @param format 时间格式
     * @return
     */
    public static String getFormatDateTime(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = new Date();
        return sdf.format(date);
    }

    /**
     * 获取指定格式的指定时间
     */
    public static String getFormatDateTime(long timeStamp, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = new Date(timeStamp);
        return sdf.format(date);
    }

    public static String getChineseDateStr(long timeStamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeStamp);
        return String.format("%d月%d日 %02d:%02d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static String getChineseDateStr2(long timeStamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeStamp);
        return String.format("%d年%d月%d日 %02d:%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    /**
     * 距离今天多久
     *
     * @param timeStamp
     * @return
     */
    public static String fromToday(long timeStamp) {
        long ago = System.currentTimeMillis() - timeStamp;
        if (ago <= DateUtils.MINUTE_IN_MILLIS) {
            return "刚刚";
        } else if (ago <= DateUtils.HOUR_IN_MILLIS)
            return "刚刚";
        else if (ago <= DateUtils.DAY_IN_MILLIS)
            return ago / DateUtils.HOUR_IN_MILLIS + "小时前";
        else if (ago <= (DateUtils.DAY_IN_MILLIS * 30)) {
            long day = ago / DateUtils.DAY_IN_MILLIS;
            return day + "天前";
        } else if (ago <= (DateUtils.DAY_IN_MILLIS * 365)) {
            long month = ago / (DateUtils.DAY_IN_MILLIS * 30);
            return month + "个月前";
        } else {
            long year = ago / (DateUtils.DAY_IN_MILLIS * 365);
            return year + "年前";
        }
    }

    /**
     * 距离今天多久
     *
     * @param timeStamp
     * @return
     */
    public static String fromOneMonth(long timeStamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        long time = timeStamp / 1000;
        long now = new Date().getTime() / 1000;
        long ago = now - time;
        if (ago > ONE_MONTH) {
            return "1个月前";
        } else {
            return getDateTime(timeStamp);
        }
    }

    /**
     * 根据秒返回时分秒
     *
     * @param time
     * @return
     */
    public static String getFormatHMS(long time) {
        //time = time / 1000;//总秒数
        int s = (int) (time % 60);//秒
        int m = (int) ((time % 3600) / 60);//分
        int h = (int) (time / 3600);//时
        if (h>0){
            return String.format("%02d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d", m, s);
    }

    /**
     * 根据毫秒返回时分秒
     *
     * @param time
     * @return
     */
    public static String getFormatHMS2(long time) {
        //time = time / 1000;//总秒数
        int m = (int) ((time % 3600) / 60);//分
        int h = (int) (time / 3600);//时
        return h + "小时" + m + "分";
    }

    /**
     * 根据毫秒返回时分秒
     *
     * @param time
     * @return
     */
    public static String getFormatHMS3(long time) {
        int s = (int) (time % 60);//秒
        int m = (int) ((time % 3600) / 60);//分
        int h = (int) (time / 3600);//时
        return h + "小时" + m + "分" + s + "秒";
    }

    /**
     * 根据毫秒返回时分
     *
     * @param time
     * @return
     */
    public static String getFormatHM(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        return String.format("%02d:%02d", h, m);
    }


    /**
     * 根据毫秒返回分秒
     *
     * @param time
     * @return
     */
    public static String getFormatMS(long time) {
        int s = (int) (time % 60);//秒
        int m = (int) (time / 60);//总分数
        //int m = (int) ((time % 3600) / 60);//分
        //int h = (int) (time / 3600);//时
        return String.format("%02d:%02d", m, s);
    }



    public static boolean isTomorrow(long time1) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(time1);

        Calendar cal2 = Calendar.getInstance();
        int today = cal2.get(Calendar.DAY_OF_YEAR);
        cal2.set(Calendar.DAY_OF_YEAR, today + 1);

        boolean isTomorrow = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        return isTomorrow;
    }

    public static String formatTimeForChat(Date date) {
        SimpleDateFormat formatter = null;
        Date currentTime = new Date();
        if (currentTime.getYear() != date.getYear()) {
            formatter = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        } else {
            formatter = new SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault());
            if (currentTime.getMonth() == date.getMonth()) {
                if (currentTime.getDate() == date.getDate()) {
                    formatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    long minutes = (currentTime.getTime() - date.getTime()) / 1000 / ONE_MINUTE;
                    if (minutes < 60)
                        if (minutes == 0)
                            return "刚刚";
                        else
                            return minutes + "分钟前";
                    // return

                } else if (currentTime.getDate() == date.getDate() + 1) {
                    formatter = new SimpleDateFormat("昨天 HH:mm", Locale.getDefault());
                }
            }
        }
        return formatter.format(date);
    }

    /**
     * 最近联系人 界面所显示时间
     *
     * @param date
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    @SuppressWarnings("deprecation")
    public static String formatTimeForContactList(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = null;
        Date currentTime = new Date();
        Calendar formatDate = Calendar.getInstance();
        formatDate.setTime(date);
        Calendar current = Calendar.getInstance();
        if (currentTime.getYear() != date.getYear()) {
            formatter = new SimpleDateFormat("yyyy/MM", Locale.getDefault());
        } else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate()) {
            formatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate() + 1) {
            formatter = new SimpleDateFormat("昨天 HH:mm", Locale.getDefault());
        } else if (currentTime.getMonth() == date.getMonth() && currentTime.getDate() == date.getDate() + 2) {
            formatter = new SimpleDateFormat("前天 HH:mm", Locale.getDefault());
        }else if (formatDate.get(Calendar.WEEK_OF_YEAR) == current.get(Calendar.WEEK_OF_YEAR)) {
            formatter = new SimpleDateFormat("E HH:mm", Locale.getDefault());
        } else {
            formatter = new SimpleDateFormat("MM-dd", Locale.getDefault());
        }
        return formatter.format(date);
    }

    /**
     * 毫秒转时间字符串
     *
     * @param msecTotal
     * @return 00:00
     */
    public static String parseMMSSString(int msecTotal) {
        msecTotal /= 1000;
        int minute = msecTotal / 60;
        int second = msecTotal % 60;
        minute %= 60;
        return String.format("%02d:%02d", minute, second);
    }
}
