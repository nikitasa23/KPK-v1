package com.example.examplemod.gui;

import com.example.examplemod.item.ItemKPK;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiSelectPlayerForChat extends GuiScreen {
    protected GuiScreen parentScreen;
    protected String title;
    protected Consumer<List<String>> onConfirm;
    protected int maxSelections;

    private List<String> availableContacts;
    private List<String> selectedContacts;

    private GuiButton doneButton;
    private GuiButton cancelButton;

    private int listTop, listBottom, slotHeight;
    private int scrollOffset = 0;
    private int maxVisibleSlots = 0;

    private final int COLOR_BACKGROUND = 0xFF1E1E1E;
    private final int COLOR_LIST_BG = 0x99000000;
    private final int COLOR_LIST_BORDER = 0xFF000000;
    private final int COLOR_SCROLLBAR = 0xFF888888;
    private final int COLOR_SELECTED_ITEM = 0x80FFAA00;
    private final int COLOR_TITLE = 0xFFFFAA00;
    private final int COLOR_TEXT_NORMAL = 0xFFD0D0D0;
    private final int COLOR_TEXT_SELECTED = 0xFFFFFFFF;
    private final int COLOR_TEXT_HINT = 0xFF888888;

    public GuiSelectPlayerForChat(GuiScreen parent, String title, Consumer<List<String>> onConfirm, int maxSelections) {
        this.parentScreen = parent;
        this.title = title;
        this.onConfirm = onConfirm;
        this.maxSelections = maxSelections;
        this.selectedContacts = new ArrayList<>();

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null) {
            ItemStack kpkStack = player.getHeldItemMainhand();
            if (!(kpkStack.getItem() instanceof ItemKPK)) {
                kpkStack = player.getHeldItemOffhand();
            }

            if (kpkStack.getItem() instanceof ItemKPK) {
                this.availableContacts = ItemKPK.getContacts(kpkStack);
            } else {
                this.availableContacts = new ArrayList<>();
            }
        } else {
            this.availableContacts = new ArrayList<>();
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.doneButton = this.addButton(new GuiButton(0, this.width / 2 - 154, this.height - 38, 150, 20, "Готово"));
        this.cancelButton = this.addButton(new GuiButton(1, this.width / 2 + 4, this.height - 38, 150, 20, "Отмена"));

        this.listTop = 32;
        this.listBottom = this.height - 64;
        this.slotHeight = fontRenderer.FONT_HEIGHT + 6;
        this.maxVisibleSlots = (this.listBottom - this.listTop) / this.slotHeight;

        updateDoneButtonState();
    }

    private void updateDoneButtonState() {
        this.doneButton.enabled = !this.selectedContacts.isEmpty();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) {
            if (button.id == 0) {
                this.onConfirm.accept(new ArrayList<>(this.selectedContacts));
                this.mc.displayGuiScreen(this.parentScreen);
            } else if (button.id == 1) {
                this.mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && availableContacts.size() > maxVisibleSlots) {
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(availableContacts.size() - maxVisibleSlots, scrollOffset + 1);
            }
        }
    }


    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, COLOR_BACKGROUND);
        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, 15, COLOR_TITLE);

        int listLeft = this.width / 2 - 120;
        int listWidth = 240;

        drawRect(listLeft - 1, listTop - 1, listLeft + listWidth + 1, listBottom + 1, COLOR_LIST_BORDER);
        drawRect(listLeft, listTop, listLeft + listWidth, listBottom, COLOR_LIST_BG);

        for (int i = 0; i < maxVisibleSlots; i++) {
            int contactIndex = i + scrollOffset;
            if (contactIndex >= 0 && contactIndex < availableContacts.size()) {
                String contactCallsign = availableContacts.get(contactIndex);
                boolean isSelected = selectedContacts.contains(contactCallsign);

                int itemY = listTop + i * slotHeight;

                if (isSelected) {
                    drawRect(listLeft, itemY, listLeft + listWidth, itemY + slotHeight, COLOR_SELECTED_ITEM);
                }

                String displayString = contactCallsign;
                this.fontRenderer.drawString(displayString, listLeft + 5, itemY + 4, isSelected ? COLOR_TEXT_SELECTED : COLOR_TEXT_NORMAL);
            }
        }

        if (availableContacts.size() > maxVisibleSlots) {
            int scrollBarHeight = Math.max(10, (int) ((float) maxVisibleSlots / availableContacts.size() * (listBottom - listTop)));
            int scrollBarY = listTop + (int) ((float) scrollOffset / (availableContacts.size() - maxVisibleSlots) * ((listBottom - listTop) - scrollBarHeight));
            drawRect(listLeft + listWidth + 2, scrollBarY, listLeft + listWidth + 5, scrollBarY + scrollBarHeight, COLOR_SCROLLBAR);
        }


        super.drawScreen(mouseX, mouseY, partialTicks);
        if (maxSelections > 1) {
            this.drawCenteredString(this.fontRenderer, "Выбрано: " + selectedContacts.size() + "/" + maxSelections, this.width/2, this.height - 52, COLOR_TEXT_HINT);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            int listLeft = this.width / 2 - 120;
            int listWidth = 240;

            if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listBottom) {
                int slotIndex = (mouseY - listTop) / slotHeight;
                int contactIndex = slotIndex + scrollOffset;

                if (contactIndex >= 0 && contactIndex < availableContacts.size()) {
                    String clickedCallsign = availableContacts.get(contactIndex);
                    if (selectedContacts.contains(clickedCallsign)) {
                        selectedContacts.remove(clickedCallsign);
                    } else {
                        if (selectedContacts.size() < maxSelections) {
                            selectedContacts.add(clickedCallsign);
                        } else if (maxSelections == 1) {
                            selectedContacts.clear();
                            selectedContacts.add(clickedCallsign);
                        }
                    }
                    updateDoneButtonState();
                }
            }
        }
    }
}