package com.kuaigeglobal.epnova.feature.recharge

import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.request.CreateOrderBody
import com.kuaigeglobal.epnova.core.network.apis.request.CreatePointOrderBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoinRepository : AppRepository() {

    /**
     * 金币商品配置列表
     */
    suspend fun coinConfigList() = withContext(Dispatchers.IO) { api.coinsConfigList(UserTokenBody()) }

    /**
     * Credits（算力）商品配置列表
     */
    suspend fun pointConfigList() = withContext(Dispatchers.IO) { api.pointConfigList(UserTokenBody()) }

    /**
     * 创建 Google 支付订单（金币类商品，Android 端通用）
     * @param goodsCode 商品编号（算力配置表编号）
     */
    suspend fun createGoogleOrder(goodsCode: String?) = withContext(Dispatchers.IO) {
        api.createOrder(CreateOrderBody(goodsCode = goodsCode, goodsType = 1, mobileType = 1))
    }

    /**
     * 交易支付方式配置列表
     */
    suspend fun paymentConfigList() = withContext(Dispatchers.IO) { api.paymentConfigList(UserTokenBody()) }

    /**
     * 创建app支付宝支付订单
     * @param goodsCode 商品编号目前是算力配置表的编号
     * @param goodsType 商品类型1-算力
     * @param payMerchantConfigId 支付商配置表ID
     */
    suspend fun createOrderAlipay(goodsCode: String?, goodsType: Int?, payMerchantConfigId: String?) = withContext(Dispatchers.IO) {
        api.createOrderAlipay(CreatePointOrderBody(goodsCode, goodsType, payMerchantConfigId))
    }

    /**
     * 创建app微信支付订单
     * @param goodsCode 商品编号目前是算力配置表的编号
     * @param goodsType 商品类型1-算力
     * @param payMerchantConfigId 支付商配置表ID
     */
    suspend fun createOrderWechat(goodsCode: String?, goodsType: Int?, payMerchantConfigId: String?) = withContext(Dispatchers.IO) {
        api.createOrderWechat(CreatePointOrderBody(goodsCode, goodsType, payMerchantConfigId))
    }

}