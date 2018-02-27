package org.abimon.emojithing

import org.abimon.heavensHarmony.buffer
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer

fun IChannel.queueMessage(msg: String): RequestBuffer.RequestFuture<IMessage> = buffer { sendMessage(msg) }
fun IChannel.queueMessage(embed: EmbedObject): RequestBuffer.RequestFuture<IMessage> = buffer { sendMessage(embed) }
fun IChannel.queueMessage(msg: String, embed: EmbedObject): RequestBuffer.RequestFuture<IMessage> = buffer { sendMessage(msg, embed) }
fun IChannel.queueMessage(init: KMessageBuilder.() -> Unit): RequestBuffer.RequestFuture<IMessage> = buffer { sendMessage(init) }

fun IChannel.queueMessageAndWait(msg: String): IMessage = buffer { sendMessage(msg) }.get()
fun IChannel.queueMessageAndWait(embed: EmbedObject): IMessage = buffer { sendMessage(embed) }.get()
fun IChannel.queueMessageAndWait(msg: String, embed: EmbedObject): IMessage = buffer { sendMessage(msg, embed) }.get()
fun IChannel.queueMessageAndWait(init: KMessageBuilder.() -> Unit): IMessage = buffer { sendMessage(init) }.get()

fun IChannel.sendMessage(embed: KMessageBuilder.() -> Unit): IMessage {
    val builder = KMessageBuilder()
    builder.embed()
    builder.channel = this
    return builder.send()
}

fun Any.deepEquals(any: Any): Boolean {
    val clazz = this::class.java
    if(!clazz.isInstance(any))
        return false

    if(clazz.methods.any { method -> method.name == "equals" && method.declaringClass == clazz })
        return clazz.methods.first { method -> method.name == "equals" && method.declaringClass == clazz }.invoke(this, any) as Boolean

    val properties = clazz.fields
    val a = properties.map { property -> property.name to property.get(this) }
    val b = properties.map { property -> property.name to property.get(any) }.toMap()

    return a.all { (key, value) -> value == b[key] || b[key]?.deepEquals(value ?: return@all false) ?: value == null }
}

fun String.breakDown(): List<String> {
    val list = ArrayList<String>()
    var curr = ""

    for(line in split('\n')) {
        if(line.length + curr.length > 1900) {
            list.add(curr.trim())
            curr = ""
        }
        curr += line + '\n'
    }
    if(curr.isNotBlank())
        list.add(curr.trim())

    return list
}