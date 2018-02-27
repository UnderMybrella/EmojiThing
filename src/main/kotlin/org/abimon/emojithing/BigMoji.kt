package org.abimon.emojithing

import org.abimon.heavensHarmony.HeavenCommand
import org.abimon.heavensHarmony.buffer
import org.abimon.heavensHarmony.bufferAndWait
import org.abimon.heavensHarmony.permissions.EnumMemberStatus
import sx.blah.discord.api.internal.json.objects.EmbedObject
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO

object BigMoji {
    val bigmoji = HeavenCommand("Bigmoji", "bigmoji", EnumMemberStatus.USER.permission) {
        if(params.size < 3) {
            buffer { channel.sendMessage("Syntax: ${prefix}bigmoji [emoji name] [columns]") }
            return@HeavenCommand
        }

        val emojiName = params[1]
        val columnCount = params[2].toIntOrNull() ?: 10

        val emojis = client.guilds.flatMap { guild -> guild.emojis }
                .filter { emoji -> emoji.name.matches("\\Q$emojiName\\E\\d+".toRegex()) }
                .sortedBy { emoji -> emoji.name.replace(emojiName, "").toIntOrNull() ?: 1 }
                .chunked(columnCount)
                .joinToString("\n") { emojiList -> emojiList.joinToString("", postfix = "\u200B") { emoji -> emoji.toString() } }

        val lines = emojis.breakDown().filter { line -> line.length in 1..1999 }

        lines.forEachIndexed { index, line ->
            bufferAndWait { channel.sendMessage(line) }
            Thread.sleep((((lines.size - 1) % 5 + 1) / 5 * 1000).toLong())
        }
    }

    val listmoji = HeavenCommand("Listmoji", "listmoji", EnumMemberStatus.USER.permission) {
        val emojis = client.guilds.flatMap { guild -> guild.emojis }
                .filter { emoji -> emoji.name.matches("\\w+\\d+".toRegex()) }
                .groupBy { emoji -> emoji.name.replace("\\d".toRegex(), "") }
                .filter { (_, emojiList) -> emojiList.size > 1 }
                .entries.joinToString("\n", prefix = "Emoji List:\n") { (emojiName, emojiList) -> "$emojiName (${emojiList.size})" }

        emojis.breakDown().forEach { line -> bufferAndWait { channel.sendMessage(line) } }
    }

    val stitchmoji = HeavenCommand("Stitchmoji", "stitchmoji", EnumMemberStatus.USER.permission) {
        if(params.size < 3) {
            buffer { channel.sendMessage("Syntax: ${prefix}bigmoji [emoji name] [columns]") }
            return@HeavenCommand
        }

        val emojiName = params[1]
        val columnCount = params[2].toIntOrNull() ?: 10

        val emojis = client.guilds.flatMap { guild -> guild.emojis }
                .filter { emoji -> emoji.name.matches("\\Q$emojiName\\E\\d+".toRegex()) }
                .sortedBy { emoji -> emoji.name.replace(emojiName, "").toIntOrNull() ?: 1 }

        if(emojis.any { emoji -> emoji.isAnimated }) {
            buffer { channel.sendMessage("Cannot stitch together animated emojis!") }
            return@HeavenCommand
        }

        val keystone = emojis[0].let { emoji ->
            val http = URL(emoji.imageUrl).openConnection()
            http.setRequestProperty("User-Agent", EmojiThing.USER_AGENT)
            return@let ImageIO.read(http.getInputStream())
        }

        val width = keystone.width
        val height = keystone.height

        val img = BufferedImage(width * columnCount, height * (emojis.size / columnCount + (emojis.size % columnCount).coerceAtMost(1)), BufferedImage.TYPE_INT_ARGB)
        val g = img.graphics

        emojis.forEachIndexed { index, emoji ->
            val x = index % columnCount
            val y = index / columnCount

            val http = URL(emoji.imageUrl).openConnection()
            http.setRequestProperty("User-Agent", EmojiThing.USER_AGENT)
            val emojiImage = ImageIO.read(http.getInputStream())

            g.drawImage(emojiImage, x * width, y * height, null)
        }

        g.dispose()

        val data = ByteArrayOutputStream()
        ImageIO.write(img, "PNG", data)

        if(data.size() > 7.8 * 1000 * 1000) {
            buffer { channel.sendMessage("Stitched image too large to send back!") }
            return@HeavenCommand
        }

        buffer {
            channel.sendMessage {
                title = "$emojiName: ${columnCount}x${emojis.size / columnCount} emojis; ${img.width}x${img.height} px"

                fileStream = ByteArrayInputStream(data.toByteArray())
                fileName = "$emojiName.png"

                image = EmbedObject.ImageObject("attachment://$emojiName.png", null, img.width, img.height)
            }
        }
    }
}