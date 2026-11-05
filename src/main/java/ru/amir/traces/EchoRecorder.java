package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * Keeps a short, bounded server-side record of a player's recent actions and
 * stores a death snapshot for later red-echo playback.
 */
@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EchoRecorder {
    public static final String DEATH_MEMORY_KEY = "traces_death_memory_v1";
    public static final String WORK_MEMORY_KEY = "traces_work_memory_v1";
    private static final int WORK_MEMORY_TICKS = 8 * 60 * 20;
    private static final int MAX_WORK_ACTIONS = 48;

    private static final int RECORDING_TICKS = 10 * 20;
    private static final int FRAME_INTERVAL_TICKS = 1;
    private static final int ENTITY_FRAME_INTERVAL_TICKS = 1;
    private static final int MAX_FRAMES = RECORDING_TICKS / FRAME_INTERVAL_TICKS;
    private static final int MAX_ENTITY_FRAMES = RECORDING_TICKS / ENTITY_FRAME_INTERVAL_TICKS;
    private static final int MAX_ACTIONS = 40;

    private static final Map<UUID, PlayerHistory> HISTORIES = new HashMap<>();

    private EchoRecorder() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        long gameTime = player.level().getGameTime();
        if (gameTime % FRAME_INTERVAL_TICKS != 0L) return;

        PlayerHistory history = historyFor(player);
        appendFrame(history, captureFrame(player, gameTime));
        if (gameTime % ENTITY_FRAME_INTERVAL_TICKS == 0L) {
            captureNearbyLivingEntities(player, history, gameTime);
        }
        prune(history, gameTime);
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) return;
        recordBlockAction(player, level, "break", event.getPos(), event.getState());
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) return;
        recordBlockAction(player, level, "place", event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getCrafting();
        if (stack.isEmpty()) return;

        CompoundTag action = baseAction(player.level().getGameTime(), "craft");
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        action.putString("id", itemId.toString());
        action.putInt("count", stack.getCount());
        putWorkContext(action, player);
        addAction(player, action);
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;

        long gameTime = level.getGameTime();
        PlayerHistory history = historyFor(player);
        appendFrame(history, captureFrame(player, gameTime));

        CompoundTag memory = new CompoundTag();
        memory.putInt("version", 1);
        memory.putLong("captured_at", gameTime);
        memory.putString("dimension", level.dimension().location().toString());
        memory.putDouble("death_x", player.getX());
        memory.putDouble("death_y", player.getY());
        memory.putDouble("death_z", player.getZ());

        DamageSource source = event.getSource();
        memory.putString("cause", source.getMsgId());
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
            attacker = projectile.getOwner();
        }
        if (attacker == null && direct instanceof Projectile projectile) {
            attacker = projectile.getOwner();
        }
        if (attacker != null) {
            memory.putString("attacker_type", BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString());
            memory.putDouble("attacker_x", attacker.getX());
            memory.putDouble("attacker_y", attacker.getY());
            memory.putDouble("attacker_z", attacker.getZ());
            memory.putFloat("attacker_yaw", attacker.getYRot());
            memory.putString("attacker_uuid", attacker.getUUID().toString());
            EntityTrack attackerTrack = history.entityTracks.get(attacker.getUUID());
            if (attackerTrack != null) {
                ListTag attackerFrames = new ListTag();
                for (CompoundTag frame : attackerTrack.frames) attackerFrames.add(frame.copy());
                memory.put("attacker_frames", attackerFrames);
            }
        }
        if (direct != null && direct != attacker) {
            memory.putString("direct_attacker_type",
                    BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()).toString());
        }

        ListTag frames = new ListTag();
        for (CompoundTag frame : history.frames) frames.add(frame.copy());
        ListTag actions = new ListTag();
        for (CompoundTag action : history.actions) actions.add(action.copy());
        memory.put("frames", frames);
        memory.put("actions", actions);

        player.getPersistentData().put(DEATH_MEMORY_KEY, memory);
        if (level.random.nextBoolean()) {
            BlockPos scarGround = BlockPos.containing(player.getX(), player.getY(), player.getZ()).below();
            BlockState ground = level.getBlockState(scarGround);
            if (isNaturalScarGround(ground) && level.getBlockState(scarGround.above()).isAir()) {
                level.setBlock(scarGround, ModBlocks.DEATH_SCAR.get().defaultBlockState(), 3);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getOriginal() instanceof ServerPlayer original)
                || !(event.getEntity() instanceof ServerPlayer replacement)) return;

        CompoundTag oldData = original.getPersistentData();
        if (oldData.contains(DEATH_MEMORY_KEY, Tag.TAG_COMPOUND)) {
            replacement.getPersistentData().put(DEATH_MEMORY_KEY,
                    oldData.getCompound(DEATH_MEMORY_KEY).copy());
        }
        if (oldData.contains(WORK_MEMORY_KEY, Tag.TAG_LIST)) {
            replacement.getPersistentData().put(WORK_MEMORY_KEY,
                    oldData.getList(WORK_MEMORY_KEY, Tag.TAG_COMPOUND).copy());
        }
        HISTORIES.remove(original.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HISTORIES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        HISTORIES.clear();
    }

    /**
     * Returns a defensive copy of the most recent death recording, if present.
     * Playback code should read this from the server-side player.
     */
    public static CompoundTag getDeathMemory(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(DEATH_MEMORY_KEY, Tag.TAG_COMPOUND)
                ? data.getCompound(DEATH_MEMORY_KEY).copy()
                : new CompoundTag();
    }

    /** Clears the saved recording after a future echo has consumed it. */
    public static void clearDeathMemory(ServerPlayer player) {
        player.getPersistentData().remove(DEATH_MEMORY_KEY);
    }

    private static boolean isNaturalScarGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MYCELIUM);
    }

    private static CompoundTag captureFrame(ServerPlayer player, long gameTime) {
        CompoundTag frame = new CompoundTag();
        frame.putLong("time", gameTime);
        frame.putDouble("x", player.getX());
        frame.putDouble("y", player.getY());
        frame.putDouble("z", player.getZ());
        frame.putFloat("yaw", player.getYRot());
        frame.putFloat("pitch", player.getXRot());
        frame.putFloat("head_yaw", player.getYHeadRot());
        frame.putBoolean("on_ground", player.onGround());
        frame.putBoolean("sprinting", player.isSprinting());
        frame.putBoolean("crouching", player.isCrouching());
        putHeldItem(frame, "main_hand", player.getMainHandItem());
        putHeldItem(frame, "off_hand", player.getOffhandItem());
        return frame;
    }

    private static void captureNearbyLivingEntities(ServerPlayer player, PlayerHistory history, long gameTime) {
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(16.0D), entity -> entity != player);
        nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (nearby.size() > 16) nearby = nearby.subList(0, 16);
        for (LivingEntity entity : nearby) {
            UUID uuid = entity.getUUID();
            EntityTrack track = history.entityTracks.computeIfAbsent(uuid,
                    ignored -> new EntityTrack(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
            CompoundTag frame = new CompoundTag();
            frame.putLong("time", gameTime);
            frame.putDouble("x", entity.getX());
            frame.putDouble("y", entity.getY());
            frame.putDouble("z", entity.getZ());
            frame.putFloat("yaw", entity.getYRot());
            frame.putFloat("pitch", entity.getXRot());
            frame.putBoolean("aggressive", entity instanceof Mob mob && mob.isAggressive());
            frame.putBoolean("swinging", entity.swinging);
            frame.putInt("swing_time", entity.swingTime);
            frame.putFloat("attack_anim", entity.attackAnim);
            frame.putBoolean("baby", entity instanceof Mob mob && mob.isBaby());
            putHeldItem(frame, "main_hand", entity.getMainHandItem());
            putHeldItem(frame, "off_hand", entity.getOffhandItem());
            putHeldItem(frame, "armor_head", entity.getItemBySlot(EquipmentSlot.HEAD));
            putHeldItem(frame, "armor_chest", entity.getItemBySlot(EquipmentSlot.CHEST));
            putHeldItem(frame, "armor_legs", entity.getItemBySlot(EquipmentSlot.LEGS));
            putHeldItem(frame, "armor_feet", entity.getItemBySlot(EquipmentSlot.FEET));
            track.frames.addLast(frame);
            while (track.frames.size() > MAX_ENTITY_FRAMES) track.frames.removeFirst();
        }
        if (gameTime % 20L == 0L) {
            long oldestTime = gameTime - RECORDING_TICKS;
            history.entityTracks.values().forEach(track -> {
                while (!track.frames.isEmpty() && track.frames.peekFirst().getLong("time") < oldestTime) {
                    track.frames.removeFirst();
                }
            });
            history.entityTracks.values().removeIf(track -> track.frames.isEmpty());
            while (history.entityTracks.size() > 64) {
                UUID oldest = history.entityTracks.entrySet().stream()
                        .min(Comparator.comparingLong(entry -> entry.getValue().frames.peekLast().getLong("time")))
                        .map(Map.Entry::getKey).orElse(null);
                if (oldest == null) break;
                history.entityTracks.remove(oldest);
            }
        }
    }

    private static void appendFrame(PlayerHistory history, CompoundTag frame) {
        if (!history.frames.isEmpty()
                && history.frames.peekLast().getLong("time") == frame.getLong("time")) {
            history.frames.removeLast();
        }
        history.frames.addLast(frame);
        while (history.frames.size() > MAX_FRAMES) history.frames.removeFirst();
    }

    private static void recordBlockAction(ServerPlayer player, ServerLevel level,
                                          String actionType, BlockPos pos, BlockState state) {
        CompoundTag action = baseAction(level.getGameTime(), actionType);
        action.putInt("x", pos.getX());
        action.putInt("y", pos.getY());
        action.putInt("z", pos.getZ());
        action.putString("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        putWorkContext(action, player);
        addAction(player, action);
    }

    private static void putWorkContext(CompoundTag action, ServerPlayer player) {
        action.putString("dimension", player.level().dimension().location().toString());
        action.putDouble("actor_x", player.getX());
        action.putDouble("actor_y", player.getY());
        action.putDouble("actor_z", player.getZ());
        action.putFloat("yaw", player.getYRot());
        putHeldItem(action, "main_hand", player.getMainHandItem());
        putHeldItem(action, "off_hand", player.getOffhandItem());
    }

    /** Returns recent, bounded work actions used by the green echo director. */
    public static ListTag getRecentWorkActions(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ListTag saved = data.getList(WORK_MEMORY_KEY, Tag.TAG_COMPOUND);
        ListTag retained = new ListTag();
        long oldest = player.level().getGameTime() - WORK_MEMORY_TICKS;
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag action = saved.getCompound(i);
            if (action.getLong("time") >= oldest) retained.add(action.copy());
        }
        while (retained.size() > MAX_WORK_ACTIONS) retained.remove(0);
        data.put(WORK_MEMORY_KEY, retained.copy());
        return retained;
    }

    private static void appendWorkAction(ServerPlayer player, CompoundTag action) {
        ListTag saved = getRecentWorkActions(player);
        saved.add(action.copy());
        while (saved.size() > MAX_WORK_ACTIONS) saved.remove(0);
        player.getPersistentData().put(WORK_MEMORY_KEY, saved);
    }

    private static void putHeldItem(CompoundTag frame, String hand, ItemStack stack) {
        frame.putString(hand, stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static CompoundTag baseAction(long gameTime, String actionType) {
        CompoundTag action = new CompoundTag();
        action.putLong("time", gameTime);
        action.putString("type", actionType);
        return action;
    }

    private static void addAction(ServerPlayer player, CompoundTag action) {
        PlayerHistory history = historyFor(player);
        long gameTime = action.getLong("time");
        prune(history, gameTime);
        history.actions.addLast(action);
        while (history.actions.size() > MAX_ACTIONS) history.actions.removeFirst();
        appendWorkAction(player, action);
    }

    private static PlayerHistory historyFor(ServerPlayer player) {
        return HISTORIES.computeIfAbsent(player.getUUID(), ignored -> new PlayerHistory());
    }

    private static void prune(PlayerHistory history, long gameTime) {
        long oldestTime = gameTime - RECORDING_TICKS;
        while (!history.frames.isEmpty() && history.frames.peekFirst().getLong("time") < oldestTime) {
            history.frames.removeFirst();
        }
        while (!history.actions.isEmpty() && history.actions.peekFirst().getLong("time") < oldestTime) {
            history.actions.removeFirst();
        }
    }

    private static final class PlayerHistory {
        private final Deque<CompoundTag> frames = new ArrayDeque<>();
        private final Deque<CompoundTag> actions = new ArrayDeque<>();
        private final Map<UUID, EntityTrack> entityTracks = new HashMap<>();
    }

    private static final class EntityTrack {
        private final String type;
        private final Deque<CompoundTag> frames = new ArrayDeque<>();
        private EntityTrack(String type) { this.type = type; }
    }
}
