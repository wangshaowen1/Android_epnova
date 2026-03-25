package com.kuaigeglobal.epnova.core.google.login

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GoogleSignInHelper(private val context: Context) {

    companion object {
        //客户端 ID 707414526754-2f9285e8rsekh8t38n4e7r32qvliks0e.apps.googleusercontent.com707414526754-2f9285e8rsekh8t38n4e7r32qvliks0e.apps.googleusercontent.com
        const val CLIENT_ID = "707414526754-o32uj00k6kh4v540ncerse069nf112c0.apps.googleusercontent.com"
        const val key = "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"
    }

    private val credentialManager by lazy { CredentialManager.create(context) }
    private val tag = "GoogleAuthHelper"

    private fun getGoogleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setServerClientId(CLIENT_ID)//WEB应用客户端ID
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .build()
    }

    fun signIn(
        onSuccess: (body: LoginByThirdBody) -> Unit,
        onFailure: (exception: CharSequence?) -> Unit
    ) {

        //就这个方式可以切换账号，添加新账号
        val getSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = CLIENT_ID
        ).build()

        //四种方式
        val request = GetCredentialRequest.Builder()
            //.addCredentialOption(getGoogleIdOption()) // 直接调用公开方法
            .addCredentialOption(getSignInWithGoogleOption)
            //.addCredentialOption(GetPasswordOption())//密码方式
            //.addCredentialOption(GetPublicKeyCredentialOption(requestJson = "requestJson"))//密钥凭证方式
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = credentialManager.getCredential(context, request)
                handleSignInResult(response, onSuccess, onFailure)
            } catch (e: GetCredentialException) {
                if (e is NoCredentialException) {
                    Log.d(tag, "NoCredentialException: $e")
                } else {
                    Log.d(tag, "GetCredentialException: $e")
                }
                onFailure(e.errorMessage)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun prepare() {
        coroutineScope {
            credentialManager.prepareGetCredential(
                GetCredentialRequest(
                    listOf(
                        GetPublicKeyCredentialOption(requestJson = "requestJson"),
                        GetPasswordOption()
                    )
                )
            )
        }
    }

    private fun handleSignInResult(
        response: GetCredentialResponse,
        onSuccess: (LoginByThirdBody) -> Unit,
        onFailure: (CharSequence?) -> Unit
    ) {
        when (val credential = response.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(tag, "GoogleIdTokenCredential: googleCred.id=" + googleCred.id)
                        Log.d(tag, "GoogleIdTokenCredential: googleCred.idToken=" + googleCred.idToken)
                        Log.d(tag, "GoogleIdTokenCredential: googleCred.phoneNumber=" + googleCred.phoneNumber)
                        Log.d(tag, "GoogleIdTokenCredential: googleCred.givenName=" + googleCred.givenName)
                        Log.d(tag, "GoogleIdTokenCredential: googleCred.familyName=" + googleCred.familyName)
                        Log.d(tag, "GoogleIdTokenCredential: googleCred.profilePictureUri=" + googleCred.profilePictureUri)
                        Log.d(tag, "GoogleIdTokenCredential: googleCred.profilePictureUri=" + googleCred.profilePictureUri?.toString())
//                        onSuccess(
//                            LoginByThirdBody(
//                                googleCred.idToken,
//                                googleCred.givenName + " " + googleCred.familyName,
//                                googleCred.profilePictureUri?.toString()
//                            )
//                        )
                    } catch (e: Exception) {
                        onFailure(e.message)
                    }
                } else {
                    onFailure("CustomCredential Unsupported credential type=" + credential.type)
                }
            }
            // 其他类型凭证（如密码/公钥）处理 :cite[2]
            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                // 发送到服务器验证
                Log.d(tag, "PasswordCredential: username=$username password=$password")
            }

            is PublicKeyCredential -> {
                val json = credential.authenticationResponseJson
                Log.d(tag, "PublicKeyCredential: json=$json")
                // WebAuthn 场景处理
            }

            else -> onFailure("Unsupported credential type:$credential")
        }
    }

    // 修复点2：替换废弃的 signOut() 方法 :cite[1]
    fun signOut(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 新API清除凭证状态
                credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )
                onComplete(true)
            } catch (e: Exception) {
                Log.d(tag, "clearCredentialStateException: $e")
                // 兼容旧设备回退方案
                try {
                    Identity.getSignInClient(context).signOut()
                    onComplete(true)
                } catch (fallbackEx: Exception) {
                    Log.d(tag, "getSignInClientSignOutException: $e")
                    onComplete(false)
                }
            }
        }
    }
}