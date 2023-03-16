package ftb_teams_nametag_addon;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.KnownClientPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class FTNAClientEvents {

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModHandler.class);
        MinecraftForge.EVENT_BUS.register(ForgeHandler.class);
    }

    public static final class ModHandler {

    }

    public static final class ForgeHandler {

        @SubscribeEvent
        public static void onRenderNametag(final RenderNameTagEvent event) {
            // verify non-spectator player
            if(event.getEntity().getType() != EntityType.PLAYER || event.getEntity().isSpectator()) {
                return;
            }
            final Entity target = event.getEntity();
            UUID targetTeam = null;
            // load current player
            final Player player = Minecraft.getInstance().player;
            if(target == player && !FtbTeamsNametagAddon.CONFIG.RENDER_FOR_SELF.get()) return;
            UUID playerTeam = null;
            // check invisibility
            if(!FtbTeamsNametagAddon.CONFIG.SHOW_INVISIBLE_PLAYERS.get() && event.getEntity().isInvisibleTo(player)) {
                return;
            }
            // determine positions
            final Vec3 playerPos = player.getEyePosition();
            final Vec3 targetPos = target.getEyePosition();
            final double distTo = playerPos.distanceTo(targetPos);
            // check FTB team
            final Optional<ClientTeamManager> oManager = FtbTeamsNametagAddon.getClientTeamManager();
            if(oManager.isPresent()) {
                // load entity and player teams
                final KnownClientPlayer knownTarget = oManager.get().getKnownPlayer(target.getUUID());
                final KnownClientPlayer knownPlayer = oManager.get().getKnownPlayer(player.getUUID());
                // determine team IDs
                if(knownTarget != null && knownPlayer != null) {
                    targetTeam = knownTarget.teamId;
                    playerTeam = knownPlayer.teamId;
                    // check for same team
                    if(distTo < FtbTeamsNametagAddon.CONFIG.SHOW_TEAMMATE_DISTANCE.get() && targetTeam.equals(playerTeam)) {
                        // render additional nametag visuals
                        renderNametagExtra(event, oManager, targetTeam);
                        // enable nametag
                        event.setResult(Event.Result.ALLOW);
                        return;
                    }
                }
            }
            // prepare to check line of sight
            final double maxRange = FtbTeamsNametagAddon.CONFIG.SHOW_NAME_TAG_DISTANCE.get();
            final Vec3 playerView = player.getViewVector(1.0F);
            final Vec3 playerLookPos = playerPos.add(playerView.scale(distTo + 1.0D));
            // check entity selected and line of sight
            if(distTo <= maxRange
                    && getEntityHitResult(player, playerPos, playerLookPos, target, maxRange) != null
                    && (!FtbTeamsNametagAddon.CONFIG.REQUIRE_LINE_OF_SIGHT.get() || player.hasLineOfSight(event.getEntity()))) {
                // render additional nametag visuals
                renderNametagExtra(event, oManager, targetTeam);
                // enable nametag
                event.setResult(Event.Result.ALLOW);
                return;
            }
            // all checks failed
            event.setResult(Event.Result.DENY);
        }
    }

    private static void renderNametagExtra(final RenderNameTagEvent event, final Optional<ClientTeamManager> oManager, UUID teamId) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientTeam team;
        if(oManager.isPresent() && teamId != null && (team = oManager.get().getTeam(teamId)) != null) {
            if(FtbTeamsNametagAddon.CONFIG.HIDE_ON_SNEAK.get()){
                if(event.getEntity().isShiftKeyDown()){
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }
            if(FtbTeamsNametagAddon.CONFIG.SHOW_TEAM_NAME.get()){
                MutableComponent name = team.getName().copy().withStyle(s->s.withColor(team.getColor()));
                event.getPoseStack().pushPose();
                event.getPoseStack().translate(0, 0.25f, 0);
                event.setContent(name);
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(event.getEntity()).renderNameTag(event.getEntity(), event.getOriginalContent(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
                event.getPoseStack().popPose();
            }
            // prepare pose stack
            event.getPoseStack().pushPose();
            // these transformations are copied from EntityRenderer#renderNameTag
            event.getPoseStack().translate(0, event.getEntity().getBbHeight() + 0.5D, 0);
            event.getPoseStack().mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
            event.getPoseStack().scale(-0.025F, -0.025F, 0.025F);
            // this transformation moves the line below the name tag
            event.getPoseStack().translate(0, minecraft.font.lineHeight + 2, 0);
            // prepare vertex consumer
            final VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.LINES);
            PoseStack.Pose pose = event.getPoseStack().last();
            Matrix4f matrix4f = pose.pose();
            final float width = 1 + minecraft.font.width(event.getContent()) / 2.0F;
            int color = team.getColor() | 0xFF000000;
            // prepare to draw quad
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            // draw quad
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(matrix4f, -width, 0, 0)
                    .color(color)
                    .endVertex();
            buffer.vertex(matrix4f, width, 0, 0)
                    .color(color)
                    .endVertex();
            buffer.vertex(matrix4f, width, -2, 0)
                    .color(color)
                    .endVertex();
            buffer.vertex(matrix4f, -width, -2, 0)
                    .color(color)
                    .endVertex();
            // finish rendering
            tesselator.end();
            RenderSystem.disableBlend();

            event.getPoseStack().popPose();
        }
    }

    private static final double EPSILON = 1.0E-7D;

    @Nullable
    private static EntityHitResult getEntityHitResult(Entity player, Vec3 start, Vec3 end, Entity target, double maxDis) {
        if(player.level != target.level) {
            return null;
        }
        double maxDisSq = maxDis * maxDis;
        Entity entity = null;
        Vec3 hitVec = null;

        // This code is modified from ProjectileUtil#getEntityHitResult
        AABB aabb = target.getBoundingBox().inflate(FtbTeamsNametagAddon.CONFIG.SELECTION_RADIUS.get());
        Optional<Vec3> oHitVec = aabb.clip(start, end);
        // check start is inside aabb
        if (aabb.contains(start)) {
            if (maxDisSq >= EPSILON) {
                entity = target;
                hitVec = oHitVec.orElse(start);
                maxDisSq = 0.0D;
            }
        } else if (oHitVec.isPresent()) {
            Vec3 vec31 = oHitVec.get();
            double distToSq = start.distanceToSqr(vec31);
            if (distToSq < maxDisSq || maxDisSq < EPSILON) {
                if (target.getRootVehicle() == player.getRootVehicle() && !target.canRiderInteract()) {
                    if (maxDisSq < EPSILON) {
                        entity = target;
                        hitVec = vec31;
                    }
                } else {
                    entity = target;
                    hitVec = vec31;
                    maxDisSq = distToSq;
                }
            }
        }
        // create hit result
        if(entity != null) {
            return new EntityHitResult(entity, hitVec);
        }
        // no result
        return null;
    }
}
