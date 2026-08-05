package cn.james.music.data.kugou

import cn.james.music.core.model.account.UserProfile
import cn.james.music.core.model.account.UserProfileError
import cn.james.music.core.model.account.UserProfileRepository
import cn.james.music.core.model.account.UserProfileResult
import cn.james.music.core.model.account.VipSummary
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouUserDetailDto
import cn.james.music.kugou.api.endpoint.KugouUserService
import cn.james.music.kugou.api.endpoint.KugouVipSummaryDto
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.KugouError
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KugouUserProfileRepository
    @Inject
    constructor(
        private val sessionProvider: KugouSessionProvider,
        private val userService: KugouUserService,
    ) : UserProfileRepository {
        override suspend fun load(): UserProfileResult {
            val session =
                when (val initialization = sessionProvider.initialize()) {
                    is KugouInitializationResult.Failure -> {
                        return UserProfileResult.Failure(UserProfileError.SessionInitialization)
                    }

                    is KugouInitializationResult.Ready -> {
                        initialization.session
                    }
                }
            val sessionUserId = session.authenticatedUserId() ?: return UserProfileResult.Anonymous
            return coroutineScope {
                val detail = async { userService.fetchUserDetail(session.requestContext()) }
                val vip = async { userService.fetchVipSummary(session.requestContext()) }
                when (val detailResult = detail.await()) {
                    is KugouApiResult.Failure -> {
                        vip.await()
                        UserProfileResult.Failure(detailResult.error.toProfileError())
                    }

                    is KugouApiResult.Success -> {
                        detailResult.value.toDomain(
                            sessionUserId = sessionUserId,
                            vip = vip.await().toDomain(),
                        )
                    }
                }
            }
        }

        private fun KugouSessionSnapshot.authenticatedUserId(): String? =
            userId?.takeIf { !token.isNullOrBlank() && it.toLongOrNull()?.let { value -> value > 0 } == true }

        private fun KugouUserDetailDto.toDomain(
            sessionUserId: String,
            vip: VipSummary,
        ): UserProfileResult {
            if (userId != null && userId != sessionUserId) {
                return UserProfileResult.Failure(UserProfileError.Protocol)
            }
            return UserProfileResult.Success(
                UserProfile(
                    userId = sessionUserId,
                    nickname = nickname,
                    avatarUrl = avatarUrl.sized(240),
                    backgroundUrl = backgroundUrl.sized(720),
                    signature = signature,
                    fans = fans,
                    follows = follows,
                    listenMinutes = listenMinutes,
                    vip = vip,
                ),
            )
        }

        private fun KugouApiResult<KugouVipSummaryDto>.toDomain(): VipSummary =
            when (this) {
                is KugouApiResult.Failure -> {
                    VipSummary.Unavailable
                }

                is KugouApiResult.Success -> {
                    if (value.isActive) {
                        VipSummary.Active(value.label ?: "VIP")
                    } else {
                        VipSummary.Inactive
                    }
                }
            }

        private fun String?.sized(size: Int): String? =
            this
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.replace("{size}", size.toString())
                ?.replaceFirst("http://", "https://")

        private fun KugouError.toProfileError(): UserProfileError =
            when (this) {
                is KugouError.Network -> {
                    when (kind) {
                        KugouError.Network.Kind.Offline -> UserProfileError.Offline
                        KugouError.Network.Kind.Timeout -> UserProfileError.Timeout
                        KugouError.Network.Kind.Connection -> UserProfileError.Connection
                    }
                }

                is KugouError.Http -> {
                    UserProfileError.ServiceUnavailable
                }

                is KugouError.Risk -> {
                    UserProfileError.VerificationRequired
                }

                is KugouError.Protocol -> {
                    if (reason == KugouError.Protocol.Reason.ServiceRejected) {
                        UserProfileError.Rejected
                    } else {
                        UserProfileError.Protocol
                    }
                }
            }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouUserProfileBindings {
    @Binds
    abstract fun bindUserProfileRepository(implementation: KugouUserProfileRepository): UserProfileRepository
}
