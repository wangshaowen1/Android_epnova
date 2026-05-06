# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt
#---------------------------------基础混淆----------------------------------
# 设置混淆的压缩比率 0 ~ 7
-optimizationpasses 5
# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames
# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共库的成员
-dontskipnonpubliclibraryclassmembers
# 混淆时不做预校验
-dontpreverify
# 混淆时不记录日志
-verbose
# 忽略警告 gradle3.5以前为-ignorewarning
-ignorewarnings
# 代码优化
-dontshrink
# 不优化输入的类文件
-dontoptimize
# 保留注解不混淆
-keepattributes *Annotation*,InnerClasses
# 避免混淆泛型
-keepattributes Signature
# 保留代码行号，方便异常信息的追踪
-keepattributes SourceFile,LineNumberTable
# 混淆采用的算法
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# seeds.txt文件列出未混淆的类和成员
-printseeds seeds.txt
# usage.txt文件列出从apk中删除的代码
-printusage unused.txt
# mapping.txt文件列出混淆前后的映射
-printmapping mapping.txt
#---------------------------------基础混淆----------------------------------

#---------------------------------AndroidX---------------------------------
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
#---------------------------------AndroidX---------------------------------

#-----------------------------默认保留基础配置-------------------------------
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.preference.Preference
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.annotation.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
#keep 反射相关
-keepnames class com.kuaigeglobal.epnova.core.media.**
# 保留我们自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class com.android.vending.licensing.ILicensingService
-keep class com.kuaigeglobal.epnova.core.media.** { *; }
#v4
-dontwarn android.support.v4.**
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
-keep class android.support.v4.** { *; }
#v7
-dontwarn android.support.v7.**
-keep class android.support.v7.internal.** { *; }
-keep interface android.support.v7.internal.** { *; }
-keep class android.support.v7.** { *; }
#design
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

#日志(谨慎使用)
#-assumenosideeffects class android.util.Log {
#    public static *** d(...);
#    public static *** v(...);
#    public static *** i(...);
#    public static *** e(...);
#    public static *** w(...);
#}
#R文件
-keep class **.R$* {*;}
#onClick事件
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}
#event事件 针对自定义onEvent事件
-keepclassmembers class * {
    void *(*Event);
}
#枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
#native方法
-keepclasseswithmembernames class * {
    native <methods>;
}
#Android序列化
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
#Java序列化
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保持测试相关的代码
-dontnote junit.framework.**
-dontnote junit.runner.**
-dontwarn android.test.**
-dontwarn android.support.test.**
-dontwarn org.junit.**

#-----------------------------默认保留基础配置-------------------------------

#---------------------------------webview----------------------------------
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}
#---------------------------------webview----------------------------------

#--------------------------------实体相关------------------------------
-keep class com.kuaigeglobal.epnova.common.entity.**{*;}
-keep class com.kuaigeglobal.epnova.common.jsbridge.data.**{*;}
-keep class com.kuaigeglobal.epnova.core.network.apis.**{*;}
-keep class com.kuaigeglobal.epnova.core.db.**{*;}
-keep class com.kuaigeglobal.epnova.core.model.**{*;}
#--------用户信息相关------
-keep class com.kuaigeglobal.epnova.core.User{*;}
-keep class com.kuaigeglobal.epnova.core.User$*{*;}

-keep class com.kuaigeglobal.epnova.core.Config{*;}
-keep class com.kuaigeglobal.epnova.core.Config$*{*;}
-keep class com.kuaigeglobal.epnova.common.Constants{*;}
-keep class com.kuaigeglobal.epnova.common.Constants$*{*;}
-keep class com.kuaigeglobal.epnova.common.Events*{*;}
-keep class com.kuaigeglobal.epnova.common.Events$*{*;}

#封装ViewBinding 用了反射需要这个
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
  public static * inflate(android.view.LayoutInflater);
}
#--------推送------

#----------------------------------第三方包---------------------------------

#SVGA
-keep class com.squareup.wire.** { *; }
-keep class com.opensource.svgaplayer.proto.** { *; }

#eventbus3
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# And if you use AsyncExecutor:
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

#华为推送/定位sdk
#-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.huawei.hianalytics.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}
-keep class * extends com.huawei.hms.core.aidl.IMessageEntity{ *; }
-keep public class com.huawei.location.nlp.network.** {*; }
-keep class com.huawei.wisesecurity.ucs.**{*;}

#穿山甲
-keep class com.bytedance.sdk.openadsdk.**{*;}
-keep public interface com.bytedance.sdk.openadsdk.downloadnew.**{*;}
-keep class com.pgl.sys.ces.**{*;}
-keep class com.bytedance.**
-keep class com.bytedance.**{*;}
-keep class com.pgl.**
-keep class com.pgl.**{*;}
-keep class com.ss.android.**
-keep class com.ss.android.**{*;}
#shareInstall
-keep class com.lockscreen.**
-keep class com.lockscreen.**{*;}
-keep class com.sh.sdk.shareinstall.**
-keep class com.sh.sdk.shareinstall.**{*;}

#openInstall
-dontwarn com.fm.openinstall.**
-dontwarn io.openinstall.**
-keep public class com.fm.openinstall.** {*; }
-keep public interface com.fm.openinstall.** {*; }

#支付相关
-keep public class com.alipay.**
-keep public class com.alipay.**{*;}
-keep class com.tencent.**
-keep class com.tencent.**{ *;}
-dontwarn com.tencent.**
#umeng
-keep class com.umeng.** {*;}
-keep class org.repackage.** {*;}
-keep class com.uyumao.** { *; }
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.tencent.mm.opensdk.**
-keep class com.tencent.mm.opensdk.**{*;}
-keep class com.tencent.mm.sdk.**
-keep class com.tencent.mm.sdk.**{*;}

#matisse
-dontwarn com.bumptech.glide.**
-dontwarn com.squareup.picasso.**

#闪验
-dontwarn com.cmic.gen.sdk.**
-keep class com.cmic.gen.sdk.**{*;}
-dontwarn com.sdk.**
-keep class com.sdk.** { *;}
-dontwarn com.unikuwei.mianmi.account.shield.**
-keep class com.unikuwei.mianmi.account.shield.** {*;}
-keep class cn.com.chinatelecom.account.api.**{*;}
-dontwarn cn.com.chinatelecom.account.**
-keep class cn.com.chinatelecom.account.**{*;}
-keep class com.unicom.online.account.shield.** {*;}
-keep class com.unicom.online.account.kernel.** {*;}
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *;}
-keep class com.chuanglan.shanyan_sdk.**{*;}

#穿山甲
-keep class com.bytedance.sdk.openadsdk.**{*;}
-keep public interface com.bytedance.sdk.openadsdk.downloadnew.**{*;}
-keep class com.pgl.sys.ces.**{*;}
-keep class com.bytedance.**
-keep class com.bytedance.**{*;}
-keep class com.pgl.**
-keep class com.pgl.**{*;}
-keep class com.ss.android.**
-keep class com.ss.android.**{*;}

#butterknife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

-keep class com.youth.banner.** {
    *;
 }

#友盟
#umeng
#厂商推送相关
#huawei
#-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.huawei.hianalytics.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}
#xiaomi
-keep class org.android.agoo.xiaomi.MiPushBroadcastReceiver {*;}
-dontwarn com.xiaomi.push.**
#oppo
-keep public class * extends android.app.Service

#vivo
-dontwarn com.vivo.push.**
-keep class com.vivo.push.** {*;}
-keep class com.vivo.vms.** {*;}
#荣耀
-dontwarn com.hihonor.push.**
-keep class com.hihonor.android.push.** {*;}
-keep class com.hihonor.push.** {*;}
-keep class org.android.agoo.honor.* {*;}

#自定义push
-keep class com.kuaigeglobal.epnova.feature.push.*{*;}


#新加的
-keep class com.efs.** { *; }

-keep class com.umeng.** {*;}
-keep class com.uc.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class com.zui.** {*;}
-keep class com.miui.** {*;}
-keep class com.heytap.** {*;}
-keep class a.** {*;}
-keep class com.vivo.** {*;}
-keep public class com.kuaigeglobal.epnova.R$*{
    public static final int *;
}
-keep public class com.sina.weibo.sdk.**{*;}


#bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

#以下是umeng旧版

-dontwarn com.umeng.**
-dontwarn com.taobao.**
-dontwarn anet.channel.**
-dontwarn anetwork.channel.**
-dontwarn org.android.**
-dontwarn org.apache.thrift.**
-dontwarn com.xiaomi.**
-dontwarn com.huawei.**
-dontwarn com.meizu.**

-keep class com.taobao.** {*;}
-keep class org.android.** {*;}
-keep class anet.channel.** {*;}
-keep class com.umeng.** {*;}
-keep class com.xiaomi.** {*;}
-keep class com.huawei.** {*;}
-keep class com.meizu.** {*;}
-keep class org.apache.thrift.** {*;}
-keep class com.alibaba.sdk.android.**{*;}
-keep class com.ut.**{*;}
-keep class com.ta.**{*;}
-keep public class **.R$*{
   public static final int *;
}
-dontshrink
-dontoptimize
-dontwarn com.google.android.maps.**
-dontwarn android.webkit.WebView
-dontwarn com.umeng.**
-dontwarn com.tencent.weibo.sdk.**
-dontwarn com.facebook.**
-keep public class javax.**
-keep public class android.webkit.**
-dontwarn android.support.v4.**
-keep enum com.facebook.**
-keepattributes Exceptions,InnerClasses,Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public interface com.facebook.**
-keep public interface com.tencent.**
-keep public interface com.umeng.socialize.**
-keep public interface com.umeng.socialize.sensor.**
-keep public interface com.umeng.scrshot.**
-keep public class com.umeng.socialize.* {*;}
-keep class com.facebook.** {*;}
-keep class com.umeng.scrshot.**
-keep public class com.tencent.** {*;}
-keep class com.umeng.socialize.sensor.**
-keep class com.umeng.socialize.handler.**
-keep class com.umeng.socialize.handler.*
-keep class com.umeng.weixin.handler.**
-keep class com.umeng.weixin.handler.*
-keep class com.umeng.qq.handler.**
-keep class com.umeng.qq.handler.*
-keep class UMMoreHandler{*;}
-keep class com.tencent.mm.sdk.modelmsg.WXMediaMessage {*;}
-keep class com.tencent.mm.sdk.modelmsg.** implements com.tencent.mm.sdk.modelmsg.WXMediaMessage$IMediaObject {*;}
-keep class im.yixin.sdk.api.YXMessage {*;}
-keep class im.yixin.sdk.api.** implements im.yixin.sdk.api.YXMessage$YXMessageData{*;}
-keep class com.tencent.wxop.**{*;}
-keep class com.twitter.** { *; }
-keep class com.kakao.** {*;}
-dontwarn com.kakao.**
-keep public class com.umeng.com.umeng.soexample.R$*{
    public static final int *;
}
-keep public class com.linkedin.android.mobilesdk.R$*{
    public static final int *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.tencent.open.TDialog$*
-keep class com.tencent.open.TDialog$* {*;}
-keep class com.tencent.open.PKDialog
-keep class com.tencent.open.PKDialog {*;}
-keep class com.tencent.open.PKDialog$*
-keep class com.tencent.open.PKDialog$* {*;}
-keep class com.umeng.socialize.impl.ImageImpl {*;}
-keep class com.sina.** {*;}
-dontwarn com.sina.**
-keep class  com.alipay.share.sdk.** {
   *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keep class com.linkedin.** { *; }
-keep class com.android.dingtalk.share.ddsharemodule.** { *; }
-keepattributes Signature

-dontwarn com.umeng.**
-dontwarn com.taobao.**
-dontwarn anet.channel.**
-dontwarn anetwork.channel.**
-dontwarn org.android.**
-dontwarn org.apache.thrift.**
-dontwarn com.xiaomi.**
-dontwarn com.huawei.**
-dontwarn com.meizu.**

-keepattributes *Annotation*

-keep class com.taobao.** {*;}
-keep class org.android.** {*;}
-keep class anet.channel.** {*;}
-keep class com.umeng.** {*;}
-keep class com.xiaomi.** {*;}
-keep class com.huawei.** {*;}
-keep class com.meizu.** {*;}
-keep class org.apache.thrift.** {*;}

-keep class com.alibaba.sdk.android.**{*;}
-keep class com.ut.**{*;}
-keep class com.ta.**{*;}

#videoCache
-keep class com.danikula.videocache.** {*;}

# Gson
-keepattributes *Annotation*
-dontwarn sun.misc.**
-dontwarn com.google.gson.**
-keep class com.google.gson.**
-keep class com.google.gson.**{*;}
-keep interface com.google.gson.**{*;}
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
#Okhttp3
-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.**{*;}
-keep interface okhttp3.**{*;}
-keep class okhttp3.**
-keep class okhttp3.logging.**
-keep class okio.**
-dontwarn okhttp3.**
-dontwarn okhttp3.logging.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# RxJava RxAndroid
-dontwarn java.util.concurrent.Flow* #Rxjava针对 java9
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

#glid
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

#immersionbar
-keep class com.gyf.immersionbar.* {*;}
-dontwarn com.gyf.immersionbar.**

#gesture-views

#云信
#如果你开启数据库功能，需要加入
-keep class net.sqlcipher.** {*;}

-dontwarn com.netease.**
-keep class com.netease.** {*;}
#如果你使用全文检索插件，需要加入
-dontwarn org.apache.lucene.**
-keep class org.apache.lucene.** {*;}

-keep class com.shizhefei.** {*;}
-keep class com.tulele.im.** {*;}
-keep class com.kuaigeglobal.epnova.common.ait.** {*;}
-keep class com.tbruyelle.** {*;}
-keep class com.dinuscxj.progressbar.** {*;}
-keep class pl.droidsonroids.gif.** {*;}
-keep class org.fmod.** {*;}
-keep class com.snail.antifake.jni.** {*;}

#视频压缩库videocompressor
-keep public class com.vincent.**{*;}

-keep public class **.R$*{
   public static final int *;
}

#----------------------------------第三方包---------------------------------

#--小米--
-dontwarn com.xiaomi.push.**
-keep class com.xiaomi.** {*;}
#--华为--
#-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.hianalytics.android.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}

#--vivo--
-dontwarn com.vivo.push.**
-keep class com.vivo.push.**{*; }
-keep class com.vivo.vms.**{*; }
-keep class com.netease.nimlib.mixpush.vivo.VivoPush* {*;}
-keep class com.netease.nimlib.mixpush.vivo.VivoPushReceiver{*;}

#--oppo--
-keep public class * extends android.app.Service

#--huawei--
-keep class com.hianalytics.android.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}
######################阿里云短视频混淆配置#########################
-keep class com.aliyun.**{*;}
-keep class com.duanqu.**{*;}
-keep class com.qu.**{*;}
-keep class com.alibaba.**{*;}
-keep class component.alivc.**{*;}
-keep class com.alivc.**{*;}

-keep class com.alivc.**{*;}
-keep class com.aliyun.**{*;}
-keep class com.cicada.**{*;}
-dontwarn com.alivc.**
-dontwarn com.aliyun.**
-dontwarn com.cicada.**
# sdk
-keep class com.bun.miitmdid.** { *; }
-keep interface com.bun.supplier.** { *; }
# asus
-keep class com.asus.msa.SupplementaryDID.** { *; }
-keep class com.asus.msa.sdid.** { *; }
# freeme
-keep class com.android.creator.** { *; }
-keep class com.android.msasdk.** { *; }
# huawei
-keep class com.huawei.hms.ads.** { *; }
-keep interface com.huawei.hms.ads.** {*; }
# lenovo
-keep class com.zui.deviceidservice.** { *; }
-keep class com.zui.opendeviceidlibrary.** { *; }
# meizu
-keep class com.meizu.flyme.openidsdk.** { *; }
# nubia
-keep class com.bun.miitmdid.provider.nubia.NubiaIdentityImpl { *; }
# oppo
-keep class com.heytap.openid.** { *; }
# samsung
 -keep class com.samsung.android.deviceidservice.** { *; } # vivo
-keep class com.vivo.identifier.** { *; }
# xiaomi
-keep class com.bun.miitmdid.provider.xiaomi.IdentifierManager { *; }
# zte
-keep class com.bun.lib.** { *; }
# coolpad
-keep class com.coolpad.deviceidsupport.** { *; }
#巨量
-keep class com.bytedance.ads.convert.broadcast.common.EncryptionTools {*;}
######################短视频混淆配置#########################
-keep class com.aliyun.**{*;}
-keep class com.duanqu.**{*;}
-keep class com.qu.**{*;}
-keep class com.alibaba.**{*;}
-keep class component.alivc.**{*;}
-keep class com.alivc.**{*;}
-keep class org.webrtc.**{*;}
-keep class org.ugsv.**{*;}

-keepattributes Signature
-keepattributes Exceptions,InnerClasses

-keep class ms.bz.bd.**{ *; }
-keep class com.volcengine.mobsecBiz.**{ *; }
-keep class com.bytedance.frameworks.encryptor.EncryptorUtil { *; }

######################pdf加载器#########################
-keep class com.shockwave.**
######################pdf加载器#########################

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.applet.Applet
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn javax.sound.sampled.AudioFormat
-dontwarn javax.sound.sampled.AudioSystem
-dontwarn javax.sound.sampled.DataLine$Info
-dontwarn javax.sound.sampled.Line$Info
-dontwarn javax.sound.sampled.Line
-dontwarn javax.sound.sampled.LineUnavailableException
-dontwarn javax.sound.sampled.SourceDataLine
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry


######################GrowingIO#########################
-keep class com.growingio.** {
    *;
}
-dontwarn com.growingio.**
######################GrowingIO#########################

-keep class ru.noties.jlatexmath.** { *; }


# -------------------------------
# Firebase Analytics & Firebase SDK
# -------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keepattributes *Annotation*

# -------------------------------
# Google Sign-In
# -------------------------------
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }

# -------------------------------
# Google Play Billing / IAP
# -------------------------------
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# 1. 保留 Google Play Services / AppSet ID 相关
-keep class com.google.android.gms.appset.** { *; }
-keep interface com.google.android.gms.appset.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# 2. 保留系统底层 MediaDrm 及其回调逻辑
# 虽然是系统 API，但为了防止某些混淆优化破坏底层调用链路，建议保留
-keep class android.media.MediaDrm { *; }
-keep class android.media.NotProvisionedException { *; }
-keep class android.media.UnsupportedSchemeException { *; }

# 3. 针对 Settings.Secure 访问
-keep class android.provider.Settings$Secure { *; }

# 4. 保留我们自己定义的设备指纹数据类（避免服务端解析 JSON 失败）
-keepclassmembers class com.kuaigeglobal.epnova.core.manager.OverseasDeviceFingerprint {
    <fields>;
}

# 保护 AppsFlyer 核心类
-keep class com.appsflyer.** { *; }
# 保护所有 DeepLink 相关的接口和实现类
-keep interface com.appsflyer.deeplink.** { *; }
-keep class com.appsflyer.deeplink.** { *; }
# 避免移除某些用于归因的属性
-keepattributes Signature
-keepattributes *Annotation*


# Facebook SDK 基础规则
-keep class com.facebook.** { *; }
-keepattributes Signature

# 保持特定的接口和方法不被混淆，确保反射调用正常
-keepclassmembers class * extends javax.inject.Provider {
    @javax.inject.Inject <init>(...);
}

# 如果你使用了 Facebook 登录或分享
-keep class com.facebook.login.Login { *; }
-keep class com.facebook.common.Common { *; }

# 保持 Custom Tab 相关的类（Facebook 网页登录依赖此项）
-keep class com.facebook.CustomTabActivity { *; }