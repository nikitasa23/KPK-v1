package com.example.kpkmod.model;

import com.example.kpkmod.item.ItemKPKRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

@SideOnly(Side.CLIENT)
public class CustomKPKModel implements IBakedModel {
    private final IBakedModel baseModel;
    private final ItemOverrideList overrides;
    private ItemCameraTransforms.TransformType currentTransform = ItemCameraTransforms.TransformType.NONE;

    public CustomKPKModel(IBakedModel baseModel) {
        this.baseModel = baseModel;
        this.overrides = new ItemOverrideList(baseModel.getOverrides().getOverrides()) {
            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack,
                                               @Nullable World world, @Nullable EntityLivingBase entity) {
                return CustomKPKModel.this;
            }
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return baseModel.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return baseModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return currentTransform != ItemCameraTransforms.TransformType.GUI &&
                currentTransform != ItemCameraTransforms.TransformType.FIXED &&
                currentTransform != ItemCameraTransforms.TransformType.NONE;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseModel.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        this.currentTransform = cameraTransformType;
        if (Minecraft.getMinecraft().getRenderItem() != null) {
            ItemKPKRenderer.setTransformType(cameraTransformType);
        }

        return Pair.of(this, baseModel.handlePerspective(cameraTransformType).getRight());
    }
}