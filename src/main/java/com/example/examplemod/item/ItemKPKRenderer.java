package com.example.examplemod.item;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.User;
import com.example.examplemod.chat.ChatChannel;
import com.example.examplemod.chat.ChatChannelType;
import com.example.examplemod.chat.ChatMessage;
import com.example.examplemod.chat.ClientChatCache;
import com.example.examplemod.gui.KPKModelInteractionGui;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class ItemKPKRenderer extends TileEntityItemStackRenderer {
    private static ItemCameraTransforms.TransformType currentTransform = ItemCameraTransforms.TransformType.NONE;
    private IBakedModel tabletModel;
    private boolean modelLoaded = false;
    private static final float MODEL_SCALE = 0.4f;
    private static Minecraft mc = Minecraft.getMinecraft();
    private int previousFormattedLinesCount = 0;
    private int channelScrollOffset = 0;

    private static final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer viewport = BufferUtils.createIntBuffer(16);

    private static final Map<String, List<Pair<String, Boolean>>> FONT_CACHE = new ConcurrentHashMap<>();
    private static boolean isFontCacheDirty = true;
    private static int lastScreenWidth = 0;
    private static int lastScreenHeight = 0;

    private static VertexBuffer tabletVBO;

    static {
        ClientChatCache.addOnChatDataUpdatedListener(ItemKPKRenderer::invalidateFontCache);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (tabletVBO != null) {
                tabletVBO.deleteGlBuffers();
            }
        }));
    }

    public static void invalidateFontCache() {
        isFontCacheDirty = true;
    }


    public static Rectangle modelInfoButtonRectOnScreen = null;
    public static Rectangle modelChatButtonRectOnScreen = null;
    public static Rectangle modelContactsButtonRectOnScreen = null;
    public static Rectangle modelChatCreateButtonRectOnScreen = null;
    public static Rectangle modelChatInputFieldRectOnScreen = null;
    public static Rectangle modelChatSendButtonRectOnScreen = null;
    public static Rectangle modelChatAnonymousButtonRectOnScreen = null;
    public static Rectangle modelContactsAddButtonRectOnScreen = null;
    public static Rectangle modelContactsInputFieldRectOnScreen = null;
    public static Rectangle modelContactsConfirmAddRectOnScreen = null;
    public static Rectangle modelChatCreatePmButtonRectOnScreen = null;
    public static Rectangle modelChatCreateGroupButtonRectOnScreen = null;
    public static List<Rectangle> modelChannelListButtonRectsOnScreen = new ArrayList<>();
    public static List<String> modelChannelListButtonAssociatedId = new ArrayList<>();
    public static Rectangle modelChannelListAreaRectOnScreen = null;
    public static List<Rectangle> modelContactDeleteButtonRectsOnScreen = new ArrayList<>();
    public static List<String> modelContactDeleteButtonAssociatedName = new ArrayList<>();
    public static List<Rectangle> modelChannelDeleteButtonRectsOnScreen = new ArrayList<>();
    public static List<String> modelChannelDeleteButtonAssociatedId = new ArrayList<>();
    public static List<Rectangle> modelMemberRemoveButtonRectsOnScreen = new ArrayList<>();
    public static List<UUID> modelMemberRemoveButtonAssociatedId = new ArrayList<>();

    private static final int COLOR_BORDER = 0xFF1E1E1E;
    private static final int COLOR_INACTIVE_TOP = 0xFF4F4F4F;
    private static final int COLOR_INACTIVE_BOTTOM = 0xFF3A3A3A;
    private static final int COLOR_ACTIVE_TOP = 0xFF6A6A6A;
    private static final int COLOR_ACTIVE_BOTTOM = 0xFF555555;
    private static final int COLOR_POSITIVE_TOP = 0xFF00B200;
    private static final int COLOR_POSITIVE_BOTTOM = 0xFF008C00;
    private static final int COLOR_NEGATIVE_TOP = 0xFFD40000;
    private static final int COLOR_NEGATIVE_BOTTOM = 0xFFA00000;

    private static final int COLOR_INPUT_BG = 0xFF2B2B2B;
    private static final int COLOR_INPUT_SHADOW_DARK = 0xFF212121;
    private static final int COLOR_INPUT_SHADOW_LIGHT = 0xFF454545;
    private static final int COLOR_INPUT_BORDER_ACTIVE = 0xFF55AFFF;

    public ItemKPKRenderer() {
        super();
    }

    private void drawStyledInputField(int x, int y, int width, int height, boolean isActive) {
        if (isActive) {
            Gui.drawRect(x - 1, y - 1, x + width + 1, y, COLOR_INPUT_BORDER_ACTIVE); // Top
            Gui.drawRect(x - 1, y + height, x + width + 1, y + height + 1, COLOR_INPUT_BORDER_ACTIVE); // Bottom
            Gui.drawRect(x - 1, y, x, y + height, COLOR_INPUT_BORDER_ACTIVE); // Left
            Gui.drawRect(x + width, y, x + width + 1, y + height, COLOR_INPUT_BORDER_ACTIVE); // Right
        }

        Gui.drawRect(x, y, x + width, y + height, COLOR_INPUT_BG);

        Gui.drawRect(x, y, x + width, y + 1, COLOR_INPUT_SHADOW_DARK);
        Gui.drawRect(x, y + 1, x + 1, y + height, COLOR_INPUT_SHADOW_DARK);
        Gui.drawRect(x + 1, y + height - 1, x + width, y + height, COLOR_INPUT_SHADOW_LIGHT);
        Gui.drawRect(x + width - 1, y + 1, x + width, y + height - 1, COLOR_INPUT_SHADOW_LIGHT);
    }

    private void drawStyledButton(int x, int y, int width, int height, int topColor, int bottomColor, int borderColor) {
        Gui.drawRect(x, y, x + width, y + height, borderColor);
        this.drawGradientRect(x + 1, y + 1, x + width - 1, y + height - 1, topColor, bottomColor);
    }

    protected void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >> 8 & 255) / 255.0F;
        float startBlue = (float)(startColor & 255) / 255.0F;
        float endAlpha = (float)(endColor >> 24 & 255) / 255.0F;
        float endRed = (float)(endColor >> 16 & 255) / 255.0F;
        float endGreen = (float)(endColor >> 8 & 255) / 255.0F;
        float endBlue = (float)(endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos((double)right, (double)top, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        bufferbuilder.pos((double)left, (double)top, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        bufferbuilder.pos((double)left, (double)bottom, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        bufferbuilder.pos((double)right, (double)bottom, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private void loadModel() {
        if (!modelLoaded) {
            try {
                IModel model = ModelLoaderRegistry.getModel(new ResourceLocation(ExampleMod.MODID, "item/kpk.obj"));
                if (model instanceof OBJModel) {
                    OBJModel objModel = (OBJModel) model;
                    objModel = (OBJModel) objModel.retexture(ImmutableMap.of(
                            "material", ExampleMod.MODID + ":items/kpk_item",
                            "#material", ExampleMod.MODID + ":items/kpk_item"
                    ));
                    objModel = (OBJModel) objModel.process(ImmutableMap.of("flip-v", "true"));
                    model = objModel;
                }
                tabletModel = model.bake(
                        model.getDefaultState(),
                        DefaultVertexFormats.ITEM,
                        location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString())
                );
                modelLoaded = true;
            } catch (Exception e) {
                System.err.println("Failed to load tablet model: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        loadModel();
        GlStateManager.pushMatrix();
        applyTabletTransform(currentTransform);

        if (tabletModel != null) {
            renderTabletModel();
        }

        if (currentTransform == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND ||
                currentTransform == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND) {
            renderHandsHoldingTablet();
            if (ItemKPK.isEnabled(stack)) {
                renderModelScreen(stack);
            }
        }
        GlStateManager.popMatrix();
    }

    private void renderHandsHoldingTablet() {
        AbstractClientPlayer player = mc.player;
        if (player == null) return;
        GlStateManager.pushMatrix();
        renderArm(EnumHandSide.RIGHT, player);
        renderArm(EnumHandSide.LEFT, player);
        GlStateManager.popMatrix();
    }

    private void renderArm(EnumHandSide side, AbstractClientPlayer player) {
        GlStateManager.pushMatrix();
        if (side == EnumHandSide.RIGHT) {
            GlStateManager.translate(7.6F, -7F, -13F);
            GlStateManager.rotate(-10.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(8.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-15.0F, 0.0F, 0.0F, 1.0F);
        } else {
            GlStateManager.translate(1.5F, -5.6F, 3.7F);
            GlStateManager.rotate(20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-12.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-2.0F, 0.0F, 0.0F, 1.0F);
        }
        GlStateManager.scale(10.2F, 10.2F, 10.2F);
        mc.getTextureManager().bindTexture(player.getLocationSkin());
        GlStateManager.enableLighting();
        GlStateManager.disableCull();
        if (side == EnumHandSide.RIGHT) {
            mc.getRenderManager().getSkinMap().get("default").renderRightArm(player);
        } else {
            mc.getRenderManager().getSkinMap().get("default").renderLeftArm(player);
        }
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    private void generateTabletVBO() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.ITEM);
        for (EnumFacing enumfacing : EnumFacing.values()) {
            renderQuads(bufferbuilder, tabletModel.getQuads(null, enumfacing, 0L), -1);
        }
        renderQuads(bufferbuilder, tabletModel.getQuads(null, null, 0L), -1);

        bufferbuilder.finishDrawing();
        tabletVBO = new VertexBuffer(DefaultVertexFormats.ITEM);
        tabletVBO.bufferData(bufferbuilder.getByteBuffer().asReadOnlyBuffer());
    }


    private void renderTabletModel() {
        if (tabletModel == null) return;

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderHelper.enableStandardItemLighting();

        GlStateManager.pushMatrix();
        GlStateManager.translate(1F, 2F, -6);

        if (tabletVBO == null) {
            generateTabletVBO();
        }

        tabletVBO.bindBuffer();

        int stride = DefaultVertexFormats.ITEM.getIntegerSize() * 4;
        GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, stride, 0);
        GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, stride, 12);
        GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, stride, 16);
        GL11.glNormalPointer(GL11.GL_BYTE, stride, 24);

        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);

        tabletVBO.drawArrays(GL11.GL_QUADS);

        tabletVBO.unbindBuffer();
        GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    private void renderQuads(BufferBuilder renderer, List<BakedQuad> quads, int color) {
        for (BakedQuad bakedquad : quads) {
            net.minecraftforge.client.model.pipeline.LightUtil.renderQuadColor(renderer, bakedquad, color);
        }
    }

    private void renderModelScreen(ItemStack stack) {
        boolean interactionGuiOpen = mc.currentScreen instanceof KPKModelInteractionGui;

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 2.0F, -6.005F);

        float desiredInterfaceWidthVirtual = 180f;
        float desiredInterfaceHeightVirtual = 110f;
        float modelPhysicalScreenWidth = 1.7f;
        float interfaceScale = modelPhysicalScreenWidth / desiredInterfaceWidthVirtual;
        GlStateManager.scale(interfaceScale, interfaceScale, interfaceScale);

        GlStateManager.rotate(270F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);

        GlStateManager.translate(-desiredInterfaceWidthVirtual / 2f, -desiredInterfaceHeightVirtual / 2f, 0.01F);

        ScaledResolution sr = null;
        if (interactionGuiOpen) {
            modelView.clear();
            projection.clear();
            viewport.clear();

            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            sr = new ScaledResolution(mc);
        }

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableDepth();

        FontRenderer fontRenderer = mc.fontRenderer;

        int topButtonWidth = 45;
        int topButtonHeight = 14;
        int topButtonY = -68;
        int topButtonSpacing = 3;
        int totalTopButtonsWidth = (topButtonWidth * ItemKPK.TOTAL_MODEL_PAGES) + (topButtonSpacing * (ItemKPK.TOTAL_MODEL_PAGES -1));
        int topTabStartX = (int)((desiredInterfaceWidthVirtual - totalTopButtonsWidth) / 2f - 176);
        int currentTopButtonX = topTabStartX;

        int currentPage = ItemKPK.getCurrentModelPage(stack);

        boolean isInfoActive = (currentPage == ItemKPK.PAGE_INFO);
        drawStyledButton(currentTopButtonX, topButtonY, topButtonWidth, topButtonHeight,
                isInfoActive ? COLOR_ACTIVE_TOP : COLOR_INACTIVE_TOP,
                isInfoActive ? COLOR_ACTIVE_BOTTOM : COLOR_INACTIVE_BOTTOM,
                COLOR_BORDER);
        drawCenteredStringWithShadow(fontRenderer, TextFormatting.BOLD + "ИНФА", currentTopButtonX + topButtonWidth/2, topButtonY + (topButtonHeight - fontRenderer.FONT_HEIGHT)/2 + 1, isInfoActive ? 0xFFFFFFFF : 0xFFAAAAAA);
        if(interactionGuiOpen) ItemKPKRenderer.modelInfoButtonRectOnScreen = calculateScreenRectForVirtual(currentTopButtonX, topButtonY, topButtonWidth, topButtonHeight, modelView, projection, viewport, sr);

        currentTopButtonX += topButtonWidth + topButtonSpacing;

        boolean isChatActive = (currentPage == ItemKPK.PAGE_CHAT);
        drawStyledButton(currentTopButtonX, topButtonY, topButtonWidth, topButtonHeight,
                isChatActive ? COLOR_ACTIVE_TOP : COLOR_INACTIVE_TOP,
                isChatActive ? COLOR_ACTIVE_BOTTOM : COLOR_INACTIVE_BOTTOM,
                COLOR_BORDER);
        drawCenteredStringWithShadow(fontRenderer, TextFormatting.BOLD + "ЧАТ", currentTopButtonX + topButtonWidth/2, topButtonY + (topButtonHeight - fontRenderer.FONT_HEIGHT) / 2 + 1, isChatActive ? 0xFFFFFFFF : 0xFFAAAAAA);
        if(interactionGuiOpen) ItemKPKRenderer.modelChatButtonRectOnScreen = calculateScreenRectForVirtual(currentTopButtonX, topButtonY, topButtonWidth, topButtonHeight, modelView, projection, viewport, sr);

        currentTopButtonX += topButtonWidth + topButtonSpacing;

        boolean isContactsActive = (currentPage == ItemKPK.PAGE_CONTACTS);
        drawStyledButton(currentTopButtonX, topButtonY, topButtonWidth, topButtonHeight,
                isContactsActive ? COLOR_ACTIVE_TOP : COLOR_INACTIVE_TOP,
                isContactsActive ? COLOR_ACTIVE_BOTTOM : COLOR_INACTIVE_BOTTOM,
                COLOR_BORDER);
        drawCenteredStringWithShadow(fontRenderer, TextFormatting.BOLD + "КОНТАКТЫ", currentTopButtonX + topButtonWidth/2, topButtonY + (topButtonHeight - fontRenderer.FONT_HEIGHT)/2 + 1, isContactsActive ? 0xFFFFFFFF : 0xFFAAAAAA);
        if(interactionGuiOpen) ItemKPKRenderer.modelContactsButtonRectOnScreen = calculateScreenRectForVirtual(currentTopButtonX, topButtonY, topButtonWidth, topButtonHeight, modelView, projection, viewport, sr);

        if (!interactionGuiOpen) {
            modelChatCreateButtonRectOnScreen = null;
            modelContactsAddButtonRectOnScreen = null;
            modelContactsInputFieldRectOnScreen = null;
            modelContactsConfirmAddRectOnScreen = null;
            modelChatInputFieldRectOnScreen = null;
            modelChatSendButtonRectOnScreen = null;
            modelChatAnonymousButtonRectOnScreen = null;
            modelChatCreatePmButtonRectOnScreen = null;
            modelChatCreateGroupButtonRectOnScreen = null;
            modelChannelListAreaRectOnScreen = null;
            if (modelChannelListButtonRectsOnScreen != null) modelChannelListButtonRectsOnScreen.clear();
            if (modelChannelListButtonAssociatedId != null) modelChannelListButtonAssociatedId.clear();
            if (modelContactDeleteButtonRectsOnScreen != null) modelContactDeleteButtonRectsOnScreen.clear();
            if (modelContactDeleteButtonAssociatedName != null) modelContactDeleteButtonAssociatedName.clear();
            if (modelChannelDeleteButtonRectsOnScreen != null) modelChannelDeleteButtonRectsOnScreen.clear();
            if (modelChannelDeleteButtonAssociatedId != null) modelChannelDeleteButtonAssociatedId.clear();
            if (modelMemberRemoveButtonRectsOnScreen != null) modelMemberRemoveButtonRectsOnScreen.clear();
            if (modelMemberRemoveButtonAssociatedId != null) modelMemberRemoveButtonAssociatedId.clear();
        }

        User userData = ItemKPK.getUserData(stack);

        if (userData != null) {
            if (currentPage == ItemKPK.PAGE_INFO) {
                renderInfoPage(userData);
            } else if (currentPage == ItemKPK.PAGE_CHAT) {
                renderChatPage(stack, interactionGuiOpen, modelView, projection, viewport, sr);
            } else if (currentPage == ItemKPK.PAGE_CONTACTS) {
                if (ItemKPK.isChatCreationMode(stack)) {
                    renderContactSelection(stack, interactionGuiOpen, modelView, projection, viewport, sr);
                } else {
                    renderContactList(stack, interactionGuiOpen, modelView, projection, viewport, sr);
                }
            }
        } else {
            drawCenteredStringWithShadow(fontRenderer, "КПК НЕ ИНИЦИАЛИЗИРОВАН", 0, -5, 0xFF5555);
            drawCenteredStringWithShadow(fontRenderer, "Используйте /kpk set", 0, 5, 0xFFFF55);
        }

        if (currentPage != ItemKPK.PAGE_CHAT) {
            this.previousFormattedLinesCount = 0;
        }

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderInfoPage(User userData) {
        int contentStartX = -150;
        int lineHeight = mc.fontRenderer.FONT_HEIGHT + 2;

        if (userData == null) {
            mc.fontRenderer.drawStringWithShadow("ДАННЫЕ НЕ ЗАГРУЖЕНЫ", contentStartX + 100, 0, 0xFFCC00);
        } else {
            String title = TextFormatting.GOLD + "" + TextFormatting.BOLD + "КПК: УСТРОЙСТВО";
            GlStateManager.pushMatrix();
            float titleScaleFactor = 1.5f;
            float originalTitleX = contentStartX + 355;
            float originalTitleY = -46;

            GlStateManager.translate(originalTitleX, originalTitleY, 0);
            GlStateManager.scale(titleScaleFactor, titleScaleFactor, 1.0f);
            mc.fontRenderer.drawStringWithShadow(title, 0, 0, 0xFFD700);
            GlStateManager.popMatrix();

            float dataBlockStartY = originalTitleY + (mc.fontRenderer.FONT_HEIGHT * titleScaleFactor) + 5;
            float dataBlockStartX = contentStartX;
            String[] labels = {"Владелец:", "Позывной:", "Дата рождения:", "Пол:"};
            String[] values = {
                    TextFormatting.WHITE + userData.familiya + " " + userData.name,
                    TextFormatting.WHITE + userData.pozivnoy,
                    TextFormatting.WHITE + userData.birthdate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    TextFormatting.WHITE + userData.gender.getDisplayName()
            };
            float dataScaleFactor = 2.3f;
            for(int i = 0; i < labels.length; i++) {
                String lineToDraw = labels[i] + " " + values[i];
                GlStateManager.pushMatrix();
                float currentLineY = dataBlockStartY + (i * (int)(lineHeight * dataScaleFactor));
                GlStateManager.translate(dataBlockStartX, currentLineY , 0);
                GlStateManager.scale(dataScaleFactor, dataScaleFactor, 1.0f);
                mc.fontRenderer.drawStringWithShadow(lineToDraw, 0, 0, 0x00FFFF);
                GlStateManager.popMatrix();
            }
        }
    }

    private void renderChatPage(ItemStack stack, boolean interactionGuiOpen, FloatBuffer mv, FloatBuffer proj, IntBuffer vp, ScaledResolution sr) {
        renderChannelList(stack, interactionGuiOpen, mv, proj, vp, sr);
        renderChatHistoryAndInput(stack, interactionGuiOpen, mv, proj, vp, sr);
        renderMemberList(stack, interactionGuiOpen, mv, proj, vp, sr);

        if (interactionGuiOpen && ItemKPK.isChatCreationMode(stack)) {
            renderChatCreateOverlay(stack, interactionGuiOpen, mv, proj, vp, sr);
        }
    }

    private void renderChannelList(ItemStack stack, boolean interactionGuiOpen, FloatBuffer mv, FloatBuffer proj, IntBuffer vp, ScaledResolution sr) {
        int listX = -157;
        int listY = -45;
        int listWidth = 93;
        int listHeight = 195;
        int createBtnHeight = 20;
        int itemHeight = 22;

        Gui.drawRect(listX, listY, listX + listWidth, listY + listHeight, 0xAA000000);
        if (interactionGuiOpen) {
            modelChannelListAreaRectOnScreen = calculateScreenRectForVirtual(listX, listY, listWidth, listHeight, mv, proj, vp, sr);
        }

        if (interactionGuiOpen) {
            modelChannelListButtonRectsOnScreen.clear();
            modelChannelListButtonAssociatedId.clear();
            modelChannelDeleteButtonRectsOnScreen.clear();
            modelChannelDeleteButtonAssociatedId.clear();
        }

        List<ChatChannel> channels = ClientChatCache.getSubscribedChannels();

        int maxVisible = (listHeight - createBtnHeight - 5) / itemHeight;

        if (mc.currentScreen instanceof KPKModelInteractionGui) {
            KPKModelInteractionGui gui = (KPKModelInteractionGui) mc.currentScreen;
            if (gui.channelScrollOffset > channels.size() - maxVisible) gui.channelScrollOffset = Math.max(0, channels.size() - maxVisible);
            if (gui.channelScrollOffset < 0) gui.channelScrollOffset = 0;
            this.channelScrollOffset = gui.channelScrollOffset;
        }

        String currentChannelId = ItemKPK.getCurrentChatChannelId(stack);

        for (int i = 0; i < maxVisible; i++) {
            int channelIndex = i + channelScrollOffset;
            if (channelIndex < channels.size()) {
                ChatChannel channel = channels.get(channelIndex);
                int itemY = listY + i * itemHeight;
                boolean isActive = channel.getChannelId().equals(currentChannelId);

                drawStyledButton(listX, itemY, listWidth, itemHeight,
                        isActive ? COLOR_ACTIVE_TOP : COLOR_INACTIVE_TOP,
                        isActive ? COLOR_ACTIVE_BOTTOM : COLOR_INACTIVE_BOTTOM,
                        COLOR_BORDER);

                String displayName = mc.fontRenderer.trimStringToWidth(channel.getDisplayName(), listWidth - 16);
                drawCenteredStringWithShadow(mc.fontRenderer, displayName, listX + listWidth / 2, itemY + (itemHeight - 8) / 2, isActive ? 0xFFFFFFFF : 0xFFAAAAAA);

                if (interactionGuiOpen) {
                    modelChannelListButtonRectsOnScreen.add(calculateScreenRectForVirtual(listX, itemY, listWidth, itemHeight, mv, proj, vp, sr));
                    modelChannelListButtonAssociatedId.add(channel.getChannelId());

                    if (channel.getType() == ChatChannelType.PRIVATE_MESSAGE || channel.getType() == ChatChannelType.PRIVATE_GROUP) {
                        int delButtonSize = 10;
                        int delButtonX = listX + listWidth - delButtonSize - 2;
                        int delButtonY = itemY + (itemHeight - delButtonSize) / 2;
                        drawStyledButton(delButtonX, delButtonY, delButtonSize, delButtonSize, COLOR_NEGATIVE_TOP, COLOR_NEGATIVE_BOTTOM, COLOR_BORDER);
                        drawCenteredStringWithShadow(mc.fontRenderer, "X", delButtonX + delButtonSize / 2, delButtonY + 1, 0xFFFFFFFF);

                        modelChannelDeleteButtonRectsOnScreen.add(calculateScreenRectForVirtual(delButtonX, delButtonY, delButtonSize, delButtonSize, mv, proj, vp, sr));
                        modelChannelDeleteButtonAssociatedId.add(channel.getChannelId());
                    }
                }
            }
        }

        int createBtnY = listY + listHeight - createBtnHeight + 18;
        drawStyledButton(listX, createBtnY, listWidth, createBtnHeight, COLOR_INACTIVE_TOP, COLOR_INACTIVE_BOTTOM, COLOR_BORDER);
        drawCenteredStringWithShadow(mc.fontRenderer, TextFormatting.BOLD + "Создать", listX + listWidth/2, createBtnY + (createBtnHeight - 8) / 2 + 1, 0xFFFFFFFF);
        if (interactionGuiOpen) {
            modelChatCreateButtonRectOnScreen = calculateScreenRectForVirtual(listX, createBtnY, listWidth, createBtnHeight, mv, proj, vp, sr);
        }
    }

    private void renderChatHistoryAndInput(ItemStack stack, boolean interactionGuiOpen, FloatBuffer mv, FloatBuffer proj, IntBuffer vp, ScaledResolution sr) {
        KPKModelInteractionGui kpkGui = null;
        if (interactionGuiOpen) {
            kpkGui = (KPKModelInteractionGui) mc.currentScreen;
        }

        String currentChannelId = ItemKPK.getCurrentChatChannelId(stack);
        ChatChannel channel = ClientChatCache.getChannel(currentChannelId);

        if (mc.displayWidth != lastScreenWidth || mc.displayHeight != lastScreenHeight) {
            invalidateFontCache();
            lastScreenWidth = mc.displayWidth;
            lastScreenHeight = mc.displayHeight;
        }

        final float CHAT_TEXT_SCALE = 1.5f;
        int chatHistoryX = -54;
        int chatHistoryY = -45;

        int anonymousButtonHeight = 16;
        int chatInputY = 171 - anonymousButtonHeight - 2;

        final int MEMBER_LIST_WIDTH = 80;
        final int GAP = 6;
        final int FULL_WIDTH = 364;

        int chatHistoryWidth;
        if (channel != null && channel.getType() != ChatChannelType.COMMON_SERVER_WIDE) {
            chatHistoryWidth = FULL_WIDTH - MEMBER_LIST_WIDTH - GAP;
        } else {
            chatHistoryWidth = FULL_WIDTH;
        }

        int chatHistoryHeight = chatInputY - chatHistoryY;
        int chatLineHeight = 10;
        int maxVisibleLines = (int)(chatHistoryHeight / (chatLineHeight * CHAT_TEXT_SCALE));

        List<Pair<String, Boolean>> finalLines;

        if (isFontCacheDirty) {
            FONT_CACHE.clear();

            for (ChatChannel subscribedChannel : ClientChatCache.getSubscribedChannels()) {
                String cacheChannelId = subscribedChannel.getChannelId();
                int currentWrappingWidth;
                if (subscribedChannel.getType() != ChatChannelType.COMMON_SERVER_WIDE) {
                    currentWrappingWidth = (int)((FULL_WIDTH - MEMBER_LIST_WIDTH - GAP) / CHAT_TEXT_SCALE);
                } else {
                    currentWrappingWidth = (int)(FULL_WIDTH / CHAT_TEXT_SCALE);
                }

                List<Pair<String, Boolean>> processedLines = new ArrayList<>();
                List<ChatMessage> messages = ClientChatCache.getChatHistory(cacheChannelId);

                for (ChatMessage msg : messages) {
                    boolean isSelf = msg.senderUuid != null && mc.player != null && msg.senderUuid.equals(mc.player.getUniqueID());

                    String senderName = msg.isAnonymous ? (TextFormatting.GRAY + "Аноним") : (TextFormatting.GRAY + msg.senderCallsign);

                    if (isSelf && !msg.isAnonymous) {
                        String suffix = " :" + senderName + " " + TextFormatting.GRAY + "[" + msg.getFormattedTimestamp() + "]";
                        int wrapWidth = currentWrappingWidth - mc.fontRenderer.getStringWidth(suffix);
                        List<String> wrappedContent = mc.fontRenderer.listFormattedStringToWidth(msg.messageContent, wrapWidth > 0 ? wrapWidth : 1);

                        for(int i = 0; i < wrappedContent.size(); i++) {
                            String line = wrappedContent.get(i);
                            processedLines.add(Pair.of(i == wrappedContent.size() - 1 ? line + suffix : line, true));
                        }
                    } else {
                        String prefix = TextFormatting.GRAY + "[" + msg.getFormattedTimestamp() + "] " + senderName + TextFormatting.WHITE + ": ";
                        int wrapWidth = currentWrappingWidth - mc.fontRenderer.getStringWidth(prefix);
                        List<String> wrappedContent = mc.fontRenderer.listFormattedStringToWidth(msg.messageContent, wrapWidth > 0 ? wrapWidth : 1);
                        String indent = mc.fontRenderer.trimStringToWidth(" ", mc.fontRenderer.getStringWidth(prefix));

                        for (int i = 0; i < wrappedContent.size(); i++) {
                            processedLines.add(Pair.of((i == 0 ? prefix : indent) + wrappedContent.get(i), false));
                        }
                    }
                }
                FONT_CACHE.put(cacheChannelId, processedLines);
            }
            isFontCacheDirty = false;
        }

        finalLines = FONT_CACHE.getOrDefault(currentChannelId, Collections.emptyList());

        int currentFormattedLinesCount = finalLines.size();
        if (interactionGuiOpen && kpkGui != null) {
            boolean wasScrolledToBottom = (maxVisibleLines <= 0) || (kpkGui.getChatScrollOffset() >= this.previousFormattedLinesCount - maxVisibleLines);
            if (currentFormattedLinesCount > this.previousFormattedLinesCount && wasScrolledToBottom) {
                kpkGui.setChatScrollOffset(Integer.MAX_VALUE);
            }
        }
        this.previousFormattedLinesCount = currentFormattedLinesCount;

        int scrollOffset = 0;
        int maxScroll = 0;
        boolean canScroll = finalLines.size() > maxVisibleLines;
        if (interactionGuiOpen && kpkGui != null) {
            scrollOffset = kpkGui.getChatScrollOffset();
            if (canScroll) {
                maxScroll = finalLines.size() - maxVisibleLines;
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
                kpkGui.setChatScrollOffset(scrollOffset);
            } else {
                scrollOffset = 0;
                kpkGui.setChatScrollOffset(0);
            }
        }

        Gui.drawRect(chatHistoryX, chatHistoryY, chatHistoryX + chatHistoryWidth, chatHistoryY + chatHistoryHeight, 0x55000000);

        for (int i = 0; i < maxVisibleLines; i++) {
            int lineIndex = i + scrollOffset;
            if (lineIndex >= 0 && lineIndex < finalLines.size()) {
                Pair<String, Boolean> lineData = finalLines.get(lineIndex);
                String line = lineData.getLeft();
                boolean isSelf = lineData.getRight();

                GlStateManager.pushMatrix();
                float yLinePos = chatHistoryY + 2 + (i * chatLineHeight * CHAT_TEXT_SCALE);
                float xLinePos = isSelf ? chatHistoryX + chatHistoryWidth - 2 : chatHistoryX + 2;

                GlStateManager.translate(xLinePos, yLinePos, 0);
                GlStateManager.scale(CHAT_TEXT_SCALE, CHAT_TEXT_SCALE, 1.0f);

                mc.fontRenderer.drawStringWithShadow(line, isSelf ? -mc.fontRenderer.getStringWidth(line) : 0, 0, 0xFFFFFF);
                GlStateManager.popMatrix();
            }
        }

        if (canScroll) {
            int scrollbarX = chatHistoryX + chatHistoryWidth + 2;
            Gui.drawRect(scrollbarX, chatHistoryY, scrollbarX + 2, chatHistoryY + chatHistoryHeight, 0xFF101010);
            int handleHeight = Math.max(10, (int)((float)maxVisibleLines / finalLines.size() * chatHistoryHeight));
            int handleY = chatHistoryY + (int)((float)scrollOffset / maxScroll * (chatHistoryHeight - handleHeight));
            Gui.drawRect(scrollbarX, handleY, scrollbarX + 2, handleY + handleHeight, 0xFF888888);
        }

        if (interactionGuiOpen && kpkGui != null) {
            int anonymousButtonY = chatInputY - 2;
            int inputAreaY = anonymousButtonY + anonymousButtonHeight + 2;
            int inputFieldHeight = 20;
            int sendButtonWidth = 20;
            int inputFieldWidth = chatHistoryWidth - sendButtonWidth - 2;
            int inputFieldX = chatHistoryX;
            int sendButtonX = inputFieldX + inputFieldWidth + 2;

            if (currentChannelId.equals(ChatChannel.COMMON_CHANNEL_ID_PREFIX)) {
                boolean isAnonymousMode = kpkGui.isAnonymous();
                int anonButtonWidth = 80;
                int anonButtonX = inputFieldX;
                drawStyledButton(anonButtonX, anonymousButtonY, anonButtonWidth, anonymousButtonHeight,
                        isAnonymousMode ? COLOR_ACTIVE_TOP : COLOR_INACTIVE_TOP,
                        isAnonymousMode ? COLOR_ACTIVE_BOTTOM : COLOR_INACTIVE_BOTTOM,
                        COLOR_BORDER);
                drawCenteredStringWithShadow(mc.fontRenderer, "Анонимно", anonButtonX + anonButtonWidth/2, anonymousButtonY + (anonymousButtonHeight-8)/2, isAnonymousMode ? 0xFFFFFFFF : 0xFFAAAAAA);
                modelChatAnonymousButtonRectOnScreen = calculateScreenRectForVirtual(anonButtonX, anonymousButtonY, anonButtonWidth, anonymousButtonHeight, mv, proj, vp, sr);
            } else {
                modelChatAnonymousButtonRectOnScreen = null;
            }

            String currentInput = kpkGui.getCurrentChatInput();
            boolean inputActive = kpkGui.isChatInputActive();
            int cursorBlink = kpkGui.getCursorCounter();

            drawStyledInputField(inputFieldX, inputAreaY, inputFieldWidth, inputFieldHeight, inputActive);

            String displayText = currentInput;
            if (inputActive && (cursorBlink / 6) % 2 == 0) displayText += "_";

            int availableTextWidth = inputFieldWidth - 6;
            if (mc.fontRenderer.getStringWidth(displayText) > availableTextWidth) {
                displayText = mc.fontRenderer.trimStringToWidth(displayText, availableTextWidth, true);
            }
            mc.fontRenderer.drawStringWithShadow(displayText, inputFieldX + 3, inputAreaY + (inputFieldHeight - mc.fontRenderer.FONT_HEIGHT)/2 + 1, 0xFFEEEEEE);
            modelChatInputFieldRectOnScreen = calculateScreenRectForVirtual(inputFieldX, inputAreaY, inputFieldWidth, inputFieldHeight, mv, proj, vp, sr);

            drawStyledButton(sendButtonX, inputAreaY, sendButtonWidth, inputFieldHeight, COLOR_POSITIVE_TOP, COLOR_POSITIVE_BOTTOM, COLOR_BORDER);
            drawCenteredStringWithShadow(mc.fontRenderer, ">", sendButtonX + sendButtonWidth/2, inputAreaY + (inputFieldHeight - mc.fontRenderer.FONT_HEIGHT)/2, 0xFFFFFFFF);
            modelChatSendButtonRectOnScreen = calculateScreenRectForVirtual(sendButtonX, inputAreaY, sendButtonWidth, inputFieldHeight, mv, proj, vp, sr);
        }
    }

    private void renderMemberList(ItemStack stack, boolean interactionGuiOpen, FloatBuffer mv, FloatBuffer proj, IntBuffer vp, ScaledResolution sr) {
        String currentChannelId = ItemKPK.getCurrentChatChannelId(stack);
        ChatChannel channel = ClientChatCache.getChannel(currentChannelId);

        if (channel == null || channel.getType() == ChatChannelType.COMMON_SERVER_WIDE) {
            return;
        }

        int listX = 230;
        int listY = -45;
        int listWidth = 80;
        int listHeight = 214;
        int itemHeight = 15;
        float textScale = 1.3f;

        Gui.drawRect(listX, listY, listX + listWidth, listY + listHeight, 0x55000000);
        drawCenteredStringWithShadow(mc.fontRenderer, "Участники", listX + listWidth/2, listY + 2, 0xFFAAAAAA);

        if (interactionGuiOpen) {
            modelMemberRemoveButtonRectsOnScreen.clear();
            modelMemberRemoveButtonAssociatedId.clear();
        }

        List<UUID> members = channel.getMembers();
        UUID creatorUuid = channel.getCreatorUuid();
        boolean isLocalPlayerCreator = creatorUuid != null && mc.player != null && creatorUuid.equals(mc.player.getUniqueID());

        for (int i = 0; i < members.size(); i++) {
            UUID memberUuid = members.get(i);
            String callsign = ClientChatCache.getCallsignForUUID(memberUuid);

            boolean isCreator = Objects.equals(memberUuid, creatorUuid);
            String prefix = isCreator ? TextFormatting.GOLD + "[★] " : "";
            String fullText = prefix + TextFormatting.WHITE + callsign;

            int itemY = listY + 12 + i * itemHeight;

            GlStateManager.pushMatrix();
            GlStateManager.translate(listX + 2, itemY, 0);
            GlStateManager.scale(textScale, textScale, 1f);
            mc.fontRenderer.drawString(fullText, 0, 0, 0xFFFFFF);
            GlStateManager.popMatrix();

            if (interactionGuiOpen && isLocalPlayerCreator && !memberUuid.equals(mc.player.getUniqueID()) && channel.getType() == ChatChannelType.PRIVATE_GROUP) {
                int delButtonSize = 10;
                int delButtonX = listX + listWidth - delButtonSize - 2;
                int delButtonY = itemY;
                drawStyledButton(delButtonX, delButtonY, delButtonSize, delButtonSize, COLOR_NEGATIVE_TOP, COLOR_NEGATIVE_BOTTOM, COLOR_BORDER);
                drawCenteredStringWithShadow(mc.fontRenderer, "X", delButtonX + delButtonSize / 2, delButtonY + 1, 0xFFFFFFFF);

                modelMemberRemoveButtonRectsOnScreen.add(calculateScreenRectForVirtual(delButtonX, delButtonY, delButtonSize, delButtonSize, mv, proj, vp, sr));
                modelMemberRemoveButtonAssociatedId.add(memberUuid);
            }
        }
    }

    private void renderChatCreateOverlay(ItemStack stack, boolean interactionGuiOpen, FloatBuffer mv, FloatBuffer proj, IntBuffer vp, ScaledResolution sr) {
        int ccsX = -157;
        int ccsY = 170;
        int btnWidth = 45;
        int btnHeight = 25;
        float scale = 1.5f;

        Gui.drawRect(ccsX - 2, ccsY - 2, ccsX + btnWidth * 2 + 5, ccsY + btnHeight + 2, 0xEE111111);

        drawStyledButton(ccsX, ccsY, btnWidth, btnHeight, COLOR_INACTIVE_TOP, COLOR_INACTIVE_BOTTOM, COLOR_BORDER);
        GlStateManager.pushMatrix();
        String lsText = TextFormatting.BOLD + "ЛС";
        float scaledTextWidth = mc.fontRenderer.getStringWidth(lsText) * scale;
        float scaledTextHeight = mc.fontRenderer.FONT_HEIGHT * scale;
        float textX = ccsX + (btnWidth - scaledTextWidth) / 2.0f;
        float textY = ccsY + (btnHeight - scaledTextHeight) / 2.0f;
        GlStateManager.translate(textX, textY, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        mc.fontRenderer.drawStringWithShadow(lsText, 0, 0, 0xFFAAAAAA);
        GlStateManager.popMatrix();

        if (interactionGuiOpen)
            modelChatCreatePmButtonRectOnScreen = calculateScreenRectForVirtual(ccsX, ccsY, btnWidth, btnHeight, mv, proj, vp, sr);

        ccsX += btnWidth + 3;
        drawStyledButton(ccsX, ccsY, btnWidth, btnHeight, COLOR_INACTIVE_TOP, COLOR_INACTIVE_BOTTOM, COLOR_BORDER);
        GlStateManager.pushMatrix();
        String zkText = TextFormatting.BOLD + "ЗК";
        scaledTextWidth = mc.fontRenderer.getStringWidth(zkText) * scale;
        scaledTextHeight = mc.fontRenderer.FONT_HEIGHT * scale;
        textX = ccsX + (btnWidth - scaledTextWidth) / 2.0f;
        textY = ccsY + (btnHeight - scaledTextHeight) / 2.0f;
        GlStateManager.translate(textX, textY, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        mc.fontRenderer.drawStringWithShadow(zkText, 0, 0, 0xFFAAAAAA);
        GlStateManager.popMatrix();

        if (interactionGuiOpen)
            modelChatCreateGroupButtonRectOnScreen = calculateScreenRectForVirtual(ccsX, ccsY, btnWidth, btnHeight, mv, proj, vp, sr);
    }

    private void renderContactSelection(ItemStack stack, boolean interactionGuiOpen, FloatBuffer mv, FloatBuffer proj, IntBuffer vp, ScaledResolution sr) {
        int creationType = ItemKPK.getChatCreationType(stack);
        String title = (creationType == ItemKPK.CHAT_TYPE_PM) ? "Выберите контакт для ЛС" : "Выберите до 2 контактов для ЗК";
        drawCenteredStringWithShadow(mc.fontRenderer, TextFormatting.GOLD + "" + TextFormatting.BOLD + title, 80, -45, 0xFFD700);

        List<String> selectedContacts = ItemKPK.getSelectedContactsForGroup(stack);
        List<String> allContacts = ItemKPK.getContacts(stack);

        if (interactionGuiOpen) {
            modelContactDeleteButtonRectsOnScreen.clear();
            modelContactDeleteButtonAssociatedName.clear();
        }

        int gridStartX = -157;
        int contactListStartY = -25;
        int cellWidth = 148;
        int cellHeight = 22;
        int horizontalSpacing = 10;
        int verticalSpacing = 5;
        int numColumns = 2;

        for (int i = 0; i < Math.min(allContacts.size(), 10); i++) {
            int row = i / numColumns;
            int col = i % numColumns;

            int cellX = gridStartX + col * (cellWidth + horizontalSpacing);
            int cellY = contactListStartY + row * (cellHeight + verticalSpacing);
            String contactName = allContacts.get(i);
            boolean isSelected = selectedContacts.contains(contactName);

            int borderColor = isSelected ? 0xFF00AA00 : 0xFFFFFFFF;
            int bgColor = 0x99000000;
            Gui.drawRect(cellX - 1, cellY - 1, cellX + cellWidth + 1, cellY + cellHeight + 1, borderColor);
            Gui.drawRect(cellX, cellY, cellX + cellWidth, cellY + cellHeight, bgColor);
            drawCenteredStringWithShadow(mc.fontRenderer, contactName, cellX + cellWidth / 2, cellY + (cellHeight - 8) / 2, 0xFFFFFF);

            if (interactionGuiOpen) {
                modelContactDeleteButtonRectsOnScreen.add(calculateScreenRectForVirtual(cellX, cellY, cellWidth, cellHeight, mv, proj, vp, sr));
                modelContactDeleteButtonAssociatedName.add(contactName);
            }
        }

        if (creationType == ItemKPK.CHAT_TYPE_GROUP && interactionGuiOpen) {
            int confirmButtonWidth = 100;
            int confirmButtonHeight = 18;
            int confirmButtonX = 32;
            int confirmButtonY = 179;
            boolean canConfirm = !selectedContacts.isEmpty();

            if (canConfirm) {
                drawStyledButton(confirmButtonX, confirmButtonY, confirmButtonWidth, confirmButtonHeight, COLOR_POSITIVE_TOP, COLOR_POSITIVE_BOTTOM, COLOR_BORDER);
            } else {
                drawStyledButton(confirmButtonX, confirmButtonY, confirmButtonWidth, confirmButtonHeight, COLOR_INACTIVE_TOP, COLOR_INACTIVE_BOTTOM, COLOR_BORDER);
            }
            drawCenteredStringWithShadow(mc.fontRenderer, "Создать Канал", confirmButtonX + confirmButtonWidth/2, confirmButtonY + (confirmButtonHeight - 8)/2, canConfirm ? 0xFFFFFFFF : 0xFFAAAAAA);
            modelContactsConfirmAddRectOnScreen = calculateScreenRectForVirtual(confirmButtonX, confirmButtonY, confirmButtonWidth, confirmButtonHeight, mv, proj, vp, sr);
        } else {
            modelContactsConfirmAddRectOnScreen = null;
        }
    }

    private void renderContactList(ItemStack stack, boolean interactionGuiOpen, FloatBuffer mv, FloatBuffer proj, IntBuffer vp, ScaledResolution sr) {
        FontRenderer fontRenderer = mc.fontRenderer;
        int contactsPageTitleY = -45;
        drawCenteredStringWithShadow(fontRenderer, TextFormatting.GOLD + "" + TextFormatting.BOLD + "СПИСОК КОНТАКТОВ", 80, contactsPageTitleY, 0xFFD700);

        if (interactionGuiOpen) {
            modelContactDeleteButtonRectsOnScreen.clear();
            modelContactDeleteButtonAssociatedName.clear();
        }

        if (mc.player == null) return;

        KPKModelInteractionGui kpkGui = null;
        boolean isContactAddModeActiveForGui = false;
        if (interactionGuiOpen && mc.currentScreen instanceof KPKModelInteractionGui) {
            kpkGui = (KPKModelInteractionGui) mc.currentScreen;
            isContactAddModeActiveForGui = kpkGui.isAddingContact();
        }

        if (isContactAddModeActiveForGui && kpkGui != null) {
            String currentInput = kpkGui.getCurrentContactInput();
            boolean inputActive = kpkGui.isContactInputActive();
            int cursorBlink = kpkGui.getCursorCounter();

            int inputFieldHeight = 18;
            int inputFieldY = 179;
            int inputFieldX = -70;
            int inputFieldWidth = 280;
            int confirmButtonWidth = 20;

            drawStyledInputField(inputFieldX, inputFieldY, inputFieldWidth, inputFieldHeight, inputActive);

            String displayText = currentInput;
            if (inputActive && (cursorBlink / 6) % 2 == 0) displayText += "_";
            fontRenderer.drawStringWithShadow(displayText, inputFieldX + 3, inputFieldY + (inputFieldHeight - fontRenderer.FONT_HEIGHT)/2 + 1, 0xFFFFFFFF);
            modelContactsInputFieldRectOnScreen = calculateScreenRectForVirtual(inputFieldX, inputFieldY, inputFieldWidth, inputFieldHeight, mv, proj, vp, sr);

            int confirmButtonX = inputFieldX + inputFieldWidth + 2;
            drawStyledButton(confirmButtonX, inputFieldY, confirmButtonWidth, inputFieldHeight, COLOR_POSITIVE_TOP, COLOR_POSITIVE_BOTTOM, COLOR_BORDER);
            drawCenteredStringWithShadow(fontRenderer, "+", confirmButtonX + confirmButtonWidth/2, inputFieldY + (inputFieldHeight - fontRenderer.FONT_HEIGHT)/2 + 1, 0xFFFFFFFF);
            modelContactsConfirmAddRectOnScreen = calculateScreenRectForVirtual(confirmButtonX, inputFieldY, confirmButtonWidth, inputFieldHeight, mv, proj, vp, sr);
        } else {
            List<String> playerContacts = ItemKPK.getContacts(stack);
            int contactListStartY = -25;

            if (playerContacts.isEmpty()) {
                fontRenderer.drawStringWithShadow(TextFormatting.GRAY + "(Пусто)", -150, contactListStartY, 0xAAAAAA);
            } else {
                int gridStartX = -157;
                int cellWidth = 148;
                int cellHeight = 22;
                int horizontalSpacing = 10;
                int verticalSpacing = 5;
                int numColumns = 2;

                for (int i = 0; i < Math.min(playerContacts.size(), 10); i++) {
                    int row = i / numColumns;
                    int col = i % numColumns;

                    int cellX = gridStartX + col * (cellWidth + horizontalSpacing);
                    int cellY = contactListStartY + row * (cellHeight + verticalSpacing);
                    String contactName = playerContacts.get(i);
                    int delButtonSize = 10;
                    int delButtonX = cellX + cellWidth - delButtonSize - 2;
                    int delButtonY = cellY + (cellHeight - delButtonSize) / 2;

                    Gui.drawRect(cellX - 1, cellY - 1, cellX + cellWidth + 1, cellY + cellHeight + 1, 0xFFFFFFFF);
                    Gui.drawRect(cellX, cellY, cellX + cellWidth, cellY + cellHeight, 0x99000000);
                    fontRenderer.drawStringWithShadow(contactName, cellX + 4, cellY + (cellHeight - fontRenderer.FONT_HEIGHT) / 2 + 1, 0xFFFFFF);

                    if (interactionGuiOpen) {
                        drawStyledButton(delButtonX, delButtonY, delButtonSize, delButtonSize, COLOR_NEGATIVE_TOP, COLOR_NEGATIVE_BOTTOM, COLOR_BORDER);
                        fontRenderer.drawStringWithShadow("X", delButtonX + 2, delButtonY + 1, 0xFFFFFFFF);
                        modelContactDeleteButtonRectsOnScreen.add(calculateScreenRectForVirtual(delButtonX, delButtonY, delButtonSize, delButtonSize, mv, proj, vp, sr));
                        modelContactDeleteButtonAssociatedName.add(contactName);
                    }
                }
            }

            if (interactionGuiOpen) {
                modelContactsInputFieldRectOnScreen = null;
                modelContactsConfirmAddRectOnScreen = null;
                int addContactButtonWidth = 100;
                int addContactButtonHeight = 18;
                int addContactButtonX = 32;
                int addContactButtonY = 179;

                drawStyledButton(addContactButtonX, addContactButtonY, addContactButtonWidth, addContactButtonHeight, COLOR_INACTIVE_TOP, COLOR_INACTIVE_BOTTOM, COLOR_BORDER);
                drawCenteredStringWithShadow(fontRenderer, "Добавить контакт", addContactButtonX + addContactButtonWidth/2, addContactButtonY + (addContactButtonHeight - fontRenderer.FONT_HEIGHT)/2 + 1, 0xFFFFFFFF);
                modelContactsAddButtonRectOnScreen = calculateScreenRectForVirtual(addContactButtonX, addContactButtonY, addContactButtonWidth, addContactButtonHeight, mv, proj, vp, sr);
            }
        }
    }

    private void drawCenteredStringWithShadow(FontRenderer fontRendererIn, String text, int x, int y, int color) {
        fontRendererIn.drawStringWithShadow(text, (float)(x - fontRendererIn.getStringWidth(text) / 2), (float)y, color);
    }

    private static Rectangle calculateScreenRectForVirtual(float virtualX, float virtualY, float virtualWidth, float virtualHeight,
                                                           FloatBuffer modelView, FloatBuffer projection, IntBuffer viewport, ScaledResolution sr) {
        if (modelView == null || projection == null || viewport == null || sr == null) return null;

        float[] topLeft = projectToScreenSpaceForRect(virtualX, virtualY, 0, modelView, projection, viewport, sr);
        float[] bottomRight = projectToScreenSpaceForRect(virtualX + virtualWidth, virtualY + virtualHeight, 0, modelView, projection, viewport, sr);

        if (topLeft != null && bottomRight != null) {
            if (topLeft[2] < 1.0f && bottomRight[2] < 1.0f) {
                float rX1 = Math.min(topLeft[0], bottomRight[0]);
                float rY1 = Math.min(topLeft[1], bottomRight[1]);
                float rX2 = Math.max(topLeft[0], bottomRight[0]);
                float rY2 = Math.max(topLeft[1], bottomRight[1]);
                return new Rectangle((int)rX1, (int)rY1, (int)(rX2 - rX1), (int)(rY2 - rY1));
            }
        }
        return null;
    }

    private static float[] projectToScreenSpaceForRect(float x, float y, float z,
                                                       FloatBuffer modelViewMatrix, FloatBuffer projectionMatrix, IntBuffer viewport,
                                                       ScaledResolution sr) {
        FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
        if (GLU.gluProject(x, y, z, modelViewMatrix, projectionMatrix, viewport, screenCoords)) {
            float screenX = screenCoords.get(0);
            float screenY = mc.displayHeight - screenCoords.get(1);
            return new float[]{screenX / sr.getScaleFactor(), screenY / sr.getScaleFactor(), screenCoords.get(2)};
        }
        return null;
    }

    private void applyTabletTransform(ItemCameraTransforms.TransformType transformType) {
        switch (transformType) {
            case FIRST_PERSON_RIGHT_HAND:
            case FIRST_PERSON_LEFT_HAND:
                GlStateManager.translate(0.0F, 0.4F, -1F);
                GlStateManager.rotate(180F, 0, 1, 0);
                GlStateManager.rotate(0F, 1, 0, 0);
                GlStateManager.rotate(33F, 0, 0, 1);
                GlStateManager.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
                break;
            case THIRD_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND:
                GlStateManager.translate(1.3F, -0.1F, 0.7F);
                GlStateManager.rotate(90F, 0, 1, 0);
                GlStateManager.rotate(0F, 1, 0, 0);
                GlStateManager.rotate(15f, 0, 0, 1);
                GlStateManager.scale(MODEL_SCALE * 0.7F, MODEL_SCALE * 0.7F, MODEL_SCALE * 0.7F);
                break;
            case GROUND:
                GlStateManager.translate(0.0F, 0.0F, 0.0F);
                GlStateManager.scale(MODEL_SCALE * 0.5F, MODEL_SCALE * 0.5F, MODEL_SCALE * 0.5F);
                break;
            case GUI:
                GlStateManager.translate(0.0F, 0.0F, 0.0F);
                GlStateManager.rotate(30F, 1, 0, 0);
                GlStateManager.rotate(225F, 0, 1, 0);
                GlStateManager.scale(MODEL_SCALE * 1.2F, MODEL_SCALE * 1.2F, MODEL_SCALE * 1.2F);
                break;
            default:
                GlStateManager.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
                break;
        }
    }

    public static void setTransformType(ItemCameraTransforms.TransformType transform) {
        currentTransform = transform;
    }
}