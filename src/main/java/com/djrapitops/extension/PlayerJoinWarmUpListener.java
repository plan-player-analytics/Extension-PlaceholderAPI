package com.djrapitops.extension;

import com.djrapitops.plan.settings.SettingsService;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;
import java.util.List;

public class PlayerJoinWarmUpListener implements Listener {

    private final List<String> warmedUpPlaceholders;

    public PlayerJoinWarmUpListener() {
        warmedUpPlaceholders = SettingsService.getInstance().getStringList("PlaceholderAPI.Load_these_placeholders_on_join", () -> Collections.singletonList("%plan_server_uuid%"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlaceholderAPI.setPlaceholders(event.getPlayer(), warmedUpPlaceholders.toString());
    }

}
