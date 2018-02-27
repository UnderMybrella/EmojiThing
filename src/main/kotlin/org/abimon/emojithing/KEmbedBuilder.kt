package org.abimon.emojithing

import org.abimon.heavensHarmony.buffer
import org.abimon.heavensHarmony.bufferAndWait
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.AttachmentPartEntry
import sx.blah.discord.util.RequestBuffer
import java.awt.Color
import java.io.InputStream

open class KMessageBuilder {
    companion object {
        val BUT_NOT_BLACK = "2C2F33".toInt(16)
        val STATIC_DEFAULT_EMBED = defaultEmbed
        val defaultEmbed: EmbedObject
            get() = EmbedObject(null, "rich", null, null, null, BUT_NOT_BLACK, null, null, null, null, null,
                    null, null)
    }

    var embed: EmbedObject = EmbedObject(null, "rich", null, null, null, BUT_NOT_BLACK, null, null, null, null, null,
            null, null)

    var title: String?
        get() = embed.title
        set(value) {
            embed.title = value
        }
    var type: String
        get() = embed.type
        set(value) {
            embed.type = value
        }
    var description: String
        get() = embed.description
        set(value) {
            embed.description = value
        }
    var url: String
        get() = embed.url
        set(value) {
            embed.url = value
        }

    var timestamp: String?
        get() = embed.timestamp
        set(value) {
            embed.timestamp = value
        }
    var color: Color
        get() = Color(embed.color)
        set(value) {
            embed.color = (value.red and 0xFF shl 16) or (value.green and 0xFF shl 8) or (value.blue and 0xFF)
        }

    var footer: EmbedObject.FooterObject?
        get() = embed.footer
        set(value) {
            embed.footer = value
        }
    var image: EmbedObject.ImageObject?
        get() = embed.image
        set(value) {
            embed.image = value
        }
    var thumbnail: EmbedObject.ThumbnailObject?
        get() = embed.thumbnail
        set(value) {
            embed.thumbnail = value
        }
    var video: EmbedObject.VideoObject?
        get() = embed.video
        set(value) {
            embed.video = value
        }
    var provider: EmbedObject.ProviderObject?
        get() = embed.provider
        set(value) {
            embed.provider = value
        }
    var author: EmbedObject.AuthorObject?
        get() = embed.author
        set(value) {
            embed.author = value
        }
    var fields: Array<EmbedObject.EmbedFieldObject>?
        get() = embed.fields
        set(value) {
            embed.fields = value
        }

    var thumbnailURL: String?
        get() = thumbnail?.url
        set(value) {
            if (thumbnail == null)
                thumbnail = EmbedObject.ThumbnailObject()
            thumbnail?.url = value
        }

    var footerText: String?
        get() = footer?.text
        set(value) {
            if (footer == null)
                footer = EmbedObject.FooterObject()
            footer?.text = value
        }

    var content: String? = null

    val files: MutableList<Pair<String, InputStream>> = ArrayList()

    var fileName: String? = null
        set(value) {
            val stream = fileStream
            if (stream != null && value != null) {
                files.add(value to stream)

                field = null
                fileStream = null
            } else {
                field = value
            }
        }

    var fileStream: InputStream? = null
        set(value) {
            val name = fileName
            if (name != null && value != null) {
                files.add(name to value)

                fileName = null
                field = null
            } else {
                field = value
            }
        }

    val lastFileName: String?
        get() = files.lastOrNull()?.first

    var channel: IChannel? = null

    fun appendField(field: EmbedObject.EmbedFieldObject) {
        fields = (fields ?: emptyArray()).plus(field)
    }

    fun buildFields(init: MutableList<EmbedObject.EmbedFieldObject>.() -> Unit): Array<EmbedObject.EmbedFieldObject> {
        val list: MutableList<EmbedObject.EmbedFieldObject> = ArrayList()
        list.init()
        return list.toTypedArray()
    }

    fun MutableList<EmbedObject.EmbedFieldObject>.add(init: EmbedObject.EmbedFieldObject.() -> Unit): Unit {
        val field = EmbedObject.EmbedFieldObject()
        field.init()
        this.add(field)
    }

    fun send(): IMessage {
        val channel = this.channel ?: throw IllegalStateException("No channel defined")
        val content = this.content
        val embed = this.embed
        val files = this.files.map { (name, stream) -> AttachmentPartEntry(name, stream) }.toTypedArray()

        if (content == null) {
            if (embed.deepEquals(STATIC_DEFAULT_EMBED)) {
                if (files.isEmpty())
                    throw IllegalStateException("No content defined! Need either content, an embed, or a file(s)!")
                else
                    return channel.sendFiles("", *files)
            } else {
                if (files.isEmpty())
                    return channel.sendMessage(embed)
                else
                    return channel.sendFiles(embed, *files)
            }
        } else {
            if (embed.deepEquals(STATIC_DEFAULT_EMBED)) {
                if (files.isEmpty())
                    return channel.sendMessage(content)
                else
                    return channel.sendFiles(content, *files)
            } else {
                if (files.isEmpty())
                    return channel.sendMessage(content, embed)
                else
                    return channel.sendFiles(content, false, embed, *files)
            }
        }
    }

    fun buffer(): RequestBuffer.RequestFuture<IMessage> = buffer { send() }
    fun bufferAndWait(): IMessage = bufferAndWait { send() }
}

fun embed(init: KMessageBuilder.() -> Unit): EmbedObject {
    val embedBuilder = KMessageBuilder()
    embedBuilder.init()
    return embedBuilder.embed
}

fun bufferMessage(init: KMessageBuilder.() -> Unit): RequestBuffer.RequestFuture<IMessage> {
    val builder = KMessageBuilder()
    builder.init()
    return builder.buffer()
}

fun bufferMessageAndWait(init: KMessageBuilder.() -> Unit): IMessage {
    val builder = KMessageBuilder()
    builder.init()
    return builder.bufferAndWait()
}

fun message(init: KMessageBuilder.() -> Unit): IMessage {
    val builder = KMessageBuilder()
    builder.init()
    return builder.send()
}