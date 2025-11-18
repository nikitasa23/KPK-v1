package com.example.examplemod.item;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.Gender;
import com.example.examplemod.User;
import com.example.examplemod.gui.GuiHandler;
import com.example.examplemod.gui.KPKModelInteractionGui;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ItemKPK extends Item {
    public static final String TAG_ENABLED = "KpkEnabled";
    public static final String TAG_MODEL_DISPLAY_PAGE = "KpkModelDisplayPage";
    public static final String TAG_CHAT_CREATION_MODE = "KpkChatCreationMode";
    public static final String TAG_CHAT_CREATION_TYPE = "KpkChatCreationType";
    public static final String TAG_CHAT_SELECTED_CONTACTS = "KpkChatSelectedContacts";
    public static final String TAG_CURRENT_CHAT_CHANNEL_ID = "KpkCurrentChatId";

    public static final String TAG_USER_DATA = "KpkUserData";
    public static final String TAG_USER_FAMILIYA = "familiya";
    public static final String TAG_USER_NAME = "name";
    public static final String TAG_USER_POZIVNOY = "pozivnoy";
    public static final String TAG_USER_GENDER = "gender";
    public static final String TAG_USER_BIRTHDATE = "birthdate";
    public static final String TAG_CONTACTS = "KpkContacts";

    public static final int PAGE_INFO = 0;
    public static final int PAGE_CHAT = 1;
    public static final int PAGE_CONTACTS = 2;
    public static final int TOTAL_MODEL_PAGES = 3;

    public static final int SUBPAGE_CHAT_COMMON = 0;
    public static final int SUBPAGE_CHAT_CREATE = 1;

    public static final int CHAT_TYPE_PM = 1;
    public static final int CHAT_TYPE_GROUP = 2;

    private static long lastToggleTime = 0;
    private static final long TOGGLE_COOLDOWN_MS = 500;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public ItemKPK() {
        setRegistryName(ExampleMod.MODID, "kpk_device");
        setUnlocalizedName(ExampleMod.MODID + ".kpk_device");
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.TOOLS);
        setMaxStackSize(1);
    }

    private static NBTTagCompound getTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    public static void setUserData(ItemStack stack, User user) {
        NBTTagCompound rootTag = getTag(stack);
        if (user == null) {
            rootTag.removeTag(TAG_USER_DATA);
            return;
        }
        NBTTagCompound userTag = new NBTTagCompound();
        userTag.setString(TAG_USER_FAMILIYA, user.familiya);
        userTag.setString(TAG_USER_NAME, user.name);
        userTag.setString(TAG_USER_POZIVNOY, user.pozivnoy);
        userTag.setString(TAG_USER_GENDER, user.gender.name());
        userTag.setString(TAG_USER_BIRTHDATE, user.birthdate.format(DATE_FORMATTER));
        rootTag.setTag(TAG_USER_DATA, userTag);
    }

    @Nullable
    public static User getUserData(ItemStack stack) {
        NBTTagCompound rootTag = getTag(stack);
        if (!rootTag.hasKey(TAG_USER_DATA, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        try {
            NBTTagCompound userTag = rootTag.getCompoundTag(TAG_USER_DATA);
            String familiya = userTag.getString(TAG_USER_FAMILIYA);
            String name = userTag.getString(TAG_USER_NAME);
            String pozivnoy = userTag.getString(TAG_USER_POZIVNOY);
            Gender gender = Gender.valueOf(userTag.getString(TAG_USER_GENDER));
            LocalDate birthdate = LocalDate.parse(userTag.getString(TAG_USER_BIRTHDATE), DATE_FORMATTER);
            return new User(familiya, name, pozivnoy, gender, birthdate);
        } catch (Exception e) {
            System.err.println("Failed to read User data from KPK ItemStack NBT");
            return null;
        }
    }

    public static List<String> getContacts(ItemStack stack) {
        List<String> contacts = new ArrayList<>();
        NBTTagList tagList = getTag(stack).getTagList(TAG_CONTACTS, Constants.NBT.TAG_STRING);
        for (int i = 0; i < tagList.tagCount(); i++) {
            contacts.add(tagList.getStringTagAt(i));
        }
        return contacts;
    }

    public static void setContacts(ItemStack stack, List<String> contacts) {
        NBTTagList tagList = new NBTTagList();
        for (String contact : contacts) {
            tagList.appendTag(new NBTTagString(contact));
        }
        getTag(stack).setTag(TAG_CONTACTS, tagList);
    }

    public static void addContact(ItemStack stack, String callsign) {
        List<String> contacts = getContacts(stack);
        if (callsign != null && !callsign.isEmpty() && !contacts.contains(callsign)) {
            contacts.add(callsign);
            setContacts(stack, contacts);
        }
    }

    public static void removeContact(ItemStack stack, String callsign) {
        List<String> contacts = getContacts(stack);
        if (contacts.remove(callsign)) {
            setContacts(stack, contacts);
        }
    }

    public static boolean isEnabled(ItemStack stack) {
        return getTag(stack).getBoolean(TAG_ENABLED);
    }

    public static void setEnabled(ItemStack stack, boolean value) {
        getTag(stack).setBoolean(TAG_ENABLED, value);
        if (!value) {
            setChatCreationMode(stack, false);
        }
    }

    public static int getCurrentModelPage(ItemStack stack) {
        NBTTagCompound tag = getTag(stack);
        if (tag.hasKey(TAG_MODEL_DISPLAY_PAGE)) {
            int page = tag.getInteger(TAG_MODEL_DISPLAY_PAGE);
            return (page >= 0 && page < TOTAL_MODEL_PAGES) ? page : PAGE_INFO;
        }
        return PAGE_INFO;
    }

    public static void setCurrentModelPage(ItemStack stack, int page) {
        getTag(stack).setInteger(TAG_MODEL_DISPLAY_PAGE, (page >= 0 && page < TOTAL_MODEL_PAGES) ? page : PAGE_INFO);
        if (page != PAGE_CHAT && page != PAGE_CONTACTS) {
            setChatCreationMode(stack, false);
        }
    }

    public static boolean isChatCreationMode(ItemStack stack) {
        return getTag(stack).getBoolean(TAG_CHAT_CREATION_MODE);
    }

    public static void setChatCreationMode(ItemStack stack, boolean mode) {
        NBTTagCompound tag = getTag(stack);
        tag.setBoolean(TAG_CHAT_CREATION_MODE, mode);
        if (!mode) {
            tag.removeTag(TAG_CHAT_CREATION_TYPE);
            tag.removeTag(TAG_CHAT_SELECTED_CONTACTS);
        }
    }

    public static int getChatCreationType(ItemStack stack) {
        return getTag(stack).getInteger(TAG_CHAT_CREATION_TYPE);
    }

    public static void setChatCreationType(ItemStack stack, int type) {
        getTag(stack).setInteger(TAG_CHAT_CREATION_TYPE, type);
    }

    public static List<String> getSelectedContactsForGroup(ItemStack stack) {
        List<String> list = new ArrayList<>();
        NBTTagList nbtList = getTag(stack).getTagList(TAG_CHAT_SELECTED_CONTACTS, Constants.NBT.TAG_STRING);
        for (int i = 0; i < nbtList.tagCount(); i++) {
            list.add(nbtList.getStringTagAt(i));
        }
        return list;
    }

    public static void addContactToSelection(ItemStack stack, String callsign) {
        NBTTagCompound tag = getTag(stack);
        NBTTagList nbtList = tag.getTagList(TAG_CHAT_SELECTED_CONTACTS, Constants.NBT.TAG_STRING);
        for (int i = 0; i < nbtList.tagCount(); i++) {
            if (nbtList.getStringTagAt(i).equals(callsign)) {
                return;
            }
        }
        nbtList.appendTag(new NBTTagString(callsign));
        tag.setTag(TAG_CHAT_SELECTED_CONTACTS, nbtList);
    }

    public static void removeContactFromSelection(ItemStack stack, String callsign) {
        NBTTagCompound tag = getTag(stack);
        NBTTagList nbtList = tag.getTagList(TAG_CHAT_SELECTED_CONTACTS, Constants.NBT.TAG_STRING);
        NBTTagList newList = new NBTTagList();
        for (int i = 0; i < nbtList.tagCount(); i++) {
            if (!nbtList.getStringTagAt(i).equals(callsign)) {
                newList.appendTag(new NBTTagString(nbtList.getStringTagAt(i)));
            }
        }
        tag.setTag(TAG_CHAT_SELECTED_CONTACTS, newList);
    }

    public static void clearSelectedContactsForGroup(ItemStack stack) {
        getTag(stack).removeTag(TAG_CHAT_SELECTED_CONTACTS);
    }

    public static String getCurrentChatChannelId(ItemStack stack) {
        NBTTagCompound tag = getTag(stack);
        if (tag.hasKey(TAG_CURRENT_CHAT_CHANNEL_ID)) {
            return tag.getString(TAG_CURRENT_CHAT_CHANNEL_ID);
        }
        return com.example.examplemod.chat.ChatChannel.COMMON_CHANNEL_ID_PREFIX;
    }

    public static void setCurrentChatChannelId(ItemStack stack, String channelId) {
        if (channelId != null) {
            getTag(stack).setString(TAG_CURRENT_CHAT_CHANNEL_ID, channelId);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (playerIn.isSneaking()) {
            long currentTime = System.currentTimeMillis();
            if (worldIn.isRemote && currentTime - lastToggleTime < TOGGLE_COOLDOWN_MS) {
                return new ActionResult<>(EnumActionResult.PASS, stack);
            }
            if (worldIn.isRemote) lastToggleTime = currentTime;

            boolean currentState = isEnabled(stack);
            setEnabled(stack, !currentState);

            if (!worldIn.isRemote) {
                TextComponentString message;
                if (!currentState) {
                    message = new TextComponentString(TextFormatting.GREEN + "КПК включён");
                    if (getUserData(stack) != null) {
                        setCurrentModelPage(stack, PAGE_INFO);
                    }
                } else {
                    message = new TextComponentString(TextFormatting.RED + "КПК выключен");
                }
                playerIn.sendMessage(message);
            }

            if (currentState && worldIn.isRemote) {
                playerIn.closeScreen();
            }

            return new ActionResult<>(EnumActionResult.SUCCESS, stack);

        } else {
            if (isEnabled(stack)) {
                if (worldIn.isRemote) {
                    if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof KPKModelInteractionGui) {
                        playerIn.closeScreen();
                    } else {
                        playerIn.openGui(ExampleMod.instance, GuiHandler.KPK_MODEL_INTERACTION_GUI_ID, worldIn, (int)playerIn.posX, (int)playerIn.posY, (int)playerIn.posZ);
                    }
                }
            } else {
                if (!worldIn.isRemote) {
                    TextComponentString disabledMessage = new TextComponentString(TextFormatting.YELLOW + "КПК выключен. Включите с помощью Shift + ПКМ.");
                    playerIn.sendMessage(disabledMessage);
                }
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Персональное КПК устройство");

        User user = getUserData(stack);
        if (user != null) {
            tooltip.add(TextFormatting.AQUA + "Позывной: " + TextFormatting.WHITE + user.pozivnoy);
        } else {
            tooltip.add(TextFormatting.RED + "Не настроен");
        }

        tooltip.add(TextFormatting.YELLOW + (isEnabled(stack) ? "Включено" : "Выключено"));
        tooltip.add(TextFormatting.DARK_GRAY + "Shift+ПКМ - вкл/выкл");
        if (isEnabled(stack)) {
            tooltip.add(TextFormatting.DARK_GRAY + "ПКМ - режим курсора");
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return false;
    }
}