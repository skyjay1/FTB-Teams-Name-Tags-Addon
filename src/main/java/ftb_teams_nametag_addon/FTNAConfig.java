package ftb_teams_nametag_addon;

import net.minecraftforge.common.ForgeConfigSpec;

public class FTNAConfig {

    public final ForgeConfigSpec.IntValue SELECTION_RADIUS;
    public final ForgeConfigSpec.IntValue SHOW_NAME_TAG_DISTANCE;
    public final ForgeConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT;
    public final ForgeConfigSpec.IntValue SHOW_TEAMMATE_DISTANCE;
    public final ForgeConfigSpec.BooleanValue SHOW_INVISIBLE_PLAYERS;
    public final ForgeConfigSpec.BooleanValue SHOW_TEAM_NAME;
    public final ForgeConfigSpec.BooleanValue HIDE_ON_SNEAK;
    public final ForgeConfigSpec.BooleanValue RENDER_FOR_SELF;

    public FTNAConfig(final ForgeConfigSpec.Builder builder) {

        builder.push("options");

        SELECTION_RADIUS = builder
                .comment("The maximum number of blocks from the crosshair to a player to show the name tag")
                .defineInRange("selection_radius", 0, 0, 16);
        SHOW_NAME_TAG_DISTANCE = builder
                .comment("The maximum distance to render player name tags for other teams")
                .defineInRange("show_nametag_distance", 128, 0, 128);
        SHOW_TEAMMATE_DISTANCE = builder
                .comment("The maximum distance to render player name tags for the same team")
                .defineInRange("show_teammate_distance", 128, 0, 256);
        REQUIRE_LINE_OF_SIGHT = builder
                .comment("When true, players can only see name tags for other teams when there are no blocks in the way")
                .define("require_line_of_sight", true);
        SHOW_INVISIBLE_PLAYERS = builder
                .comment("When true, looking directly at an invisible player shows their name tag")
                .define("show_invisible_players", true);
        SHOW_TEAM_NAME = builder
                .comment("When true, the team name is shown beside the player's nametag")
                .define("show_team_name", true);
        HIDE_ON_SNEAK = builder
                .comment("When true, the nametag color bar is hidden when the player is sneaking")
                .define("hide_on_sneak", true);
        RENDER_FOR_SELF = builder
                .comment("When true, the nametag is rendered for the player in their inventory and third person.")
                .define("render_for_self", true);

        builder.pop();
    }

}
