package com.example.loldiscordbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class LoLDiscordBotApplication {

	public static void main(String[] args) {
		String discordBotToken = System.getenv("DISCORD_BOT_TOKEN");
		JDABuilder.createDefault(discordBotToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
				.setActivity(Activity.playing("메시지를 요청하라냥~"))
				.addEventListeners(new LolBot())
				.build();
	}
}
