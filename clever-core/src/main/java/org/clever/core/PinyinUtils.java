package org.clever.core;

import lombok.SneakyThrows;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.apache.commons.lang3.StringUtils;

/**
 * 汉字转换成拼音的工具类，使用pinyin4j实现<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-4-30 0:40 <br/>
 */
public class PinyinUtils {
    /**
     * 拼音格式：1.全小写；2.用音调字符表示音调；3.'ü'的表示方式"ü"，默认格式
     */
    private final static HanyuPinyinOutputFormat DEFAULT_FORMAT;

    static {
        //输出设置，大小写，音标方式等
        DEFAULT_FORMAT = new HanyuPinyinOutputFormat();
        // 小写
        DEFAULT_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        // 用音调字符表示音调
        DEFAULT_FORMAT.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        // 'ü' is "ü"
        DEFAULT_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    }

    private PinyinUtils() {

    }

    /**
     * 获取一个汉字的所有拼音
     *
     * @param c      汉字字符
     * @param format 设置拼音的格式
     * @return 如果参数c不是汉字或转换，成功返回所有拼音
     */
    @SneakyThrows
    public static String[] toAllPinYin(char c, HanyuPinyinOutputFormat format) {
        String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c, format);
        if (pinyin == null || pinyin.length <= 0) {
            return new String[]{};
        }
        return pinyin;
    }

    /**
     * 获取一个汉字的所有拼音<br/>
     * 使用默认格式：1.全小写；2.用音调字符表示音调；3.'ü'的表示方式"ü"，默认格式<br/>
     *
     * @param c 汉字字符
     * @return 如果参数c不是汉字或转换失败返回null，成功返回所有拼音
     */
    public static String[] toAllPinYin(char c) {
        return toAllPinYin(c, DEFAULT_FORMAT);
    }

    /**
     * 获取一个汉字的一个发音
     *
     * @param c      汉字字符
     * @param format 设置拼音的格式
     * @return 如果参数c不是汉字或转换失败返回null，成功返回第一个发音的拼音
     */
    public static String toPinYin(char c, HanyuPinyinOutputFormat format) {
        String[] pinyin = toAllPinYin(c, format);
        if (pinyin.length <= 0) {
            return null;
        }
        return pinyin[0];
    }

    /**
     * 获取一个汉字的一个发音
     *
     * @param c 汉字字符
     * @return 如果参数c不是汉字或转换失败返回null，成功返回第一个发音的拼音
     */
    public static String toPinYin(char c) {
        return toPinYin(c, DEFAULT_FORMAT);
    }

    /**
     * 把一个汉字字符串转成拼音字符串，不能转换的部分原样输出
     *
     * @param str       汉字字符串
     * @param separator 拼音之间的分隔符
     * @param format    设置拼音的格式
     * @return 转换后的字符串，转换失败返回null
     */
    public static String getStringPinYin(String str, String separator, HanyuPinyinOutputFormat format) {
        if (null == str) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String tempPinyin;
        boolean flag = false;
        for (int i = 0; i < str.length(); ++i) {
            tempPinyin = toPinYin(str.charAt(i), format);
            if (tempPinyin == null) {
                sb.append(str.charAt(i));
                flag = true;
            } else {
                if (flag && i < (str.length() - 1) && null != separator) {
                    flag = false;
                    sb.append(separator);
                }
                sb.append(tempPinyin);
                if (i < (str.length() - 1) && null != separator) {
                    sb.append(separator);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 把一个汉字字符串转成拼音字符串，不能转换的部分原样输出<br/>
     * 使用默认格式：1.全小写；2.用音调字符表示音调；3.'ü'的表示方式"ü"，默认格式<br/>
     *
     * @param str       汉字字符串
     * @param separator 拼音之间的分隔符
     * @return 转换后的字符串，转换失败返回null
     */
    public static String getStringPinYin(String str, String separator) {
        return getStringPinYin(str, separator, DEFAULT_FORMAT);
    }

    /**
     * 把一个汉字字符串转成拼音字符串，不能转换的部分原样输出<br/>
     * 使用默认格式：1.全小写；2.用音调字符表示音调；3.'ü'的表示方式"ü"，默认格式<br/>
     *
     * @param str 汉字字符串
     * @return 转换后的字符串，转换失败返回null
     */
    public static String getStringPinYin(String str) {
        return getStringPinYin(str, StringUtils.EMPTY, DEFAULT_FORMAT);
    }

    /**
     * 把一个汉字字符串转成拼音字符串，忽略不能转换的部分<br/>
     * <b>注意：不输出不能转换的部分</b>
     *
     * @param str       汉字字符串
     * @param separator 拼音之间的分隔符
     * @param format    设置拼音的格式
     * @return 转换后的字符串，转换失败返回null
     */
    public static String getStringPurePinYin(String str, String separator, HanyuPinyinOutputFormat format) {
        if (null == str) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String tempPinyin;
        for (int i = 0; i < str.length(); ++i) {
            tempPinyin = toPinYin(str.charAt(i), format);
            if (tempPinyin != null) {
                sb.append(tempPinyin);
                if (i < (str.length() - 1) && null != separator) {
                    sb.append(separator);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 把一个汉字字符串转成拼音字符串，忽略不能转换的部分<br/>
     * 使用默认格式：1.全小写；2.用音调字符表示音调；3.'ü'的表示方式"ü"，默认格式<br/>
     * <b>注意：不输出不能转换的部分</b>
     *
     * @param str       汉字字符串
     * @param separator 拼音之间的分隔符
     * @return 转换后的字符串
     */
    public static String getStringPurePinYin(String str, String separator) {
        return getStringPurePinYin(str, separator, DEFAULT_FORMAT);
    }

    /**
     * 把一个汉字字符串转成拼音字符串，忽略不能转换的部分<br/>
     * 使用默认格式：1.全小写；2.用音调字符表示音调；3.'ü'的表示方式"ü"，默认格式<br/>
     * <b>注意：不输出不能转换的部分</b>
     *
     * @param str 汉字字符串
     * @return 转换后的字符串
     */
    public static String getStringPurePinYin(String str) {
        return getStringPurePinYin(str, StringUtils.EMPTY, DEFAULT_FORMAT);
    }

    /**
     * 获取汉字的拼音首字母<br/>
     *
     * @param c 汉字字符
     * @return 成功返回首字母
     */
    public static char toHeadPinYin(char c) {
        char headChar = '0';
        String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c);
        if (pinyin != null && pinyin.length > 0) {
            headChar = pinyin[0].charAt(0);
        }
        return headChar;
    }

    /**
     * 获取一个中文字符串的所有首拼，不能转换的部分原样输出<br/>
     *
     * @param str       汉字字符串
     * @param separator 拼音之间的分隔符
     * @return 转换后的字符串，转换失败返回null
     */
    public static String getStringHeadPinYin(String str, String separator) {
        if (null == str) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        char headChar;
        boolean flag = false;
        for (int i = 0; i < str.length(); ++i) {
            headChar = toHeadPinYin(str.charAt(i));
            if (headChar != '0') {
                if (flag && i < (str.length() - 1) && null != separator) {
                    sb.append(separator);
                    flag = false;
                }
                sb.append(headChar);
                if (i < (str.length() - 1) && null != separator) {
                    sb.append(separator);
                }
            } else {
                sb.append(str.charAt(i));
                flag = true;
            }
        }
        return sb.toString();
    }

    /**
     * 获取一个中文字符串的所有首拼，忽略不能转换的部分<br/>
     * <b>注意：不输出不能转换的部分</b>
     *
     * @param str       汉字字符串
     * @param separator 拼音之间的分隔符
     * @return 转换后的字符串，转换失败返回null
     */
    public static String getStringHeadPurePinYin(String str, String separator) {
        if (null == str) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        char headChar;
        for (int i = 0; i < str.length(); ++i) {
            headChar = toHeadPinYin(str.charAt(i));
            if (headChar != '0') {
                sb.append(headChar);
                if (i < (str.length() - 1) && null != separator) {
                    sb.append(separator);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 判断一个字符串是否全部由中文汉字组成<br/>
     *
     * @param str 判断的字符串
     * @return str完全由汉字组成返回true
     */
    public static boolean isChineseString(String str) {
        return StringUtils.isNotBlank(str) && str.matches("[\\u4E00-\\u9FA5]+");
    }

    /**
     * 将字符串中的中文转化为拼音,其他字符不变
     */
    public static String getPingYin(String inputString) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        char[] input = inputString.trim().toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char ch : input) {
            String str = Character.toString(ch);
            if (isChineseString(str)) {
                try {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (arr != null && arr.length > 0) {
                        str = arr[0];
                    }
                } catch (Exception ignored) {
                }
            }
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 获取汉字串拼音首字母，英文字符不变
     *
     * @param chinese 汉字串
     * @return 汉语拼音首字母
     */
    public static String getFirstSpell(String chinese) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        StringBuilder sb = new StringBuilder();
        char[] chineseArr = chinese.toCharArray();
        for (char ch : chineseArr) {
            char firstSpell = ch;
            if (ch > 128) {
                try {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (arr != null && arr.length > 0) {
                        firstSpell = arr[0].charAt(0);
                    }
                } catch (Exception ignored) {
                }
            }
            sb.append(firstSpell);
        }
        return sb.toString().toUpperCase();
    }


    /**
     * 获取汉字串拼音，英文字符不变
     *
     * @param chinese 汉字串
     * @return 汉语拼音
     */
    public static String getFullSpell(String chinese) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        StringBuilder sb = new StringBuilder();
        char[] chineseArr = chinese.toCharArray();
        for (char ch : chineseArr) {
            String str = Character.toString(ch);
            if (ch > 128) {
                try {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (arr != null && arr.length > 0) {
                        str = arr[0];
                    }
                } catch (Exception ignored) {
                }
            }
            sb.append(str);
        }
        return sb.toString();
    }
}
