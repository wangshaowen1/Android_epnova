package com.kuaigeglobal.epnova.common.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nhsd.basic.utils.Logger;
import com.kuaigeglobal.epnova.core.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CheckSumBuilder {
    /*一、app需要的值
    1.authAppSecret 已经单独发出来了
    2.authSaltVal -- 配置接口返回的jmsz
    3.appKey -- 配置接口返回的appKey

    二、http header中包含：
    1、appKey
    2、nonce -- 随机字符串，java的写UUID即可
    3、curTime -- 当前UTC时间戳，从1970年1月1日0点0 分0 秒开始到现在的秒数(String)
    4、checkSum --  生成的方式是：CheckSumBuilder.getCheckSum(authAppSecret + authSaltValue, nonce ,curTime);*/

    /*authAppSecret*/
    public final static String SECRET = "bd32e4b94784f331f8e93de5634d9924";
    public final static String TEST_SECRET = "26ec4428da8e87ace7a9a52361ee841b";


    /**
     * MD5加密key -- 用于接口鉴权加密
     */
    public final static String MD5_KEY_REQ_VERIFY = "ghdt368rj^&*(irhjdgsdfgvbxnfhdgRYUHDFGHSFGS()@$^tdhgje%wsjgfghjsdgyhnmnzfgrtty!#%$%^$%^*ughsdfG$%@#$WRTWERTFHJFYURTYU";

    /**
     * MD5加密key -- 用于获取替换入参是数组形式的value
     */
    public final static String MD5_KEY_REQ_ARRAY_PARAM_REPLACE_VALUE = "6789tjhfghjfgjGYJKRGH@#%#^$%^*%^&(%^&#$%^~~$#^$%dsfgdfhdfghfgfyertyGKYU89667856674567";

    /**
     * MD5加密key -- 用于短信加密--测试环境使用
     */
    //public final static String MD5_KEY_SEND_SMS = "DFGdfgsd#$%^tsdfgsdfTHWERTU@$%#$^*%^&usdghshd@#$fswtrygwqert87944GFSDRFGSfdrggtuDG!%#$%";

    /**
     * MD5加密key -- 用于短信加密--这个是生产的，上面的测试是临时使用
     */
    public final static String MD5_KEY_SEND_SMS = "D%o43T&hJ63aa!BMS4@aMgE2e2gUPj56&L$t9$^M&&2s#Y897b$X7dL&@%Z2&^JR";

    /**
     * MD5加密key -- 用于获取支付宝人脸识别参数鉴权
     */
    public final static String MD5_KEY_ALIPAY_FACE_CERTIFY = "";

    /**
     * MD5加密key -- 用于获取旷世人脸识别参数鉴权
     */
    public final static String MD5_KEY_MEGVII_FACE_CERTIFY = "";

    // 计算并获取CheckSum
    public static String getCheckSum(Map map, String nonce, String curTime) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        String sortReqParam = getSortReqParam(map);
        return encode("sha1", getMD5(sortReqParam + MD5_KEY_REQ_VERIFY) + nonce + curTime);
    }

    // 获取验证码用
    public static String getCheckSum(String phone) {
        return getMD5(phone + MD5_KEY_SEND_SMS);
    }

    // 获取支付宝身份验证链接用
    public static String getIdCheckSum(String realId) {
        return getMD5(User.get().getId() + realId + MD5_KEY_ALIPAY_FACE_CERTIFY);
    }

    // 获取旷视链接用
    public static String getMegviiCheckSum(String realId) {
        return getMD5(User.get().getId() + realId + MD5_KEY_MEGVII_FACE_CERTIFY);
    }

    // 计算并获取md5值
    public static String getMD5(String requestBody) {
        return encode("md5", requestBody);
    }

    private static String encode(String algorithm, String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(value.getBytes());
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (int j = 0; j < len; j++) {
            buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static void main(String[] args) {
        //System.out.println(SecureUtil.md5().digestHex("98oiweurkj" + "rmb@#@!)(@&(^&*2019"));
        //System.out.println(SecureUtil.md5().digestHex16("98oiweurkj" + "rmbapp2019"));

       /* String curTime = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString();
        System.out.println(curTime);
        System.out.println(nonce);
        getCheckSum(nonce, curTime);*/
        String par = "{\n" +
                "\t\"annualIncome\": 4,\n" +
                "\t\"birthday\": \"1993-08-17\",\n" +
                "\t\"constellation\": \"狮子座\",\n" +
                "\t\"gender\": 0,\n" +
                "\t\"height\": 160,\n" +
                "\t\"marryPlan\": 3,\n" +
                "\t\"nickName\": \"iu\",\n" +
                "\t\"uniqueId\": \"mR7XOKmR\",\n" +
                "\t\"workAreaCity\": \"深圳\",\n" +
                "\t\"workAreaProvince\": \"广东\"\n" +
                "}";

        Map<String, String> map = new Gson().fromJson(par, new TypeToken<Map<String, String>>() {
        }.getType());

        String sortReqParam = getSortReqParam(map);

        //String md5 = getMD5("123456");
        Logger.d("md5结果：", "" + sortReqParam);
    }

    /**
     * 对参数信息进行签名
     *
     * @param map
     * @return
     */
    private static String getSortReqParam(Map map) {
        List<String> keys = new ArrayList<String>(map.keySet());
        //  key排序
        Collections.sort(keys);

        StringBuilder authInfo = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value;
            if (map.get(key) instanceof List) {
                value = MD5_KEY_REQ_ARRAY_PARAM_REPLACE_VALUE;
            } else {
                value = map.get(key);
            }
            authInfo.append(buildKeyValue(key, String.valueOf(value), false));
            if (i != keys.size() - 1) {
                authInfo.append("&");
            }
        }
        return authInfo.toString();
    }

    /**
     * 拼接键值对
     *
     * @param key
     * @param value
     * @param isEncode
     * @return
     */
    private static String buildKeyValue(String key, String value, boolean isEncode) {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        sb.append("=");
        if (isEncode) {
            try {
                sb.append(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                sb.append(value);
            }
        } else {
            sb.append(value);
        }
        return sb.toString();
    }
}
