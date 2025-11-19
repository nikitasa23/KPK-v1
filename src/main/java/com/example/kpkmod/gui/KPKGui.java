package com.example.kpkmod.gui;

import com.example.kpkmod.User;
import com.example.kpkmod.chat.ChatChannel;
import com.example.kpkmod.chat.ChatMessage;
import com.example.kpkmod.chat.ClientChatCache;
import com.example.kpkmod.item.ItemKPK;
import com.example.kpkmod.network.PacketChatMessageToServer;
import com.example.kpkmod.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class KPKGui extends GuiScreen {
    private User userData;
    private ItemStack kpkStack;
    private static final int GUI_VIRTUAL_WIDTH = 256;
    private static final int GUI_VIRTUAL_HEIGHT = 160;

    private int currentMainPage = ItemKPK.PAGE_INFO;

    private GuiButtonCustom infoButton;
    private GuiButtonCustom chatButton;

    private static final int SUBPAGE_CHAT_COMMON = 10;
    private static final int SUBPAGE_CHAT_CREATE = 11;
    private int currentChatSubPage = SUBPAGE_CHAT_COMMON;

    private GuiButtonCustom chatCommonButton;
    private GuiButtonCustom chatCreateButton;

    private GuiTextField chatInputField;
    private GuiButtonCustom sendChatButton;
    private GuiButtonCustom anonymousChatButton;
    private boolean isAnonymous = false;
    private List<String> formattedChatMessages = new ArrayList<>();
    private int chatScrollOffset = 0;
    private int maxVisibleChatLines = 0;

    private static final int CHAT_LINE_HEIGHT = 9;
    private static final int CHAT_INPUT_HEIGHT = 18;
    private static final float CHAT_TEXT_SCALE = 1.0f;

    private final int COLOR_BG = 0xEE1A1A1A;
    private final int COLOR_TITLE = 0xFFFFAA00;
    private final int COLOR_TEXT_LABEL = 0xFFAAAAAA;
    private final int COLOR_TEXT_VALUE = 0xFFE0E0E0;
    private final int COLOR_CHAT_BG = 0x99000000;
    private final int COLOR_SCROLLBAR = 0xFF888888;


    public KPKGui() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (!(heldItem.getItem() instanceof ItemKPK)) {
                heldItem = player.getHeldItemOffhand();
            }
            if (heldItem.getItem() instanceof ItemKPK) {
                this.kpkStack = heldItem;
                this.userData = ItemKPK.getUserData(this.kpkStack);
            } else {
                this.kpkStack = ItemStack.EMPTY;
                this.userData = null;
            }
        }
        ClientChatCache.addOnChatDataUpdatedListener(this::refreshChatMessages);
        refreshChatMessages();
    }

    private void scrollToBottom() {
        if (maxVisibleChatLines > 0 && formattedChatMessages.size() > maxVisibleChatLines) {
            this.chatScrollOffset = formattedChatMessages.size() - maxVisibleChatLines;
        } else {
            this.chatScrollOffset = 0;
        }
    }


    private void refreshChatMessages() {
        if (currentMainPage == ItemKPK.PAGE_CHAT && currentChatSubPage == SUBPAGE_CHAT_COMMON) {
            boolean wasScrolledToBottom = maxVisibleChatLines <= 0 || (chatScrollOffset >= formattedChatMessages.size() - maxVisibleChatLines -1);

            formattedChatMessages.clear();
            List<ChatMessage> messages = ClientChatCache.getChatHistory(ChatChannel.COMMON_CHANNEL_ID_PREFIX);

            int contentAreaX = 10 + 55 + 5;
            int chatDisplayWidth = GUI_VIRTUAL_WIDTH - contentAreaX - 10;
            int effectiveWrappingWidth = (int)(chatDisplayWidth / CHAT_TEXT_SCALE);

            for (ChatMessage msg : messages) {
                String prefix = TextFormatting.DARK_AQUA + "[" + msg.getFormattedTimestamp() + "] ";
                String senderFormatted = TextFormatting.AQUA + msg.senderPlayerName + TextFormatting.WHITE + ": ";
                String contentFormatted = msg.messageContent;

                int wrappingWidth = effectiveWrappingWidth - fontRenderer.getStringWidth(prefix) - 4;
                List<String> wrappedLines = fontRenderer.listFormattedStringToWidth(senderFormatted + contentFormatted, wrappingWidth);

                for(int i=0; i<wrappedLines.size(); ++i){
                    if(i==0){
                        formattedChatMessages.add(prefix + wrappedLines.get(i));
                    } else {
                        formattedChatMessages.add(fontRenderer.trimStringToWidth(" ", fontRenderer.getStringWidth(prefix)) + wrappedLines.get(i));
                    }
                }
            }

            if (wasScrolledToBottom) {
                scrollToBottom();
            } else {
                if (maxVisibleChatLines > 0 && chatScrollOffset > formattedChatMessages.size() - maxVisibleChatLines) {
                    chatScrollOffset = Math.max(0, formattedChatMessages.size() - maxVisibleChatLines);
                }
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);

        int buttonWidth = 55;
        int buttonHeight = 20;
        int buttonXOffset = 10;
        int buttonYStart = 15;
        int buttonSpacing = 5;

        infoButton = new GuiButtonCustom(ItemKPK.PAGE_INFO, buttonXOffset, buttonYStart, buttonWidth, buttonHeight, "ИНФРА");
        chatButton = new GuiButtonCustom(ItemKPK.PAGE_CHAT, buttonXOffset, buttonYStart + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight, "ЧАТ");

        this.buttonList.add(infoButton);
        this.buttonList.add(chatButton);

        int subMenuYStart = buttonYStart + (buttonHeight + buttonSpacing) * 2 + 15;
        chatCommonButton = new GuiButtonCustom(SUBPAGE_CHAT_COMMON, buttonXOffset, subMenuYStart, buttonWidth, buttonHeight, "Общий");
        chatCreateButton = new GuiButtonCustom(SUBPAGE_CHAT_CREATE, buttonXOffset, subMenuYStart + buttonHeight + buttonSpacing, buttonWidth, buttonHeight, "Создать");

        this.buttonList.add(chatCommonButton);
        this.buttonList.add(chatCreateButton);

        chatInputField = new GuiTextField(300, this.fontRenderer, 0, 0, 100, CHAT_INPUT_HEIGHT - 2);
        chatInputField.setMaxStringLength(256);
        chatInputField.setEnableBackgroundDrawing(false);

        sendChatButton = new GuiButtonCustom(301, 0, 0, 40, CHAT_INPUT_HEIGHT - 2, "Отпр.");
        this.buttonList.add(sendChatButton);

        anonymousChatButton = new GuiButtonCustom(302, 0, 0, 60, 12, "Анонимно");
        this.buttonList.add(anonymousChatButton);


        updateButtonStatesAndVisibility();
        refreshChatMessages();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        ClientChatCache.removeOnChatDataUpdatedListener(this::refreshChatMessages);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (chatInputField != null) {
            chatInputField.updateCursorCounter();
        }
    }

    private void updateButtonStatesAndVisibility() {
        infoButton.setActive(currentMainPage == ItemKPK.PAGE_INFO);
        chatButton.setActive(currentMainPage == ItemKPK.PAGE_CHAT);

        boolean chatPageActive = (currentMainPage == ItemKPK.PAGE_CHAT);
        chatCommonButton.visible = chatPageActive;
        chatCreateButton.visible = chatPageActive;

        boolean commonChatActive = chatPageActive && currentChatSubPage == SUBPAGE_CHAT_COMMON;
        chatInputField.setVisible(commonChatActive);
        sendChatButton.visible = commonChatActive;
        anonymousChatButton.visible = commonChatActive;

        if (chatPageActive) {
            chatCommonButton.setActive(currentChatSubPage == SUBPAGE_CHAT_COMMON);
            chatCreateButton.setActive(currentChatSubPage == SUBPAGE_CHAT_CREATE);
        }

        if (commonChatActive) {
            anonymousChatButton.setActive(isAnonymous);
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) throws IOException {
        if (guibutton.enabled || (guibutton instanceof GuiButtonCustom && ((GuiButtonCustom)guibutton).isActive())) {
            if (guibutton instanceof GuiButtonCustom && ((GuiButtonCustom)guibutton).isActive()){
                return;
            } else if (guibutton.id == ItemKPK.PAGE_INFO || guibutton.id == ItemKPK.PAGE_CHAT ) {
                if (this.currentMainPage != guibutton.id) {
                    this.currentMainPage = guibutton.id;
                    refreshChatMessages();
                    if (this.currentMainPage == ItemKPK.PAGE_CHAT) {
                        scrollToBottom();
                    } else {
                        this.chatScrollOffset = 0;
                    }
                }
            } else if (guibutton.id == SUBPAGE_CHAT_COMMON || guibutton.id == SUBPAGE_CHAT_CREATE) {
                if (this.currentMainPage == ItemKPK.PAGE_CHAT && this.currentChatSubPage != guibutton.id) {
                    this.currentChatSubPage = guibutton.id;
                    refreshChatMessages();
                    if (this.currentChatSubPage == SUBPAGE_CHAT_COMMON) {
                        scrollToBottom();
                    } else {
                        this.chatScrollOffset = 0;
                    }
                }
            } else if (guibutton == sendChatButton) {
                if (currentMainPage == ItemKPK.PAGE_CHAT && currentChatSubPage == SUBPAGE_CHAT_COMMON &&
                        chatInputField != null && !chatInputField.getText().trim().isEmpty()) {
                    PacketHandler.INSTANCE.sendToServer(new PacketChatMessageToServer(ChatChannel.COMMON_CHANNEL_ID_PREFIX, chatInputField.getText().trim(), isAnonymous));
                    chatInputField.setText("");
                }
            } else if (guibutton == anonymousChatButton) {
                if(currentMainPage == ItemKPK.PAGE_CHAT && currentChatSubPage == SUBPAGE_CHAT_COMMON) {
                    isAnonymous = !isAnonymous;
                }
            }
            updateButtonStatesAndVisibility();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (chatInputField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                chatInputField.setFocused(false);
                return;
            }
            chatInputField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == Keyboard.KEY_RETURN) {
                actionPerformed(sendChatButton);
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        float scale = Math.min((float)this.width / GUI_VIRTUAL_WIDTH, (float)this.height / GUI_VIRTUAL_HEIGHT);
        int guiLeft = (this.width - (int)(GUI_VIRTUAL_WIDTH * scale)) / 2;
        int guiTop = (this.height - (int)(GUI_VIRTUAL_HEIGHT * scale)) / 2;
        int scaledMouseX = (int)((mouseX - guiLeft) / scale);
        int scaledMouseY = (int)((mouseY - guiTop) / scale);

        if (chatInputField.getVisible()) {
            chatInputField.mouseClicked(scaledMouseX, scaledMouseY, mouseButton);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && currentMainPage == ItemKPK.PAGE_CHAT && currentChatSubPage == SUBPAGE_CHAT_COMMON) {
            if (dWheel > 0) {
                chatScrollOffset = Math.max(0, chatScrollOffset - 3);
            } else {
                if (maxVisibleChatLines < formattedChatMessages.size()) {
                    chatScrollOffset = Math.min(formattedChatMessages.size() - maxVisibleChatLines, chatScrollOffset + 3);
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (currentMainPage == ItemKPK.PAGE_CHAT && currentChatSubPage == SUBPAGE_CHAT_COMMON) {
            refreshChatMessages();
        }

        GlStateManager.pushMatrix();

        float scale = Math.min((float)this.width / GUI_VIRTUAL_WIDTH, (float)this.height / GUI_VIRTUAL_HEIGHT);
        int guiLeft = (this.width - (int)(GUI_VIRTUAL_WIDTH * scale)) / 2;
        int guiTop = (this.height - (int)(GUI_VIRTUAL_HEIGHT * scale)) / 2;

        GlStateManager.translate(guiLeft, guiTop, 0);
        GlStateManager.scale(scale, scale, 1.0F);

        Gui.drawRect(0, 0, GUI_VIRTUAL_WIDTH, GUI_VIRTUAL_HEIGHT, COLOR_BG);

        int contentAreaX = 10 + 55 + 5;
        int contentAreaY = 15;
        int lineHeight = fontRenderer.FONT_HEIGHT + 2;

        if (userData != null || currentMainPage == ItemKPK.PAGE_CHAT) {
            String pageTitle = "";
            if (currentMainPage == ItemKPK.PAGE_INFO) pageTitle = "Персональные данные";
            else if (currentMainPage == ItemKPK.PAGE_CHAT) pageTitle = "Канал связи";

            this.fontRenderer.drawString(TextFormatting.GOLD + pageTitle, contentAreaX, contentAreaY, COLOR_TITLE);

            int mainContentY = contentAreaY + lineHeight + 4;

            if (currentMainPage == ItemKPK.PAGE_INFO) {
                if (userData != null) {
                    drawString(this.fontRenderer, "Владелец:", contentAreaX, mainContentY, COLOR_TEXT_LABEL);
                    drawString(this.fontRenderer, TextFormatting.WHITE + userData.familiya + " " + userData.name, contentAreaX + 5, mainContentY + lineHeight, COLOR_TEXT_VALUE);

                    drawString(this.fontRenderer, "Позывной:", contentAreaX, mainContentY + lineHeight * 2, COLOR_TEXT_LABEL);
                    drawString(this.fontRenderer, TextFormatting.WHITE + userData.pozivnoy, contentAreaX + 5, mainContentY + lineHeight * 3, COLOR_TEXT_VALUE);

                    drawString(this.fontRenderer, "Дата рождения:", contentAreaX, mainContentY + lineHeight * 4, COLOR_TEXT_LABEL);
                    drawString(this.fontRenderer, TextFormatting.WHITE + userData.birthdate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), contentAreaX + 5, mainContentY + lineHeight * 5, COLOR_TEXT_VALUE);

                    drawString(this.fontRenderer, "Пол:", contentAreaX, mainContentY + lineHeight * 6, COLOR_TEXT_LABEL);
                    drawString(this.fontRenderer, TextFormatting.WHITE + userData.gender.getDisplayName(), contentAreaX + 5, mainContentY + lineHeight * 7, COLOR_TEXT_VALUE);
                } else {
                    int textWidth = this.fontRenderer.getStringWidth("КПК НЕ ИНИЦИАЛИЗИРОВАН");
                    drawString(this.fontRenderer, "КПК НЕ ИНИЦИАЛИЗИРОВАН", contentAreaX + (GUI_VIRTUAL_WIDTH - contentAreaX - 10 - textWidth)/2 , GUI_VIRTUAL_HEIGHT / 2 - 20, 0xFF5555);
                }

            } else if (currentMainPage == ItemKPK.PAGE_CHAT) {
                if (currentChatSubPage == SUBPAGE_CHAT_COMMON) {
                    int chatDisplayWidth = GUI_VIRTUAL_WIDTH - contentAreaX - 10;
                    int chatDisplayHeight = GUI_VIRTUAL_HEIGHT - mainContentY - 10 - CHAT_INPUT_HEIGHT - 3 - (anonymousChatButton.height + 2);
                    maxVisibleChatLines = (int)(chatDisplayHeight / (CHAT_LINE_HEIGHT * CHAT_TEXT_SCALE));

                    Gui.drawRect(contentAreaX, mainContentY, contentAreaX + chatDisplayWidth, mainContentY + chatDisplayHeight, COLOR_CHAT_BG);

                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    int scissorX = guiLeft + (int)(contentAreaX * scale);
                    int scissorY = mc.displayHeight - (guiTop + (int)((mainContentY + chatDisplayHeight) * scale));
                    int scissorWidth = (int)(chatDisplayWidth * scale);
                    int scissorHeight = (int)(chatDisplayHeight * scale);
                    GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);

                    for (int i = 0; i < maxVisibleChatLines; i++) {
                        int messageIndex = i + chatScrollOffset;
                        if (messageIndex >= 0 && messageIndex < formattedChatMessages.size()) {
                            String msgLine = formattedChatMessages.get(messageIndex);
                            GlStateManager.pushMatrix();
                            float xPos = contentAreaX + 2;
                            float yPos = mainContentY + 2 + (i * CHAT_LINE_HEIGHT * CHAT_TEXT_SCALE);
                            GlStateManager.translate(xPos, yPos, 0f);
                            GlStateManager.scale(CHAT_TEXT_SCALE, CHAT_TEXT_SCALE, CHAT_TEXT_SCALE);
                            fontRenderer.drawStringWithShadow(msgLine, 0, 0, 0xFFFFFF);
                            GlStateManager.popMatrix();
                        }
                    }
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);

                    if (formattedChatMessages.size() > maxVisibleChatLines) {
                        int scrollBarTotalHeight = chatDisplayHeight - 2;
                        int scrollBarActualHeight = Math.max(10, (int) ((float) maxVisibleChatLines / formattedChatMessages.size() * scrollBarTotalHeight));
                        int scrollBarYOffset = 0;
                        if (formattedChatMessages.size() - maxVisibleChatLines > 0) {
                            scrollBarYOffset = (int) ((float) chatScrollOffset / (formattedChatMessages.size() - maxVisibleChatLines) * (scrollBarTotalHeight - scrollBarActualHeight));
                        }
                        Gui.drawRect(contentAreaX + chatDisplayWidth - 4, mainContentY + 1 + scrollBarYOffset,
                                contentAreaX + chatDisplayWidth - 1, mainContentY + 1 + scrollBarYOffset + scrollBarActualHeight,
                                COLOR_SCROLLBAR);
                    }

                    int inputBaseY = mainContentY + chatDisplayHeight + 3;

                    anonymousChatButton.x = contentAreaX;
                    anonymousChatButton.y = inputBaseY;

                    int inputAreaY = inputBaseY + anonymousChatButton.height + 2;

                    chatInputField.x = contentAreaX;
                    chatInputField.y = inputAreaY;
                    chatInputField.width = chatDisplayWidth - sendChatButton.width - 2;
                    chatInputField.height = CHAT_INPUT_HEIGHT - 2;
                    if(chatInputField.getVisible()) {
                        drawRect(chatInputField.x-1, chatInputField.y-1, chatInputField.x+chatInputField.width+1, chatInputField.y+chatInputField.height+1, 0xFF000000);
                        drawRect(chatInputField.x, chatInputField.y, chatInputField.x+chatInputField.width, chatInputField.y+chatInputField.height, 0xFF333333);
                        chatInputField.drawTextBox();
                    }

                    sendChatButton.x = contentAreaX + chatDisplayWidth - sendChatButton.width;
                    sendChatButton.y = inputAreaY;

                } else if (currentChatSubPage == SUBPAGE_CHAT_CREATE) {
                    drawString(this.fontRenderer, "Создание нового чата...", contentAreaX, mainContentY, 0xFFFF55);
                }
            }
        } else {
            int textWidth = this.fontRenderer.getStringWidth("КПК НЕ ИНИЦИАЛИЗИРОВАН");
            drawString(this.fontRenderer, "КПК НЕ ИНИЦИАЛИЗИРОВАН", (GUI_VIRTUAL_WIDTH - textWidth) / 2, GUI_VIRTUAL_HEIGHT / 2 - 20, 0xFF5555);
            textWidth = this.fontRenderer.getStringWidth("Используйте команду /kpk set");
            drawString(this.fontRenderer, "Используйте команду /kpk set", (GUI_VIRTUAL_WIDTH - textWidth) / 2, GUI_VIRTUAL_HEIGHT / 2, 0xFFFF55);
        }

        for (GuiButton guibutton : this.buttonList) {
            guibutton.drawButton(this.mc, mouseX, mouseY, partialTicks);
        }

        GlStateManager.popMatrix();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class GuiButtonCustom extends GuiButton {
        private boolean isActive = false;

        public GuiButtonCustom(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
        }

        public void setActive(boolean active) {
            this.isActive = active;
        }

        public boolean isActive() { return this.isActive; }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                FontRenderer fontrenderer = mc.fontRenderer;

                float guiScaleFactor = Math.min((float)mc.currentScreen.width / GUI_VIRTUAL_WIDTH, (float)mc.currentScreen.height / GUI_VIRTUAL_HEIGHT);
                int guiLeft = (mc.currentScreen.width - (int)(GUI_VIRTUAL_WIDTH * guiScaleFactor)) / 2;
                int guiTop = (mc.currentScreen.height - (int)(GUI_VIRTUAL_HEIGHT * guiScaleFactor)) / 2;

                int scaledMouseX = (int)((mouseX - guiLeft) / guiScaleFactor);
                int scaledMouseY = (int)((mouseY - guiTop) / guiScaleFactor);

                this.hovered = scaledMouseX >= this.x && scaledMouseY >= this.y && scaledMouseX < this.x + this.width && scaledMouseY < this.y + this.height;

                int bgColor;
                int borderColor;

                if (isActive) {
                    bgColor = 0xFF101010;
                    borderColor = 0xFFFFAA00;
                } else if (this.hovered && this.enabled) {
                    bgColor = 0xFF3c3c3c;
                    borderColor = 0xFF666666;
                } else {
                    bgColor = 0xFF2b2b2b;
                    borderColor = 0xFF444444;
                }

                drawRect(this.x, this.y, this.x + this.width, this.y + this.height, borderColor);
                drawRect(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, bgColor);

                int textColor;
                if (!this.enabled) {
                    textColor = 0xA0A0A0;
                } else if (this.isActive) {
                    textColor = 0xFFFFAA00;
                } else if (this.hovered) {
                    textColor = 0xFFFFFFA0;
                } else {
                    textColor = 0xE0E0E0;
                }
                this.drawCenteredString(fontrenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
            }
        }
    }
}