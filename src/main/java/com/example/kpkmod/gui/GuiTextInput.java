package com.example.kpkmod.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.function.Consumer;

public class GuiTextInput extends GuiScreen {
    protected GuiScreen parentScreen;
    protected String title;
    protected Consumer<String> onConfirm;

    private GuiTextField inputField;
    private GuiButton doneButton;

    private final int COLOR_BACKGROUND = 0xFF1E1E1E;
    private final int COLOR_TITLE = 0xFFFFAA00;
    private final int COLOR_INPUT_BG = 0xFF000000;
    private final int COLOR_INPUT_BORDER = 0xFF555555;

    public GuiTextInput(GuiScreen parent, String title, Consumer<String> onConfirm) {
        this.parentScreen = parent;
        this.title = title;
        this.onConfirm = onConfirm;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        this.doneButton = this.addButton(new GuiButton(0, this.width / 2 - 100, this.height / 2 + 25, "Готово"));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 2 + 50, "Отмена"));

        this.inputField = new GuiTextField(2, this.fontRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20);
        this.inputField.setMaxStringLength(35);
        this.inputField.setFocused(true);
        this.inputField.setEnableBackgroundDrawing(false);

        updateDoneButtonState();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.inputField.updateCursorCounter();
        updateDoneButtonState();
    }

    private void updateDoneButtonState() {
        this.doneButton.enabled = !this.inputField.getText().trim().isEmpty();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) {
            if (button.id == 0) {
                this.onConfirm.accept(this.inputField.getText().trim());
                this.mc.displayGuiScreen(this.parentScreen);
            } else if (button.id == 1) {
                this.mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.inputField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN && this.doneButton.enabled) {
            this.actionPerformed(this.doneButton);
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, COLOR_BACKGROUND);
        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, this.height / 2 - 40, COLOR_TITLE);

        drawRect(this.inputField.x - 1, this.inputField.y - 1, this.inputField.x + this.inputField.width + 1, this.inputField.y + this.inputField.height + 1, COLOR_INPUT_BORDER);
        drawRect(this.inputField.x, this.inputField.y, this.inputField.x + this.inputField.width, this.inputField.y + this.inputField.height, COLOR_INPUT_BG);
        this.inputField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}