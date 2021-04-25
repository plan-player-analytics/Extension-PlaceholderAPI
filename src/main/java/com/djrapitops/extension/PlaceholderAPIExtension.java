/*
    Copyright(c) 2021 AuroraLS3

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.djrapitops.extension;

import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.DataBuilderProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.builder.ExtensionDataBuilder;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.query.QueryService;
import com.djrapitops.plan.settings.SettingsService;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DataExtension.
 *
 * @author AuroraLS3
 */
@PluginInfo(name = "PlaceholderAPI", iconName = "user-edit", iconFamily = Family.SOLID, color = Color.LIGHT_BLUE)
public class PlaceholderAPIExtension implements DataExtension {

    private List<String> trackedPlaceholders;

    public PlaceholderAPIExtension() {
        trackedPlaceholders = SettingsService.getInstance().getStringList("PlaceholderAPI.Placeholders", () -> Collections.singletonList("%example_placeholder%"));
    }

    public PlaceholderAPIExtension(boolean forTesting) {}

    public String formatTextAsIdentifier(String text) {
        return removeEnds(text).toLowerCase().replaceAll("\\s", "");
    }

    private String removeEnds(String text) {
        return text.substring(1, text.length() - 1);
    }

    private List<String> fetchStoredPlaceholders() {
        QueryService queryService = QueryService.getInstance();
        UUID serverUUID = queryService.getServerUUID().orElseThrow(NotReadyException::new);

        String sql = "SELECT r.name as placeholder FROM plan_extension_providers r" +
                " JOIN plan_extension_plugins p on p.id=r.plugin_id" +
                " WHERE p.name=?" +
                " AND p.server_uuid=?";
        return queryService.query(sql, statement -> {
            statement.setString(1, "PlaceholderAPI");
            statement.setString(2, serverUUID.toString());
            try (ResultSet set = statement.executeQuery()) {
                List<String> storedPlaceholders = new ArrayList<>();
                while (set.next()) storedPlaceholders.add(set.getString("placeholder"));
                return storedPlaceholders;
            }
        });
    }

    @DataBuilderProvider
    public ExtensionDataBuilder playerPlaceholders(UUID playerUUID) {
        ExtensionDataBuilder builder = newExtensionDataBuilder();

        // Remove placeholders that are no longer tracked
        List<String> stored = fetchStoredPlaceholders();
        trackedPlaceholders.stream()
                .filter(placeholder -> placeholder.length() > 2)
                .map(this::formatTextAsIdentifier).forEach(stored::remove);
        for (String toRemove : stored) {
            builder.invalidateValue(toRemove);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return builder;

        // Add new values of placeholders
        for (String trackedPlaceholder : trackedPlaceholders) {
            String value = PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(playerUUID), trackedPlaceholder);
            builder.addValue(String.class, valueBuilder(removeEnds(trackedPlaceholder))
                    .description("Value of " + trackedPlaceholder)
                    .icon(Icon.called("dot-circle").of(Color.LIGHT_BLUE).build())
                    .buildString(value));
        }

        return builder;
    }
}