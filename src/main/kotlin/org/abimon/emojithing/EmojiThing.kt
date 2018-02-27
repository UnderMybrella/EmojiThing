package org.abimon.emojithing

import com.fasterxml.jackson.module.kotlin.readValue
import org.abimon.dArmada.MessageSpy
import org.abimon.dArmada.ServerData
import org.abimon.heavensHarmony.*
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import java.io.File

object EmojiThing: HeavensBot() {

    val JSON_CONFIG_FILE = File("config.json")
    val YAML_CONFIG_FILE = File("config.yaml")

    val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:57.0) Gecko/20100101 Firefox/57.0"

    override val config: HeavensConfig = when {
        JSON_CONFIG_FILE.exists() -> HeavensBot.MAPPER.readValue(JSON_CONFIG_FILE)
        YAML_CONFIG_FILE.exists() -> HeavensBot.YAML_MAPPER.readValue(YAML_CONFIG_FILE)
        else -> throw IllegalStateException("Neither $JSON_CONFIG_FILE or $YAML_CONFIG_FILE exist!")
    }

    override val client: IDiscordClient

    override val database: Database = Database(this)
    override val encryption: EncryptionWrapper = EncryptionWrapper(this).apply {
        ServerData.DECRYPT = this::decrypt
        ServerData.ENCRYPT = this::encrypt
    }

    val persistent = PersistentStates(this)

    @JvmStatic
    fun main(args: Array<String>) {
        if ("-Xdecrypt" in args) {
            File("server_data").listFiles { file -> file.isDirectory && file.name.matches("\\d+".toRegex()) }.forEach { dir ->
                val serverID = dir.name.toLong()

                dir.listFiles().forEach { file -> File(file.absolutePath.substringBeforeLast('.') + ".decrypt.${file.extension}").writeBytes(encryption.decrypt(file.readBytes(), serverID, file.name)) }
            }
        }
    }

    init {
        HeavensBot.INSTANCE = this

        imperator.hireSpy(MessageSpy())
        imperator.hireSoldier(StateDatabase.messageSent)

        imperator.hireSoldiers(BigMoji)

        imperator.hireSoldier(MenuDatabase.reactionAdded)

        client = ClientBuilder()
                .withToken(config.token)
                .registerListener(discordScout)
                .registerListener { event -> (event as? ReadyEvent)?.client?.changePresence(StatusType.ONLINE, ActivityType.PLAYING, null) }
                .setPresence(StatusType.DND, ActivityType.LISTENING, "the startup tone")
                .login()
    }
}