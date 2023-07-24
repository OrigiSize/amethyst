package com.vitorpamplona.amethyst.service.model

import androidx.compose.runtime.Immutable
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.TimeUtils
import com.vitorpamplona.amethyst.model.toHexKey
import com.vitorpamplona.amethyst.service.Utils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet

@Immutable
class PeopleListEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : GeneralListEvent(id, pubKey, createdAt, kind, tags, content, sig) {
    var publicAndPrivateUserCache: ImmutableSet<HexKey>? = null

    fun publicAndPrivateUsers(privateKey: ByteArray?): ImmutableSet<HexKey> {
        publicAndPrivateUserCache?.let {
            return it
        }

        val privateUserList = privateKey?.let {
            privateTagsOrEmpty(privKey = it).filter { it.size > 1 && it[0] == "p" }.map { it[1] }.toSet()
        } ?: emptySet()
        val publicUserList = tags.filter { it.size > 1 && it[0] == "p" }.map { it[1] }.toSet()

        publicAndPrivateUserCache = (privateUserList + publicUserList).toImmutableSet()

        return publicAndPrivateUserCache ?: persistentSetOf()
    }

    fun isTaggedUser(idHex: String, isPrivate: Boolean, privateKey: ByteArray): Boolean {
        return if (isPrivate) {
            privateTagsOrEmpty(privKey = privateKey).any { it.size > 1 && it[0] == "p" && it[1] == idHex }
        } else {
            isTaggedUser(idHex)
        }
    }

    companion object {
        const val kind = 30000
        const val blockList = "mute"

        fun createListWithUser(name: String, pubKeyHex: String, isPrivate: Boolean, privateKey: ByteArray, createdAt: Long = TimeUtils.now()): PeopleListEvent {
            return if (isPrivate) {
                create(
                    content = encryptTags(listOf(listOf("p", pubKeyHex)), privateKey),
                    tags = listOf(listOf("d", name)),
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            } else {
                create(
                    content = "",
                    tags = listOf(listOf("d", name), listOf("p", pubKeyHex)),
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            }
        }

        fun addUsers(earlierVersion: PeopleListEvent, listPubKeyHex: List<String>, isPrivate: Boolean, privateKey: ByteArray, createdAt: Long = TimeUtils.now()): PeopleListEvent {
            return if (isPrivate) {
                create(
                    content = encryptTags(
                        privateTags = earlierVersion.privateTagsOrEmpty(privKey = privateKey).plus(
                            listPubKeyHex.map {
                                listOf("p", it)
                            }
                        ),
                        privateKey = privateKey
                    ),
                    tags = earlierVersion.tags,
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            } else {
                create(
                    content = earlierVersion.content,
                    tags = earlierVersion.tags.plus(
                        listPubKeyHex.map {
                            listOf("p", it)
                        }
                    ),
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            }
        }

        fun addUser(earlierVersion: PeopleListEvent, pubKeyHex: String, isPrivate: Boolean, privateKey: ByteArray, createdAt: Long = TimeUtils.now()): PeopleListEvent {
            if (earlierVersion.isTaggedUser(pubKeyHex, isPrivate, privateKey)) return earlierVersion

            return if (isPrivate) {
                create(
                    content = encryptTags(
                        privateTags = earlierVersion.privateTagsOrEmpty(privKey = privateKey).plus(element = listOf("p", pubKeyHex)),
                        privateKey = privateKey
                    ),
                    tags = earlierVersion.tags,
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            } else {
                create(
                    content = earlierVersion.content,
                    tags = earlierVersion.tags.plus(element = listOf("p", pubKeyHex)),
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            }
        }

        fun removeUser(earlierVersion: PeopleListEvent, pubKeyHex: String, isPrivate: Boolean, privateKey: ByteArray, createdAt: Long = TimeUtils.now()): PeopleListEvent {
            if (!earlierVersion.isTaggedUser(pubKeyHex, isPrivate, privateKey)) return earlierVersion

            return if (isPrivate) {
                create(
                    content = encryptTags(
                        privateTags = earlierVersion.privateTagsOrEmpty(privKey = privateKey).filter { it.size > 1 && it[1] != pubKeyHex },
                        privateKey = privateKey
                    ),
                    tags = earlierVersion.tags.filter { it.size > 1 && it[1] != pubKeyHex },
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            } else {
                create(
                    content = earlierVersion.content,
                    tags = earlierVersion.tags.filter { it.size > 1 && it[1] != pubKeyHex },
                    privateKey = privateKey,
                    createdAt = createdAt
                )
            }
        }

        fun create(content: String, tags: List<List<String>>, privateKey: ByteArray, createdAt: Long = TimeUtils.now()): PeopleListEvent {
            val pubKey = Utils.pubkeyCreate(privateKey).toHexKey()
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return PeopleListEvent(id.toHexKey(), pubKey, createdAt, tags, content, sig.toHexKey())
        }
    }
}
