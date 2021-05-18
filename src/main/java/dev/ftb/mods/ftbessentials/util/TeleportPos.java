package dev.ftb.mods.ftbessentials.util;

import com.google.gson.JsonObject;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * @author LatvianModder
 */
public class TeleportPos {
	@FunctionalInterface
	public interface TeleportResult {
		TeleportResult SUCCESS = new TeleportResult() {
			@Override
			public int runCommand(ServerPlayer player) {
				return 1;
			}

			@Override
			public boolean isSuccess() {
				return true;
			}
		};

		TeleportResult DIMENSION_NOT_FOUND = player -> {
			player.displayClientMessage(new TextComponent("Dimension not found!"), false);
			return 0;
		};

		int runCommand(ServerPlayer player);

		default boolean isSuccess() {
			return false;
		}
	}

	@FunctionalInterface
	public interface CooldownTeleportResult extends TeleportResult {
		long getCooldown();

		@Override
		default int runCommand(ServerPlayer player) {
			player.displayClientMessage(new TextComponent("Can't teleport yet! Cooldown: " + TimeUtils.prettyTimeString(getCooldown() / 1000L)), false);
			return 0;
		}
	}

	public final ResourceKey<Level> dimension;
	public final BlockPos pos;
	public long time;

	public TeleportPos(ResourceKey<Level> d, BlockPos p) {
		dimension = d;
		pos = p;
		time = System.currentTimeMillis();
	}

	public TeleportPos(Level world, BlockPos p) {
		this(world.dimension(), p);
	}

	public TeleportPos(Entity entity) {
		this(entity.level, entity.blockPosition());
	}

	public TeleportPos(JsonObject json) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(json.get("dim").getAsString()));
		pos = new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
		time = json.get("time").getAsLong();
	}

	public TeleportResult teleport(ServerPlayer player) {
		ServerLevel world = player.server.getLevel(dimension);

		if (world == null) {
			return TeleportResult.DIMENSION_NOT_FOUND;
		}

		int lvl = player.experienceLevel;
		player.teleportTo(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player.yRot, player.xRot);
		player.setExperienceLevels(lvl);
		return TeleportResult.SUCCESS;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("dim", dimension.location().toString());
		json.addProperty("x", pos.getX());
		json.addProperty("y", pos.getY());
		json.addProperty("z", pos.getZ());
		json.addProperty("time", time);
		return json;
	}

	public String distanceString(TeleportPos origin) {
		if (origin.dimension == dimension) {
			double dx = pos.getX() - origin.pos.getX();
			double dz = pos.getZ() - origin.pos.getZ();
			return (int) Math.sqrt(dx * dx + dz * dz) + "m";
		} else {
			ResourceLocation s = dimension.location();

			if (s.getNamespace().equals("minecraft")) {
				switch (s.getPath()) {
					case "overworld":
						return "Overworld";
					case "the_nether":
						return "The Nether";
					case "the_end":
						return "The End";
					default:
						return s.getPath();
				}
			}

			return s.getPath() + " [" + s.getNamespace() + "]";
		}
	}
}
