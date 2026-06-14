package org.yuemi.bingogacha.plugin.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.BingoGachaApi;
import org.yuemi.bingogacha.api.model.CardTemplate;
import org.yuemi.bingogacha.api.model.PlayerCard;
import org.yuemi.bingogacha.plugin.config.BingoConfig;
import org.yuemi.bingogacha.plugin.gui.CardListGui;
import org.yuemi.bingogacha.plugin.gui.GachaGridGui;
import org.yuemi.bingogacha.plugin.service.BingoGachaService;

public class BingoCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final BingoGachaApi api;
    private final BingoGachaService service;
    private final BingoConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public BingoCommand(
            @NotNull JavaPlugin plugin,
            @NotNull BingoGachaApi api,
            @NotNull BingoGachaService service,
            @NotNull BingoConfig config
    ) {
        this.plugin = plugin;
        this.api = api;
        this.service = service;
        this.config = config;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Only players can run this command."));
                return true;
            }
            player.openInventory(new CardListGui(player, api, service, config).getInventory());
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("claim")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Only players can claim cards."));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(miniMessage.deserialize("<red>Usage: /bingo claim <template_id>"));
                return true;
            }
            String templateId = args[1];
            CardTemplate template = api.getTemplate(templateId);
            if (template == null) {
                player.sendMessage(miniMessage.deserialize("<red>Card template '" + templateId + "' not found."));
                return true;
            }
            PlayerCard card = service.claimCard(player, template);
            if (card != null) {
                player.sendMessage(miniMessage.deserialize("<green>Successfully claimed new bingo card: " + template.getTitle()));
                player.openInventory(new GachaGridGui(player, card, template, api, service, config).getInventory());
            } else {
                player.sendMessage(miniMessage.deserialize("<red>Failed to claim card. You might already have this card active or completed."));
            }
            return true;
        }

        if (sub.equals("open")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Only players can open cards."));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(miniMessage.deserialize("<red>Usage: /bingo open <card_id>"));
                return true;
            }
            try {
                int cardId = Integer.parseInt(args[1]);
                List<PlayerCard> cards = api.getPlayerCardRepository().loadPlayerCards(player.getUniqueId());
                PlayerCard card = cards.stream().filter(c -> c.getId() == cardId).findFirst().orElse(null);
                if (card == null) {
                    player.sendMessage(miniMessage.deserialize("<red>Card ID not found in your active cards."));
                    return true;
                }
                CardTemplate template = api.getTemplate(card.getTemplateId());
                if (template == null) {
                    player.sendMessage(miniMessage.deserialize("<red>Card template not found."));
                    return true;
                }
                player.openInventory(new GachaGridGui(player, card, template, api, service, config).getInventory());
            } catch (NumberFormatException e) {
                player.sendMessage(miniMessage.deserialize("<red>Invalid card ID. Must be an integer."));
            }
            return true;
        }

        if (sub.equals("admin")) {
            if (args.length < 2) {
                sender.sendMessage(miniMessage.deserialize("<red>Usage: /bingo admin <give/reload> ..."));
                return true;
            }
            String adminSub = args[1].toLowerCase();
            if (adminSub.equals("reload")) {
                if (!sender.hasPermission("bingogacha.admin.reload")) {
                    sender.sendMessage(miniMessage.deserialize("<red>You do not have permission."));
                    return true;
                }
                config.load();
                sender.sendMessage(miniMessage.deserialize("<green>Bingo Gacha configurations successfully reloaded!"));
                return true;
            }
            if (adminSub.equals("give")) {
                if (!sender.hasPermission("bingogacha.admin.give")) {
                    sender.sendMessage(miniMessage.deserialize("<red>You do not have permission."));
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(miniMessage.deserialize("<red>Usage: /bingo admin give <player> <template_id>"));
                    return true;
                }
                String targetName = args[2];
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    sender.sendMessage(miniMessage.deserialize("<red>Player '" + targetName + "' is offline."));
                    return true;
                }
                String templateId = args[3];
                CardTemplate template = api.getTemplate(templateId);
                if (template == null) {
                    sender.sendMessage(miniMessage.deserialize("<red>Card template '" + templateId + "' not found."));
                    return true;
                }
                PlayerCard card = service.claimCard(target, template);
                if (card != null) {
                    sender.sendMessage(miniMessage.deserialize("<green>Successfully gave card " + templateId + " to " + target.getName()));
                    target.sendMessage(miniMessage.deserialize("<green>You have been given a new bingo card: " + template.getTitle()));
                } else {
                    sender.sendMessage(miniMessage.deserialize("<red>Failed to give card. Player may already have this card active or completed."));
                }
                return true;
            }
        }

        sender.sendMessage(miniMessage.deserialize("<red>Unknown subcommand. Use /bingo [list/claim/open/admin]"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(List.of("list", "claim", "open"));
            if (sender.hasPermission("bingogacha.admin")) {
                suggestions.add("admin");
            }
            return filterPrefix(suggestions, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("claim")) {
                List<String> templates = api.getTemplates().stream().map(CardTemplate::getId).collect(Collectors.toList());
                return filterPrefix(templates, args[1]);
            }
            if (args[0].equalsIgnoreCase("open") && sender instanceof Player player) {
                List<String> cardIds = api.getPlayerCardRepository().loadPlayerCards(player.getUniqueId()).stream()
                        .map(c -> String.valueOf(c.getId()))
                        .collect(Collectors.toList());
                return filterPrefix(cardIds, args[1]);
            }
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("bingogacha.admin")) {
                return filterPrefix(List.of("give", "reload"), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give") && sender.hasPermission("bingogacha.admin")) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return filterPrefix(players, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give") && sender.hasPermission("bingogacha.admin")) {
            List<String> templates = api.getTemplates().stream().map(CardTemplate::getId).collect(Collectors.toList());
            return filterPrefix(templates, args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
