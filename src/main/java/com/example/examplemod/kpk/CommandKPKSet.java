package com.example.examplemod.command;

import com.example.examplemod.Gender;
import com.example.examplemod.User;
import com.example.examplemod.item.ItemKPK;
import com.example.examplemod.item.ModItems;
import com.example.examplemod.kpk.KPKServerManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandKPKSet extends CommandBase {

    @Override
    public String getName() {
        return "kpk";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/kpk set <Фамилия> <Имя> <Позывной> <Дата_рождения> <Пол>\n" +
                "/kpk get <игрок/позывной>\n" +
                "/kpk check\n" +
                "/kpk listcontacts\n" +
                "/kpk give";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Эта команда доступна только игрокам!"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;

        if (args.length < 1) {
            throw new CommandException(getUsage(sender));
        }

        switch (args[0].toLowerCase()) {
            case "set":
                handleSet(player, args);
                break;
            case "get":
                handleGet(player, server, args);
                break;
            case "check":
                handleCheck(player);
                break;
            case "listcontacts":
                handleListContacts(player);
                break;
            case "give":
                handleGive(player);
                break;
            default:
                throw new CommandException("Неизвестная подкоманда. " + getUsage(sender));
        }
    }

    private void handleGive(EntityPlayer player) {
        ItemStack kpkItem = new ItemStack(ModItems.KPK_DEVICE);
        player.inventory.addItemStackToInventory(kpkItem);
        player.sendMessage(new TextComponentString(TextFormatting.GOLD + "Вы получили КПК устройство!"));
    }

    private void handleSet(EntityPlayer player, String[] args) throws CommandException {
        ItemStack heldStack = player.getHeldItemMainhand();
        if (!(heldStack.getItem() instanceof ItemKPK)) {
            throw new CommandException("Вы должны держать КПК в основной руке, чтобы настроить его.");
        }

        if (args.length != 6) {
            throw new CommandException("Использование: /kpk set <Фамилия> <Имя> <Позывной> <Дата_рождения> <Пол>");
        }

        String familiya = args[1];
        String name = args[2];
        String pozivnoy = args[3];
        String birthdateStr = args[4];
        String genderStr = args[5];

        if (KPKServerManager.findUserByCallsign(pozivnoy) != null) {
            throw new CommandException("Этот позывной уже занят.");
        }

        LocalDate birthdate;
        try {
            birthdate = LocalDate.parse(birthdateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException e) {
            throw new CommandException("Неверный формат даты! Используйте формат: дд.мм.гггг");
        }

        Gender gender = Gender.fromString(genderStr);
        if (gender == null) {
            throw new CommandException("Неверный пол! Используйте: м/ж");
        }

        User user = new User(familiya, name, pozivnoy, gender, birthdate);

        // 1. Записываем данные в предмет
        ItemKPK.setUserData(heldStack, user);
        ItemKPK.setContacts(heldStack, new ArrayList<>());

        // 2. Записываем данные в центральное серверное хранилище
        KPKServerManager.setUser(player.getUniqueID(), user);

        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Данные КПК успешно установлены и зарегистрированы на сервере!"));
    }

    private void handleGet(EntityPlayer sender, MinecraftServer server, String[] args) throws CommandException {
        if (args.length != 2) {
            throw new CommandException("Использование: /kpk get <игрок/позывной>");
        }

        String targetIdentifier = args[1];
        User targetUser = null;

        EntityPlayer targetPlayer = server.getPlayerList().getPlayerByUsername(targetIdentifier);
        if (targetPlayer != null) {
            targetUser = KPKServerManager.getUser(targetPlayer.getUniqueID());
        } else {
            Pair<UUID, User> foundUser = KPKServerManager.findUserByCallsign(targetIdentifier);
            if (foundUser != null) {
                targetUser = foundUser.getRight();
            }
        }

        if (targetUser == null) {
            throw new CommandException("Игрок или позывной '" + targetIdentifier + "' не найден в базе данных КПК.");
        }

        displayUserData(sender, targetUser.pozivnoy, targetUser);
    }

    private void handleCheck(EntityPlayer player) throws CommandException {
        User user = KPKServerManager.getUser(player.getUniqueID());
        if (user == null) {
            throw new CommandException("Ваши данные не найдены в базе. Используйте /kpk set.");
        }
        displayUserData(player, "Ваши", user);
    }

    private void handleListContacts(EntityPlayer player) throws CommandException {
        ItemStack heldStack = player.getHeldItemMainhand();
        if (!(heldStack.getItem() instanceof ItemKPK)) {
            throw new CommandException("Вы должны держать КПК в основной руке.");
        }

        List<String> contacts = ItemKPK.getContacts(heldStack);
        if (contacts.isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Список контактов этого КПК пуст."));
        } else {
            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Контакты в этом КПК:"));
            for (String contact : contacts) {
                player.sendMessage(new TextComponentString("- " + contact));
            }
        }
    }

    private void displayUserData(ICommandSender sender, String ownerName, User userData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "=== КПК данные (" + ownerName + ") ==="));
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Фамилия: " + TextFormatting.WHITE + userData.familiya));
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Имя: " + TextFormatting.WHITE + userData.name));
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Позывной: " + TextFormatting.WHITE + userData.pozivnoy));
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Дата рождения: " + TextFormatting.WHITE + userData.birthdate.format(formatter)));
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Пол: " + TextFormatting.WHITE + userData.gender.getDisplayName()));
    }
}