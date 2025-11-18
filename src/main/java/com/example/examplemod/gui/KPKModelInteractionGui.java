package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.ItemKPK;
import com.example.examplemod.item.ItemKPKRenderer;
import com.example.examplemod.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class KPKModelInteractionGui extends GuiScreen {

    private ItemStack kpkStack;

    private boolean isAddingContact = false;

    private String currentContactInput = "";
    private boolean contactInputActive = false;
    private String currentChatInput = "";
    private boolean chatInputActive = false;
    private boolean isAnonymous = false;
    private int cursorCounter = 0;
    public int chatScrollOffset = 0;
    public int channelScrollOffset = 0;

    // Registration form state
    private String regFam = "";
    private String regName = "";
    private String regPoz = "";
    private String regGender = "";
    private String regBirthdate = "";
    private int regActiveField = -1; // -1 none, 0..4 fields

    public KPKModelInteractionGui() {
        super();
        if (Minecraft.getMinecraft().player != null) {
            ItemStack mainHandStack = Minecraft.getMinecraft().player.getHeldItemMainhand();
            ItemStack offHandStack = Minecraft.getMinecraft().player.getHeldItemOffhand();

            if (mainHandStack.getItem() instanceof ItemKPK) {
                this.kpkStack = mainHandStack;
            } else if (offHandStack.getItem() instanceof ItemKPK) {
                this.kpkStack = offHandStack;
            }
        }
    }

    public boolean isAddingContact() {
        return this.isAddingContact;
    }

    public ItemStack getKpkStack() {
        return kpkStack;
    }

    public String getCurrentContactInput() {
        return currentContactInput;
    }

    public boolean isContactInputActive() {
        return contactInputActive;
    }

    public String getCurrentChatInput() {
        return currentChatInput;
    }

    public boolean isChatInputActive() {
        return chatInputActive;
    }

    public boolean isAnonymous() {
        return this.isAnonymous;
    }

    public String getRegFam() { return regFam; }
    public String getRegName() { return regName; }
    public String getRegPoz() { return regPoz; }
    public String getRegGender() { return regGender; }
    public String getRegBirthdate() { return regBirthdate; }

    public int getCursorCounter() {
        return cursorCounter;
    }

    public int getChatScrollOffset() {
        return this.chatScrollOffset;
    }

    public void setChatScrollOffset(int offset) {
        this.chatScrollOffset = offset;
    }

    private void resetChatCreationState() {
        if (kpkStack != null) {
            ItemKPK.setChatCreationMode(kpkStack, false);
            ItemKPK.setChatCreationType(kpkStack, 0);
            ItemKPK.clearSelectedContactsForGroup(kpkStack);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.mc != null && this.mc.mouseHelper != null) {
            this.mc.mouseHelper.ungrabMouseCursor();
        }
        Keyboard.enableRepeatEvents(true);

        if (kpkStack != null) {
            if (ItemKPK.getCurrentModelPage(kpkStack) != ItemKPK.PAGE_CONTACTS) {
                this.isAddingContact = false;
            }
            if (!this.isAddingContact) {
                contactInputActive = false;
                currentContactInput = "";
            }
            if (ItemKPK.getCurrentModelPage(kpkStack) != ItemKPK.PAGE_CHAT) {
                chatInputActive = false;
                currentChatInput = "";
                resetChatCreationState();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        if (this.mc != null && this.mc.mouseHelper != null && this.mc.currentScreen == null) {
            this.mc.mouseHelper.grabMouseCursor();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        cursorCounter++;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawGradientRect(0, 0, this.width, this.height, 0x01000000, 0x01000000);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && kpkStack != null && ItemKPK.getCurrentModelPage(kpkStack) == ItemKPK.PAGE_CHAT) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            Rectangle channelArea = ItemKPKRenderer.modelChannelListAreaRectOnScreen;
            if (channelArea != null && channelArea.contains(mouseX, mouseY)) {
                if (dWheel > 0) this.channelScrollOffset--;
                else this.channelScrollOffset++;
            } else {
                if (dWheel > 0) this.chatScrollOffset--;
                else this.chatScrollOffset++;
            }
        }
    }

    private void sendChatMessage() {
        String messageContent = currentChatInput.trim();
        if (!messageContent.isEmpty()) {
            PacketHandler.INSTANCE.sendToServer(new PacketChatMessageToServer(ItemKPK.getCurrentChatChannelId(kpkStack), messageContent, isAnonymous));
        }
        currentChatInput = "";
        chatInputActive = true;
    }

    private boolean checkRect(Rectangle rect, int mouseX, int mouseY) {
        return rect != null && rect.contains(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.kpkStack == null || !(this.kpkStack.getItem() instanceof ItemKPK) || !ItemKPK.isEnabled(this.kpkStack)) {
            if (this.mc != null) this.mc.displayGuiScreen(null);
            return;
        }

        if (mouseButton == 1) { // ПКМ для возврата/закрытия
            if (this.isAddingContact) {
                this.isAddingContact = false;
                contactInputActive = false;
                currentContactInput = "";
            } else if (ItemKPK.isChatCreationMode(kpkStack)) {
                resetChatCreationState();
            } else {
                mc.displayGuiScreen(null);
            }
            return;
        }

        if (mouseButton != 0) return;

        contactInputActive = false;
        chatInputActive = false;

        // Registration flow: if user data is missing, handle submit button
        if (ItemKPK.getUserData(kpkStack) == null) {
            if (ItemKPKRenderer.modelRegSurnameRectOnScreen != null && ItemKPKRenderer.modelRegSurnameRectOnScreen.contains(mouseX, mouseY)) { regActiveField = 0; return; }
            if (ItemKPKRenderer.modelRegNameRectOnScreen != null && ItemKPKRenderer.modelRegNameRectOnScreen.contains(mouseX, mouseY)) { regActiveField = 1; return; }
            if (ItemKPKRenderer.modelRegCallsignRectOnScreen != null && ItemKPKRenderer.modelRegCallsignRectOnScreen.contains(mouseX, mouseY)) { regActiveField = 2; return; }
            if (ItemKPKRenderer.modelRegGenderRectOnScreen != null && ItemKPKRenderer.modelRegGenderRectOnScreen.contains(mouseX, mouseY)) { regActiveField = 3; return; }
            if (ItemKPKRenderer.modelRegBirthdateRectOnScreen != null && ItemKPKRenderer.modelRegBirthdateRectOnScreen.contains(mouseX, mouseY)) { regActiveField = 4; return; }
            if (ItemKPKRenderer.modelRegSubmitRectOnScreen != null && ItemKPKRenderer.modelRegSubmitRectOnScreen.contains(mouseX, mouseY)) {
                PacketHandler.INSTANCE.sendToServer(new PacketRegisterKpkUser(regFam, regName, regPoz, regGender, regBirthdate));
                return;
            }
        }

        if (checkRect(ItemKPKRenderer.modelInfoButtonRectOnScreen, mouseX, mouseY)) {
            ItemKPK.setCurrentModelPage(kpkStack, ItemKPK.PAGE_INFO);
            resetChatCreationState();
            this.isAddingContact = false;
            return;
        }
        if (checkRect(ItemKPKRenderer.modelChatButtonRectOnScreen, mouseX, mouseY)) {
            ItemKPK.setCurrentModelPage(kpkStack, ItemKPK.PAGE_CHAT);
            this.isAddingContact = false;
            return;
        }
        if (checkRect(ItemKPKRenderer.modelContactsButtonRectOnScreen, mouseX, mouseY)) {
            ItemKPK.setCurrentModelPage(kpkStack, ItemKPK.PAGE_CONTACTS);
            resetChatCreationState();
            return;
        }

        int currentPage = ItemKPK.getCurrentModelPage(kpkStack);

        if (currentPage == ItemKPK.PAGE_CHAT) {
            handleChatPageClick(mouseX, mouseY);
        } else if (currentPage == ItemKPK.PAGE_CONTACTS) {
            handleContactsPageClick(mouseX, mouseY);
        }
    }

    private void handleChatPageClick(int mouseX, int mouseY) {
        if (ItemKPK.isChatCreationMode(kpkStack)) {
            if (checkRect(ItemKPKRenderer.modelChatCreatePmButtonRectOnScreen, mouseX, mouseY)) {
                ItemKPK.setChatCreationType(kpkStack, ItemKPK.CHAT_TYPE_PM);
                ItemKPK.setCurrentModelPage(kpkStack, ItemKPK.PAGE_CONTACTS);
                return;
            }
            if (checkRect(ItemKPKRenderer.modelChatCreateGroupButtonRectOnScreen, mouseX, mouseY)) {
                ItemKPK.setChatCreationType(kpkStack, ItemKPK.CHAT_TYPE_GROUP);
                ItemKPK.setCurrentModelPage(kpkStack, ItemKPK.PAGE_CONTACTS);
                return;
            }
        } else {
            if (checkRect(ItemKPKRenderer.modelChatInputFieldRectOnScreen, mouseX, mouseY)) {
                chatInputActive = true;
                return;
            }
            if (checkRect(ItemKPKRenderer.modelChatSendButtonRectOnScreen, mouseX, mouseY)) {
                sendChatMessage();
                return;
            }
            if (checkRect(ItemKPKRenderer.modelChatCreateButtonRectOnScreen, mouseX, mouseY)) {
                ItemKPK.setChatCreationMode(kpkStack, true);
                return;
            }
            if (checkRect(ItemKPKRenderer.modelChatAnonymousButtonRectOnScreen, mouseX, mouseY)) {
                isAnonymous = !isAnonymous;
                return;
            }

            // ИСПРАВЛЕНИЕ: Сначала проверяем нажатие на маленькие кнопки (удалить), потом на большие (выбрать канал)
            for (int i = 0; i < ItemKPKRenderer.modelChannelDeleteButtonRectsOnScreen.size(); i++) {
                if (checkRect(ItemKPKRenderer.modelChannelDeleteButtonRectsOnScreen.get(i), mouseX, mouseY)) {
                    String channelId = ItemKPKRenderer.modelChannelDeleteButtonAssociatedId.get(i);
                    PacketHandler.INSTANCE.sendToServer(new PacketRequestDeleteChannel(channelId));
                    return;
                }
            }

            for (int i = 0; i < ItemKPKRenderer.modelChannelListButtonRectsOnScreen.size(); i++) {
                if (checkRect(ItemKPKRenderer.modelChannelListButtonRectsOnScreen.get(i), mouseX, mouseY)) {
                    String channelId = ItemKPKRenderer.modelChannelListButtonAssociatedId.get(i);
                    ItemKPK.setCurrentChatChannelId(kpkStack, channelId);
                    if (!channelId.equals(com.example.examplemod.chat.ChatChannel.COMMON_CHANNEL_ID_PREFIX)) {
                        isAnonymous = false;
                    }
                    this.chatScrollOffset = 0;
                    return;
                }
            }

            for (int i = 0; i < ItemKPKRenderer.modelMemberRemoveButtonRectsOnScreen.size(); i++) {
                if (checkRect(ItemKPKRenderer.modelMemberRemoveButtonRectsOnScreen.get(i), mouseX, mouseY)) {
                    UUID memberId = ItemKPKRenderer.modelMemberRemoveButtonAssociatedId.get(i);
                    String channelId = ItemKPK.getCurrentChatChannelId(kpkStack);
                    PacketHandler.INSTANCE.sendToServer(new PacketRequestRemoveMember(channelId, memberId));
                    return;
                }
            }
        }
    }

    private void handleContactsPageClick(int mouseX, int mouseY) {
        if (ItemKPK.isChatCreationMode(kpkStack)) {
            int creationType = ItemKPK.getChatCreationType(kpkStack);

            for (int i = 0; i < ItemKPKRenderer.modelContactDeleteButtonRectsOnScreen.size(); i++) {
                if (checkRect(ItemKPKRenderer.modelContactDeleteButtonRectsOnScreen.get(i), mouseX, mouseY)) {
                    String contactCallsign = ItemKPKRenderer.modelContactDeleteButtonAssociatedName.get(i);

                    if (creationType == ItemKPK.CHAT_TYPE_PM) {
                        PacketHandler.INSTANCE.sendToServer(new PacketRequestCreatePMChannel(contactCallsign));
                        resetChatCreationState();
                        ItemKPK.setCurrentModelPage(kpkStack, ItemKPK.PAGE_CHAT);
                        return;
                    } else if (creationType == ItemKPK.CHAT_TYPE_GROUP) {
                        List<String> selected = ItemKPK.getSelectedContactsForGroup(kpkStack);
                        if (selected.contains(contactCallsign)) {
                            ItemKPK.removeContactFromSelection(kpkStack, contactCallsign);
                        } else if (selected.size() < 2) {
                            ItemKPK.addContactToSelection(kpkStack, contactCallsign);
                        }
                        return;
                    }
                }
            }

            if (creationType == ItemKPK.CHAT_TYPE_GROUP && checkRect(ItemKPKRenderer.modelContactsConfirmAddRectOnScreen, mouseX, mouseY)) {
                List<String> selectedContacts = ItemKPK.getSelectedContactsForGroup(kpkStack);
                if (!selectedContacts.isEmpty()) {
                    mc.displayGuiScreen(new GuiTextInput(this, "Введите название канала (2-20 симв.)", (channelName) -> {
                        if (channelName != null && !channelName.isEmpty()) {
                            PacketHandler.INSTANCE.sendToServer(new PacketRequestCreateGroupChannel(channelName, selectedContacts));
                            mc.addScheduledTask(() -> {
                                resetChatCreationState();
                                ItemKPK.setCurrentModelPage(kpkStack, ItemKPK.PAGE_CHAT);
                            });
                        }
                        mc.displayGuiScreen(this);
                    }));
                }
                return;
            }

        } else {
            if (this.isAddingContact) {
                if (checkRect(ItemKPKRenderer.modelContactsInputFieldRectOnScreen, mouseX, mouseY)) {
                    contactInputActive = true;
                    return;
                }
                if (checkRect(ItemKPKRenderer.modelContactsConfirmAddRectOnScreen, mouseX, mouseY)) {
                    if (!currentContactInput.trim().isEmpty()) {
                        PacketHandler.INSTANCE.sendToServer(new PacketAddContactRequest(currentContactInput.trim()));
                    }
                    currentContactInput = "";
                    contactInputActive = true;
                    return;
                }
            } else {
                if (checkRect(ItemKPKRenderer.modelContactsAddButtonRectOnScreen, mouseX, mouseY)) {
                    this.isAddingContact = true;
                    contactInputActive = true;
                    return;
                }
                for (int i = 0; i < ItemKPKRenderer.modelContactDeleteButtonRectsOnScreen.size(); i++) {
                    if (checkRect(ItemKPKRenderer.modelContactDeleteButtonRectsOnScreen.get(i), mouseX, mouseY)) {
                        String contactNameToDelete = ItemKPKRenderer.modelContactDeleteButtonAssociatedName.get(i);
                        PacketHandler.INSTANCE.sendToServer(new PacketRemoveContactRequest(contactNameToDelete));
                        return;
                    }
                }
            }
        }
    }


    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (kpkStack == null) {
            super.keyTyped(typedChar, keyCode);
            return;
        }

        if (ItemKPK.getUserData(kpkStack) == null && regActiveField >= 0) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) { regActiveField = -1; return; }
            if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN) { regActiveField = -1; return; }
            if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_BACK)) {
                backspaceActiveField();
                return;
            }
            if (net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                appendToActiveField(typedChar);
            }
            return;
        }

        if (chatInputActive) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                chatInputActive = false;
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                sendChatMessage();
                return;
            }
            if (isValidChar(typedChar, keyCode)) {
                currentChatInput = typeIn(currentChatInput, typedChar, keyCode, 256);
            }
            return;
        }

        if (contactInputActive) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.isAddingContact = false;
                contactInputActive = false;
                currentContactInput = "";
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                if (!currentContactInput.trim().isEmpty()) {
                    PacketHandler.INSTANCE.sendToServer(new PacketAddContactRequest(currentContactInput.trim()));
                }
                currentContactInput = "";
                return;
            }
            if (isValidChar(typedChar, keyCode)) {
                currentContactInput = typeIn(currentContactInput, typedChar, keyCode, 32);
            }
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            if (ItemKPK.isChatCreationMode(kpkStack)) {
                resetChatCreationState();
            } else if (this.isAddingContact) {
                this.isAddingContact = false;
                currentContactInput = "";
            } else {
                mc.displayGuiScreen(null);
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void backspaceActiveField() {
        switch (regActiveField) {
            case 0: if (!regFam.isEmpty()) regFam = regFam.substring(0, regFam.length()-1); break;
            case 1: if (!regName.isEmpty()) regName = regName.substring(0, regName.length()-1); break;
            case 2: if (!regPoz.isEmpty()) regPoz = regPoz.substring(0, regPoz.length()-1); break;
            case 3: if (!regGender.isEmpty()) regGender = regGender.substring(0, regGender.length()-1); break;
            case 4: if (!regBirthdate.isEmpty()) regBirthdate = regBirthdate.substring(0, regBirthdate.length()-1); break;
        }
    }

    private void appendToActiveField(char c) {
        switch (regActiveField) {
            case 0: if (regFam.length() < 32) regFam += c; break;
            case 1: if (regName.length() < 32) regName += c; break;
            case 2: if (regPoz.length() < 20) regPoz += c; break;
            case 3: if (regGender.length() < 12) regGender += Character.toUpperCase(c); break;
            case 4: if (regBirthdate.length() < 10) regBirthdate += c; break;
        }
    }


    private boolean isValidChar(char typedChar, int keyCode) {
        return net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(typedChar) || keyCode == Keyboard.KEY_BACK;
    }

    private String typeIn(String original, char typedChar, int keyCode, int maxLength) {
        if (keyCode == Keyboard.KEY_BACK) {
            if (!original.isEmpty()) {
                return original.substring(0, original.length() - 1);
            }
        } else {
            if (net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(typedChar) && original.length() < maxLength) {
                return original + typedChar;
            }
        }
        return original;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}