package ftb_teams_nametag_addon;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import dev.ftb.mods.ftbteams.data.TeamManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(FtbTeamsNametagAddon.MODID)
public class FtbTeamsNametagAddon {

    public static final String MODID = "ftb_teams_nametag_addon";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final FTNAConfig CONFIG;
    private static final ForgeConfigSpec CONFIG_SPEC;

    static {
        // server config
        Pair<FTNAConfig, ForgeConfigSpec> serverConfig = new ForgeConfigSpec.Builder().configure(FTNAConfig::new);
        CONFIG = serverConfig.getLeft();
        CONFIG_SPEC = serverConfig.getRight();
    }

    public FtbTeamsNametagAddon() {
        // register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC);
        // other events
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FTNAClientEvents.register());
    }

    public static Optional<TeamManager> getTeamManager() {
        if(FTBTeamsAPI.isManagerLoaded()) {
            return Optional.of(FTBTeamsAPI.getManager());
        }
        return Optional.empty();
    }

    public static Optional<ClientTeamManager> getClientTeamManager() {
        if(FTBTeamsAPI.isClientManagerLoaded()) {
            return Optional.of(FTBTeamsAPI.getClientManager());
        }
        return Optional.empty();
    }
}
