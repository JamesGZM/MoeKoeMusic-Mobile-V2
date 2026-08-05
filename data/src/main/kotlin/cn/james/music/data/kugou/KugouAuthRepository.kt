package cn.james.music.data.kugou

import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthRepository
import cn.james.music.core.model.auth.AuthState
import cn.james.music.core.model.auth.MobileCodeLoginResult
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouAuthenticationClient
import cn.james.music.kugou.api.endpoint.KugouMobileLoginResult
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionMutationResult
import cn.james.music.kugou.api.session.KugouSessionMutator
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.KugouError
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KugouAuthRepository
    @Inject
    constructor(
        private val sessionProvider: KugouSessionProvider,
        private val sessionMutator: KugouSessionMutator,
        private val authClient: KugouAuthenticationClient,
    ) : AuthRepository {
        private val mutationMutex = Mutex()

        override suspend fun currentState(): AuthState =
            when (val initialization = sessionProvider.initialize()) {
                is KugouInitializationResult.Failure -> AuthState.Unavailable(AuthError.SessionInitialization)
                is KugouInitializationResult.Ready -> initialization.session.toAuthState()
            }

        override suspend fun sendMobileCode(mobile: String): AuthActionResult {
            val session = readySession() ?: return AuthActionResult.Failure(AuthError.SessionInitialization)
            return when (val result = authClient.sendMobileCode(mobile, session.requestContext())) {
                is KugouApiResult.Failure -> AuthActionResult.Failure(result.error.toAuthError())
                is KugouApiResult.Success -> AuthActionResult.Success
            }
        }

        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): MobileCodeLoginResult =
            mutationMutex.withLock {
                val session = readySession() ?: return@withLock MobileCodeLoginResult.Failure(AuthError.SessionInitialization)
                when (val result = authClient.loginWithMobileCode(mobile, code, selectedUserId, session.requestContext())) {
                    is KugouMobileLoginResult.Failure -> {
                        MobileCodeLoginResult.Failure(result.error.toAuthError())
                    }

                    is KugouMobileLoginResult.MultipleAccounts -> {
                        MobileCodeLoginResult.MultipleAccounts(
                            result.accounts.map { account ->
                                AuthAccountOption(
                                    userId = account.userId,
                                    nickname = account.nickname,
                                    avatarUrl = account.avatarUrl,
                                    grade = account.grade,
                                )
                            },
                        )
                    }

                    is KugouMobileLoginResult.Authenticated -> {
                        val authenticated = result.session
                        val updated =
                            session.copy(
                                token = authenticated.token,
                                userId = authenticated.userId,
                                cookies = KugouCookies.from(session.cookies.asMap() + authenticated.cookies.asMap()),
                            )
                        when (sessionMutator.replace(updated)) {
                            KugouSessionMutationResult.StorageFailure -> MobileCodeLoginResult.Failure(AuthError.Storage)
                            is KugouSessionMutationResult.Updated -> MobileCodeLoginResult.Authenticated
                        }
                    }
                }
            }

        override suspend fun logout(): AuthActionResult =
            mutationMutex.withLock {
                val session = readySession() ?: return@withLock AuthActionResult.Failure(AuthError.SessionInitialization)
                val anonymous =
                    session.copy(
                        token = null,
                        userId = null,
                        cookies = KugouCookies.from(session.cookies.asMap().filterKeys { it !in AUTH_COOKIE_NAMES }),
                    )
                when (sessionMutator.replace(anonymous)) {
                    KugouSessionMutationResult.StorageFailure -> AuthActionResult.Failure(AuthError.Storage)
                    is KugouSessionMutationResult.Updated -> AuthActionResult.Success
                }
            }

        private suspend fun readySession(): KugouSessionSnapshot? =
            when (val initialization = sessionProvider.initialize()) {
                is KugouInitializationResult.Failure -> null
                is KugouInitializationResult.Ready -> initialization.session
            }

        private fun KugouSessionSnapshot.toAuthState(): AuthState =
            userId.orEmpty().let { stableUserId ->
                if (!token.isNullOrBlank() && stableUserId.isNotBlank() && stableUserId != "0") {
                    AuthState.Authenticated(stableUserId)
                } else {
                    AuthState.Anonymous
                }
            }

        private fun KugouError.toAuthError(): AuthError =
            when (this) {
                is KugouError.Network -> {
                    when (kind) {
                        KugouError.Network.Kind.Offline -> AuthError.Offline
                        KugouError.Network.Kind.Timeout -> AuthError.Timeout
                        KugouError.Network.Kind.Connection -> AuthError.Connection
                    }
                }

                is KugouError.Http -> {
                    AuthError.ServiceUnavailable
                }

                is KugouError.Protocol -> {
                    if (reason ==
                        KugouError.Protocol.Reason.ServiceRejected
                    ) {
                        AuthError.Rejected
                    } else {
                        AuthError.Protocol
                    }
                }

                is KugouError.Risk -> {
                    AuthError.Rejected
                }
            }

        private companion object {
            val AUTH_COOKIE_NAMES = setOf("token", "userid", "t1", "vip_type", "vip_token")
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouAuthBindings {
    @Binds
    abstract fun bindAuthRepository(implementation: KugouAuthRepository): AuthRepository
}
