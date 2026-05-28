/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.lgna.project.migration;

import org.lgna.project.Version;

import static org.lgna.project.migration.MigrationManager.NO_REPLACEMENT;
import static org.lgna.project.migration.ProjectMigrationTextSnippets.createMoreSpecificFieldPattern;
import static org.lgna.project.migration.ProjectMigrationTextSnippets.createMoreSpecificFieldReplacement;

// @formatter:off
@Deprecated // Text migrations now load from migrations/text-migrations.json; retained for JSON regeneration and documentation.
final class TextMigrationRegistryV3159 {

  static TextMigration[] create() {
    return new TextMigration[] {
      new TextMigration(
          new Version("3.1.59.0.0"),

          createMoreSpecificFieldPattern("PLANT1", "org.lgna.story.resources.prop.SeaPlantResource"),
          createMoreSpecificFieldReplacement("DOUBLE", "org.lgna.story.resources.prop.SeaSpongeResource"),

          createMoreSpecificFieldPattern("CAULDRON", "org.lgna.story.resources.prop.CauldronResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CauldronResource"),

          createMoreSpecificFieldPattern("YETI", "org.lgna.story.resources.biped.YetiResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.YetiResource"),

          createMoreSpecificFieldPattern("PANDA", "org.lgna.story.resources.biped.PandaResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.PandaResource"),

          createMoreSpecificFieldPattern("WONDERLAND_TREE", "org.lgna.story.resources.prop.WonderlandTreeResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.WonderlandTreeResource"),

          createMoreSpecificFieldPattern("ABYSSINIAN_CAT", "org.lgna.story.resources.quadruped.AbyssinianCatResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.AbyssinianCatResource"),

          createMoreSpecificFieldPattern("SMOOTH", "org.lgna.story.resources.marinemammal.ManateeResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.marinemammal.ManateeResource"),

          createMoreSpecificFieldPattern("MARCH_HARE", "org.lgna.story.resources.biped.MarchHareResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.MarchHareResource"),

          createMoreSpecificFieldPattern("BANANA_TREE", "org.lgna.story.resources.prop.BananaTreeResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.BananaTreeResource"),

          createMoreSpecificFieldPattern("BOULDER1_MOON", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER1_GRAY", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER1_DESERT", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER1_BROWN", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER2_MOON", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER2_GRAY", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER2_DESERT", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER2_BROWN", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER3_MOON", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER3_GRAY", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER3_DESERT", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER3_BROWN", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER4_MOON", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER4_GRAY", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER4_DESERT", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER4_BROWN", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER5_MOON", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER5_GRAY", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER5_DESERT", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER5_BROWN", "org.lgna.story.resources.prop.BoulderResource"),

          "name=\"org.lgna.story.resources.prop.JungleShrubResource",
          "name=\"org.lgna.story.resources.prop.JunglePlantResource",

          createMoreSpecificFieldPattern("CASTLE_GATE", "org.lgna.story.resources.prop.CastleGateResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CastleGateResource"),

          createMoreSpecificFieldPattern("BANANA", "org.lgna.story.resources.prop.BananaResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.BananaResource"),

          createMoreSpecificFieldPattern("WOLF", "org.lgna.story.resources.quadruped.WolfResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.WolfResource"),

          createMoreSpecificFieldPattern("STAFF", "org.lgna.story.resources.prop.StaffResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.StaffResource"),

          "org.lgna.story.resources.prop.LogBridgeResource",
          "org.lgna.story.resources.prop.JungleLogResource",

          createMoreSpecificFieldPattern("DRAGON_BABY_GREEN", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("GREEN", "org.lgna.story.resources.quadruped.BabyDragonResource"),

          createMoreSpecificFieldPattern("DRAGON_BABY_RED", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("RED", "org.lgna.story.resources.quadruped.BabyDragonResource"),

          createMoreSpecificFieldPattern("DRAGON_BABY_AQUA", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("AQUA", "org.lgna.story.resources.quadruped.BabyDragonResource"),

          createMoreSpecificFieldPattern("DRAGON_BABY_BLUE", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("BLUE", "org.lgna.story.resources.quadruped.BabyDragonResource"),

          createMoreSpecificFieldPattern("STONE_BRIDGE", "org.lgna.story.resources.prop.StoneBridgeResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.StoneBridgeResource"),

          createMoreSpecificFieldPattern("WHITE_RABBIT", "org.lgna.story.resources.biped.WhiteRabbitResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.WhiteRabbitResource"),

          createMoreSpecificFieldPattern("BOWLING_PIN", "org.lgna.story.resources.prop.BowlingPinResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.BowlingPinResource"),

          createMoreSpecificFieldPattern("SHRINE", "org.lgna.story.resources.prop.ShrineResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.ShrineResource"),

          createMoreSpecificFieldPattern("SCOTTY_DOG", "org.lgna.story.resources.quadruped.ScottyDogResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.ScottyDogResource"),

          createMoreSpecificFieldPattern("FISHING_BASKET", "org.lgna.story.resources.prop.FishingBasketResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.FishingBasketResource"),

          createMoreSpecificFieldPattern("KITE_SPOOL", "org.lgna.story.resources.prop.KiteSpoolResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.KiteSpoolResource"),

          createMoreSpecificFieldPattern("BABY_YETI", "org.lgna.story.resources.biped.BabyYetiResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.BabyYetiResource"),

          createMoreSpecificFieldPattern("NO_SCARF", "org.lgna.story.resources.biped.BabyYetiResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.BabyYetiResource"),

          createMoreSpecificFieldPattern("BUNNY", "org.lgna.story.resources.biped.BunnyResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.BunnyResource"),

          createMoreSpecificFieldPattern("MAPINGUARI", "org.lgna.story.resources.biped.MapinguariResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.MapinguariResource"),

          createMoreSpecificFieldPattern("HEDGE", "org.lgna.story.resources.prop.HedgeResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.HedgeResource"),

          createMoreSpecificFieldPattern("MANGO", "org.lgna.story.resources.prop.MangoResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.MangoResource"),

          createMoreSpecificFieldPattern("SUBMARINE", "org.lgna.story.resources.prop.SubmarineResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.SubmarineResource"),

          createMoreSpecificFieldPattern("CASTLE_TOWER_MIDDLE", "org.lgna.story.resources.prop.CastleTowerMiddleResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CastleTowerMiddleResource"),

          createMoreSpecificFieldPattern("TREEHOUSE", "org.lgna.story.resources.prop.TreehouseResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TreehouseResource"),

          createMoreSpecificFieldPattern("CASTLE_TOWER_BASE", "org.lgna.story.resources.prop.CastleTowerBaseResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CastleTowerBaseResource"),

          createMoreSpecificFieldPattern("FISHING_BASKET_LID", "org.lgna.story.resources.prop.FishingBasketLidResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.FishingBasketLidResource"),

          createMoreSpecificFieldPattern("PLATEAU1_PLATEAU1_BROWN", "org.lgna.story.resources.prop.PlateauResource"),
          createMoreSpecificFieldReplacement("TALL_BROWN", "org.lgna.story.resources.prop.PlateauResource"),

          createMoreSpecificFieldPattern("PLATEAU1_PLATEAU1_RED", "org.lgna.story.resources.prop.PlateauResource"),
          createMoreSpecificFieldReplacement("TALL_RED", "org.lgna.story.resources.prop.PlateauResource"),

          createMoreSpecificFieldPattern("PLATEAU1_PLATEAU1_GRAY", "org.lgna.story.resources.prop.PlateauResource"),
          createMoreSpecificFieldReplacement("TALL_GRAY", "org.lgna.story.resources.prop.PlateauResource"),

          createMoreSpecificFieldPattern("PLATEAU2_PLATEAU2_BROWN", "org.lgna.story.resources.prop.PlateauResource"),
          createMoreSpecificFieldReplacement("SHORT_BROWN", "org.lgna.story.resources.prop.PlateauResource"),

          createMoreSpecificFieldPattern("PLATEAU2_PLATEAU1_RED", "org.lgna.story.resources.prop.PlateauResource"),
          createMoreSpecificFieldReplacement("SHORT_RED", "org.lgna.story.resources.prop.PlateauResource"),

          createMoreSpecificFieldPattern("PLATEAU2_PLATEAU2_GRAY", "org.lgna.story.resources.prop.PlateauResource"),
          createMoreSpecificFieldReplacement("SHORT_GRAY", "org.lgna.story.resources.prop.PlateauResource"),

          createMoreSpecificFieldPattern("RED_ROVER", "org.lgna.story.resources.prop.RedRoverResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.RedRoverResource"),

          createMoreSpecificFieldPattern("ALIEN_ROBOT", "org.lgna.story.resources.quadruped.AlienRobotResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.AlienRobotResource"),

          createMoreSpecificFieldPattern("SOCCER_BALL", "org.lgna.story.resources.prop.SoccerBallResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.SoccerBallResource"),

          createMoreSpecificFieldPattern("YAK", "org.lgna.story.resources.quadruped.YakResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.YakResource"),

          createMoreSpecificFieldPattern("CAULDRON_LID", "org.lgna.story.resources.prop.CauldronLidResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CauldronLidResource"),

          createMoreSpecificFieldPattern("TEAPOT", "org.lgna.story.resources.prop.TeapotResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TeapotResource"),

          createMoreSpecificFieldPattern("CAIMAN", "org.lgna.story.resources.quadruped.CaimanResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.CaimanResource"),

          createMoreSpecificFieldPattern("QUEEN_OF_HEARTS", "org.lgna.story.resources.biped.QueenOfHeartsResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.QueenOfHeartsResource"),

          createMoreSpecificFieldPattern("CAMEL", "org.lgna.story.resources.quadruped.CamelResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.CamelResource"),

          createMoreSpecificFieldPattern("FALCON", "org.lgna.story.resources.flyer.FalconResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.flyer.FalconResource"),

          createMoreSpecificFieldPattern("PIRANHA", "org.lgna.story.resources.fish.PiranhaResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.fish.PiranhaResource"),

          createMoreSpecificFieldPattern("BAMBOO1", "org.lgna.story.resources.prop.BambooResource"),
          createMoreSpecificFieldReplacement("SHOOT1", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("BAMBOO2", "org.lgna.story.resources.prop.BambooResource"),
          createMoreSpecificFieldReplacement("SHOOT2", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("BAMBOO3", "org.lgna.story.resources.prop.BambooResource"),
          createMoreSpecificFieldReplacement("SHOOT3", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("BAMBOO4", "org.lgna.story.resources.prop.BambooResource"),
          createMoreSpecificFieldReplacement("SHOOT4", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("TORTOISE", "org.lgna.story.resources.biped.TortoiseResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.TortoiseResource"),

          createMoreSpecificFieldPattern("GONG", "org.lgna.story.resources.prop.GongResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.GongResource"),

          createMoreSpecificFieldPattern("CARD03", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("THREE3", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD10", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("TEN10", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("DOLPHIN", "org.lgna.story.resources.marinemammal.DolphinResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.marinemammal.DolphinResource"),

          createMoreSpecificFieldPattern("CAVE", "org.lgna.story.resources.prop.CaveResource"),
          createMoreSpecificFieldReplacement("DEFAULT_UNDERWATER", "org.lgna.story.resources.prop.CaveResource"),

          createMoreSpecificFieldPattern("MAGIC_WAND", "org.lgna.story.resources.prop.MagicWandResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.MagicWandResource"),

          createMoreSpecificFieldPattern("GONG_MALLET", "org.lgna.story.resources.prop.GongMalletResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.GongMalletResource"),

          createMoreSpecificFieldPattern("CLUSTER1", "org.lgna.story.resources.prop.BambooClusterResource"),
          createMoreSpecificFieldReplacement("CLUSTER1", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("CLUSTER2", "org.lgna.story.resources.prop.BambooClusterResource"),
          createMoreSpecificFieldReplacement("CLUSTER2", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("CLUSTER3", "org.lgna.story.resources.prop.BambooClusterResource"),
          createMoreSpecificFieldReplacement("CLUSTER3", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("CLUSTER4", "org.lgna.story.resources.prop.BambooClusterResource"),
          createMoreSpecificFieldReplacement("CLUSTER4", "org.lgna.story.resources.prop.BambooResource"),

          "name=\"org.lgna.story.resources.prop.BambooClusterResource",
          "name=\"org.lgna.story.resources.prop.BambooResource",

          createMoreSpecificFieldPattern("OWL", "org.lgna.story.resources.flyer.OwlResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.flyer.OwlResource"),

          createMoreSpecificFieldPattern("PECCARY", "org.lgna.story.resources.quadruped.PeccaryResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.PeccaryResource"),

          createMoreSpecificFieldPattern("TEA_TRAY", "org.lgna.story.resources.prop.TeaTrayResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TeaTrayResource"),

          createMoreSpecificFieldPattern("POCKET_WATCH", "org.lgna.story.resources.prop.PocketWatchResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.PocketWatchResource"),

          createMoreSpecificFieldPattern("BABY_WALRUS", "org.lgna.story.resources.marinemammal.BabyWalrusResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.marinemammal.BabyWalrusResource"),

          createMoreSpecificFieldPattern("SHELF1", "org.lgna.story.resources.prop.CoralShelfResource"),
          createMoreSpecificFieldReplacement("YELLOW", "org.lgna.story.resources.prop.CoralShelfResource"),

          createMoreSpecificFieldPattern("SHELF2", "org.lgna.story.resources.prop.CoralShelfResource"),
          createMoreSpecificFieldReplacement("YELLOW", "org.lgna.story.resources.prop.CoralShelfResource"),

          createMoreSpecificFieldPattern("ORCA", "org.lgna.story.resources.marinemammal.OrcaResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.marinemammal.OrcaResource"),

          createMoreSpecificFieldPattern("FISHING_LANTERN", "org.lgna.story.resources.prop.FishingLanternResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.FishingLanternResource"),

          createMoreSpecificFieldPattern("TROLL", "org.lgna.story.resources.biped.TrollResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.TrollResource"),

          createMoreSpecificFieldPattern("MAD_HATTER", "org.lgna.story.resources.biped.MadHatterResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.MadHatterResource"),

          createMoreSpecificFieldPattern("CHICKEN", "org.lgna.story.resources.flyer.ChickenResource"),
          createMoreSpecificFieldReplacement("MEAN_CHICKEN", "org.lgna.story.resources.flyer.ChickenResource"),

          createMoreSpecificFieldPattern("ARAPAIMA", "org.lgna.story.resources.fish.ArapaimaResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.fish.ArapaimaResource"),

          createMoreSpecificFieldPattern("POND", "org.lgna.story.resources.prop.PondResource"),
          createMoreSpecificFieldReplacement("LIGHT_BLUE", "org.lgna.story.resources.prop.PondResource"),

          createMoreSpecificFieldPattern("PHOENIX", "org.lgna.story.resources.flyer.PhoenixResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.flyer.PhoenixResource"),

          createMoreSpecificFieldPattern("ICE_MOUNTAIN", "org.lgna.story.resources.prop.IceMountainResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.IceMountainResource"),

          createMoreSpecificFieldPattern("BLUE_TANG", "org.lgna.story.resources.fish.BlueTangResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.fish.BlueTangResource"),

          createMoreSpecificFieldPattern("JAPANESE_CYPRESS", "org.lgna.story.resources.prop.JapaneseCypressResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.JapaneseCypressResource"),

          createMoreSpecificFieldPattern("LIONESS", "org.lgna.story.resources.quadruped.LionessResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.LionessResource"),

          createMoreSpecificFieldPattern("SPELL_BOOK", "org.lgna.story.resources.prop.SpellBookResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.SpellBookResource"),

          createMoreSpecificFieldPattern("WALRUS", "org.lgna.story.resources.marinemammal.WalrusResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.marinemammal.WalrusResource"),

          createMoreSpecificFieldPattern("PIG", "org.lgna.story.resources.biped.PigResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.PigResource"),

          createMoreSpecificFieldPattern("POODLE", "org.lgna.story.resources.quadruped.PoodleResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.PoodleResource"),

          createMoreSpecificFieldPattern("COCONUT", "org.lgna.story.resources.prop.CoconutResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CoconutResource"),

          createMoreSpecificFieldPattern("SHORT_HAIR_CAT", "org.lgna.story.resources.quadruped.ShortHairCatResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.ShortHairCatResource"),

          createMoreSpecificFieldPattern("TOUCAN", "org.lgna.story.resources.flyer.ToucanResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.flyer.ToucanResource"),

          createMoreSpecificFieldPattern("ICE_FLOE", "org.lgna.story.resources.prop.IceFloeResource"),
          createMoreSpecificFieldReplacement("ICE_FLOE1", "org.lgna.story.resources.prop.IceFloeResource"),

          createMoreSpecificFieldPattern("COLA_BOTTLE", "org.lgna.story.resources.prop.ColaBottleResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.ColaBottleResource"),

          createMoreSpecificFieldPattern("WITCH", "org.lgna.story.resources.biped.WitchResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.WitchResource"),

          createMoreSpecificFieldPattern("PRAYER_FLAGS", "org.lgna.story.resources.prop.PrayerFlagsResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.PrayerFlagsResource"),

          createMoreSpecificFieldPattern("CHESHIRE_CAT", "org.lgna.story.resources.biped.CheshireCatResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.CheshireCatResource"),

          createMoreSpecificFieldPattern("BIG_BAD_WOLF", "org.lgna.story.resources.biped.BigBadWolfResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.BigBadWolfResource"),

          createMoreSpecificFieldPattern("SHARK", "org.lgna.story.resources.fish.SharkResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.fish.SharkResource"),

          createMoreSpecificFieldPattern("TREASURE_CHEST", "org.lgna.story.resources.prop.TreasureChestResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TreasureChestResource"),

          createMoreSpecificFieldPattern("CURUPIRA", "org.lgna.story.resources.biped.CurupiraResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.CurupiraResource"),

          createMoreSpecificFieldPattern("FLAMINGO", "org.lgna.story.resources.flyer.FlamingoResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.flyer.FlamingoResource"),

          createMoreSpecificFieldPattern("FISHING_NET", "org.lgna.story.resources.prop.FishingNetResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.FishingNetResource"),

          createMoreSpecificFieldPattern("SAUCER_WHITE_RABBIT", "org.lgna.story.resources.prop.SaucerResource"),
          createMoreSpecificFieldReplacement("WHITE_RABBIT", "org.lgna.story.resources.prop.SaucerResource"),

          createMoreSpecificFieldPattern("SAUCER_QUEEN", "org.lgna.story.resources.prop.SaucerResource"),
          createMoreSpecificFieldReplacement("MARCH_HARE", "org.lgna.story.resources.prop.SaucerResource"),

          createMoreSpecificFieldPattern("SAUCER_CHESHIRE", "org.lgna.story.resources.prop.SaucerResource"),
          createMoreSpecificFieldReplacement("MARCH_HARE", "org.lgna.story.resources.prop.SaucerResource"),

          createMoreSpecificFieldPattern("SAUCER_HATTER", "org.lgna.story.resources.prop.SaucerResource"),
          createMoreSpecificFieldReplacement("MAD_HATTER", "org.lgna.story.resources.prop.SaucerResource"),

          createMoreSpecificFieldPattern("SAUCER_MARCH_HARE", "org.lgna.story.resources.prop.SaucerResource"),
          createMoreSpecificFieldReplacement("MARCH_HARE", "org.lgna.story.resources.prop.SaucerResource"),

          createMoreSpecificFieldPattern("SAUCER_PLAYING_CARD", "org.lgna.story.resources.prop.SaucerResource"),
          createMoreSpecificFieldReplacement("PLAYING_CARD", "org.lgna.story.resources.prop.SaucerResource"),

          createMoreSpecificFieldPattern("TEACUP_CHESHIRE", "org.lgna.story.resources.prop.TeacupResource"),
          createMoreSpecificFieldReplacement("MARCH_HARE", "org.lgna.story.resources.prop.TeacupResource"),

          createMoreSpecificFieldPattern("TEACUP_HATTER", "org.lgna.story.resources.prop.TeacupResource"),
          createMoreSpecificFieldReplacement("MAD_HATTER", "org.lgna.story.resources.prop.TeacupResource"),

          createMoreSpecificFieldPattern("TEACUP_MARCH_HARE", "org.lgna.story.resources.prop.TeacupResource"),
          createMoreSpecificFieldReplacement("MARCH_HARE", "org.lgna.story.resources.prop.TeacupResource"),

          createMoreSpecificFieldPattern("TEACUP_PLAYING_CARD", "org.lgna.story.resources.prop.TeacupResource"),
          createMoreSpecificFieldReplacement("PLAYING_CARD", "org.lgna.story.resources.prop.TeacupResource"),

          createMoreSpecificFieldPattern("TEACUP_WHITE_RABBIT", "org.lgna.story.resources.prop.TeacupResource"),
          createMoreSpecificFieldReplacement("WHITE_RABBIT", "org.lgna.story.resources.prop.TeacupResource"),

          createMoreSpecificFieldPattern("TEACUP_QUEEN", "org.lgna.story.resources.prop.TeacupResource"),
          createMoreSpecificFieldReplacement("MARCH_HARE", "org.lgna.story.resources.prop.TeacupResource"),

          createMoreSpecificFieldPattern("CLOWN_FISH", "org.lgna.story.resources.fish.ClownFishResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.fish.ClownFishResource"),

          createMoreSpecificFieldPattern("TENT", "org.lgna.story.resources.prop.TentResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TentResource"),

          createMoreSpecificFieldPattern("ICEBERG", "org.lgna.story.resources.prop.IcebergResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.IcebergResource"),

          createMoreSpecificFieldPattern("MONKEY_KING", "org.lgna.story.resources.biped.MonkeyKingResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.MonkeyKingResource"),

          createMoreSpecificFieldPattern("PAJAMA_FISH", "org.lgna.story.resources.fish.PajamaFishResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.fish.PajamaFishResource"),

          createMoreSpecificFieldPattern("SEAGULL", "org.lgna.story.resources.flyer.SeagullResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.flyer.SeagullResource"),

          createMoreSpecificFieldPattern("CASTLE_TOWER_TOP", "org.lgna.story.resources.prop.CastleTowerTopResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CastleTowerTopResource"),

          createMoreSpecificFieldPattern("MANX_CAT", "org.lgna.story.resources.quadruped.ManxCatResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.ManxCatResource"),

          createMoreSpecificFieldPattern("FISHING_BOAT", "org.lgna.story.resources.prop.FishingBoatResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.FishingBoatResource"),

          createMoreSpecificFieldPattern("FOX", "org.lgna.story.resources.quadruped.FoxResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.FoxResource"),

          createMoreSpecificFieldPattern("MAGIC_STAFF", "org.lgna.story.resources.prop.MagicStaffResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.MagicStaffResource"),

          createMoreSpecificFieldPattern("FISHING_LANTERN_POLE", "org.lgna.story.resources.prop.FishingLanternPoleResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.FishingLanternPoleResource"),

          createMoreSpecificFieldPattern("DALMATIAN", "org.lgna.story.resources.quadruped.DalmatianResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.DalmatianResource"),

          createMoreSpecificFieldPattern("STUFFED_TIGER", "org.lgna.story.resources.biped.StuffedTigerResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.StuffedTigerResource"),

          createMoreSpecificFieldPattern("MAGIC_SPOON", "org.lgna.story.resources.prop.MagicSpoonResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.MagicSpoonResource"),

          createMoreSpecificFieldPattern("FISHING_BOAT_CANOPY", "org.lgna.story.resources.prop.FishingBoatCanopyResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.FishingBoatCanopyResource"),

          createMoreSpecificFieldPattern("SHRINE_LANTERN", "org.lgna.story.resources.prop.ShrineLanternResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.ShrineLanternResource"),

          createMoreSpecificFieldPattern("PIRATE_SHIP", "org.lgna.story.resources.prop.PirateShipResource"),
          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.PirateShipPropResource"),

          createMoreSpecificFieldPattern("TEA_TABLE", "org.lgna.story.resources.prop.TeaTableResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TeaTableResource"),

          createMoreSpecificFieldPattern("WALL", "org.lgna.story.resources.prop.CastleWallResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.CastleWallResource"),

          createMoreSpecificFieldPattern("RED", "org.lgna.story.resources.prop.TallMushroomResource"),
          createMoreSpecificFieldReplacement("TALL_RED", "org.lgna.story.resources.prop.MushroomResource"),

          createMoreSpecificFieldPattern("WHITE", "org.lgna.story.resources.prop.TallMushroomResource"),
          createMoreSpecificFieldReplacement("TALL_WHITE", "org.lgna.story.resources.prop.MushroomResource"),

          "name=\"org.lgna.story.resources.prop.TallMushroomResource",
          "name=\"org.lgna.story.resources.prop.MushroomResource",

          createMoreSpecificFieldPattern("TREE_TRUNK", "org.lgna.story.resources.prop.TreeTrunkResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TreeTrunkResource"),

          createMoreSpecificFieldPattern("DRAGON_ORANGE", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("ORANGE", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("DRAGON_RED", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("RED", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("ADIRONDACK_CHAIR_LIVING_ADIRONDACK_CUSHION_GRAY", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ADIRONDACK_GRAY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ADIRONDACK_CHAIR_LIVING_ADIRONDACK_CUSHION_CAMO", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ADIRONDACK_CAMOUFLAGE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ADIRONDACK_CHAIR_LIVING_ADIRONDACK_CUSHION_PALM", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ADIRONDACK_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_LOVESEAT_ART_NOUVEAU_FRAME_MOHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_LOVESEAT_ART_NOUVEAU_FRAME_OAK", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_OAK", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_WOOD_LIGHT_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_LIGHT_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_CUSHION_BEIGE_FABRIC", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_WHITE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_BEIGE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TAN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("PARK_BENCH_LOVESEAT_PARK_BENCH_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("PARK_BENCH_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("PARK_BENCH_LOVESEAT_PARK_BENCH_OAKGREEN", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("PARK_BENCH_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("PARK_BENCH_LOVESEAT_PARK_BENCH_OAKBLUE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("PARK_BENCH_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_WHITE_FLOWERS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_GREEN_FLOWERS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_BEIGE_FLOWERS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("QUAINT_BROWN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("VALUE_LOVESEAT_VALUE_BLUE_STRIPE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("VALUE_BLUE_STRIPES", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("VALUE1_SOFA_VALUE1_BLUE_STRIPE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE1_BLUE_STRIPES", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("VALUE1_SOFA_VALUE1_FLOWER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE1_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("VALUE2_SOFA_VALUE2_LIGHT_BROWN_FLOWER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE2_BROWN_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("VALUE2_SOFA_VALUE2_RED_CHECKER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE2_RED_SQUARES", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("VALUE2_SOFA_VALUE2_GREEN_FLOWER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE2_GREEN_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("VALUE2_SOFA_VALUE2_BLUE_FLOWER_BORDER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE2_BLUE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL1_SOFA_COLONIAL1_FRUITS", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL1_FRUITS", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL1_SOFA_COLONIAL1_DIAMOND", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL1_DIAMONDS", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL1_SOFA_COLONIAL1_REDPATTERN", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL1_RED_SQUARES", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL1_SOFA_COLONIAL1_BEIGE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL1_BROWN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL1_SOFA_COLONIAL1_WHITE_DIAMOND", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL1_DIAMONDS", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL1_SOFA_COLONIAL1_LINE_CURVES", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL1_CURVES", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL2_SOFA_COLONIAL2_GOLDDIAMOND", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL2_GOLD", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL2_SOFA_COLONIAL2_ORANGE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL2_ORANGE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL2_SOFA_COLONIAL2_RED_STRIPES", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL2_RED", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL2_SOFA_COLONIAL2_BLUE_PATTERN", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL2_BLUE_PATTERN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("STEEL_FRAME_SOFA_MODERN_STEEL_FRAME_FABRIC_BLACKLEATHER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("STEEL_FRAME_BLACK_LEATHER", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("STEEL_FRAME_SOFA_MODERN_STEEL_FRAME_FABRIC_CORDOROY", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("STEEL_FRAME_BRWON_LEATHER", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("STEEL_FRAME_SOFA_MODERN_STEEL_FRAME_FABRIC_STRIPE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("STEEL_FRAME_BRWON_LEATHER", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("STEEL_FRAME_SOFA_MODERN_STEEL_FRAME_FABRIC_GATOR", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("STEEL_FRAME_ALLIGATOR", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_RED", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_RED", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_GREEN", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_GREEN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_BEIGE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_LIGHT_BLUE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_WHITE_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_GREEN_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_BLUE_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_PINK_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("QUAINT_PINK", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_CUTOUT_SOFA_UM_CUTOUT_BLACK_CREAM", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_CUTOUT_BLACK", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_CUTOUT_SOFA_UM_CUTOUT_BLUE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_CUTOUT_BLUE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_CUTOUT_SOFA_UM_CUTOUT_LEOPARD", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_CUTOUT_LEOPARD", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_CUTOUT_SOFA_UM_CUTOUT_PURPLE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_CUTOUT_PURPLE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_CUTOUT_SOFA_UM_CUTOUT_ZEBRA", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_CUTOUT_ZEBRA", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_CUTOUT_SOFA_UM_CUTOUT_GREEN", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_CUTOUT_GREEN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_DIAMOND_SOFA_U_M_DIAMOND_CHECK", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_DIAMOND_BLUE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_DIAMOND_SOFA_U_M_DIAMOND_BLACK", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_DIAMOND_BLACK_AND_WHITE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_DIAMOND_SOFA_U_M_DIAMOND_YELLOW", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_DIAMOND_GREEN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_DIAMOND_SOFA_U_M_DIAMOND_PURPLE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_DIAMOND_PURPLE_AND_GREEN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_DIAMOND_SOFA_U_M_DIAMOND_RED", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_DIAMOND_RED_AND_PURPLE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DRESSER_CENTRAL_ASIAN_GREEN_FLOWERS", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_GREEN_FLOWERS", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DRESSER_CENTRAL_ASIAN_RED_FLOWERS", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_RED_FLOWERS", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DRESSER_CENTRAL_ASIAN_GREEN", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_GREEN", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DRESSER_CENTRAL_ASIAN_RED", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_RED", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("COLONIAL_DRESSER_COLONIAL_WOOD", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("COLONIAL_WOOD", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("COLONIAL_DRESSER_COLONIAL_LIGHT_WOOD_CURLY", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("COLONIAL_LIGHT_WOOD", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("COLONIAL_DRESSER_COLONIAL_RED_WOOD", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("COLONIAL_REDWOOD", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("COLONIAL_DRESSER_COLONIAL_WOOD_STRAIGHT_DARK", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("COLONIAL_DARK_WOOD", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("DESIGNER_DRESSER_DESIGNER_BROWN", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("DESIGNER_BROWN", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("DESIGNER_DRESSER_DESIGNER_LIGHT_WOOD", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("DESIGNER_LIGHT_WOOD", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("DESIGNER_DRESSER_DESIGNER_BLACK", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("DESIGNER_BLACK", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("DESIGNER_DRESSER_DESIGNER_BLUE", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("DESIGNER_BLUE", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("JAPANESE_DRESSER_JAPANESE_TANSU_NORMAL", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("JAPANESE_WOOD", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("JAPANESE_DRESSER_JAPANESE_TANSU_BLACK", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("JAPANESE_BLACK", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("JAPANESE_DRESSER_JAPANESE_TANSU_RED", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("JAPANESE_RED", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_REFLECT_CHINESE_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_REFLECT_CHINESE_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_REFLECT_CHINESE_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_REFLECT_CHINESE_DARK", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_DARK_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_ASIAN_WOOD_CHINESE_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_ASIAN_WOOD_CHINESE_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_ASIAN_WOOD_CHINESE_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_COFFEE_CENTRAL_ASIAN_ASIAN_WOOD_CHINESE_DARK", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_DARK_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_WHITEOAK", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_OAK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIAL_BIRDSRED", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_CURLY_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_MAHOG", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_MAHOGANY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_GUMWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_GREEN", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_REDASH", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_BLEACHEDOAK", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_WHITE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LARGE_CLUB_TABLE_COFFEE_CLUB_RECTANGLE_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LARGE_CLUB_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LARGE_CLUB_TABLE_COFFEE_CLUB_RECTANGLE_BRIDS_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LARGE_CLUB_CURLY_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LARGE_CLUB_TABLE_COFFEE_CLUB_RECTANGLE_BLEACHED_OAK", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LARGE_CLUB_WHITE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LARGE_CLUB_TABLE_COFFEE_CLUB_RECTANGLE_MAHOG", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LARGE_CLUB_MAHOGANY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LARGE_CLUB_TABLE_COFFEE_CLUB_RECTANGLE_RED_ASH", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LARGE_CLUB_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LARGE_CLUB_TABLE_COFFEE_CLUB_RECTANGLE_WHITE_OAK", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LARGE_CLUB_OAK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LARGE_CLUB_TABLE_COFFEE_CLUB_RECTANGLE_LTBLUE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LARGE_CLUB_BLUE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_COFFEE_COLONIAL_GOLDFLORAL", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_GOLD_FLORAL", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_COFFEE_COLONIAL_PAONAZZETTO", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_COFFEE_COLONIAL_PERLINO", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_PINK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_COFFEE_COLONIAL_WHITEMARBLE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_WHITE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("DESIGNER_TABLE_COFFEE_END_DESIGNER_WHITE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("DESIGNER_WHITE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("DESIGNER_TABLE_COFFEE_END_DESIGNER_WALNUT", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("DESIGNER_WALNUT", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LOFT_TABLE_COFFEE_LOFT_SHEEN", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LOFT_CONCRETE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LOFT_TABLE_COFFEE_LOFT_CONCRETE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LOFT_CONCRETE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LOFT_TABLE_COFFEE_LOFT_PATINA", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LOFT_PATINA", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_TOP_TABLE_STAR", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_STARS_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_TOP_TABLE_ALADDIN", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_YELLOW_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_TOP_TABLE_DETAIL", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_FANCY_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_TOP_TABLE_TILE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TILE_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_WOODS_MAHOGNY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_MAHOGANY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_WOODS_YELLOWASPEN", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_YELLOW", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_WOODS_BLACK_LAQUER", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_BLACK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_COFFEE_QUAINT_BLUE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_COFFEE_QUAINT_GREEN", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_COFFEE_QUAINT_WHITE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SPINDLE_TABLE_COFFEE_SPINDLE_WOOD_PAINTED", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SPINDLE_PAINTED", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SPINDLE_TABLE_COFFEE_SPINDLE_WOOD_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SPINDLE_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DESK_CENTRAL_ASIAN_BLACK", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_BLACK", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DESK_CENTRAL_ASIAN_WALNUT", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_WALNUT", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("CLUB_DESK_CLUB_DARKWOOD", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("CLUB_DARK_WOOD", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("QUAINT_DESK_QUAINT_GREEN", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("QUAINT_DESK_QUAINT_WHITE", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("QUAINT_DESK_QUAINT_BLUE", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("VALUE_DESK_VALUE_WOODWHITE", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("VALUE_LIGHT_WOOD", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("VALUE_DESK_VALUE_WOODRED", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("VALUE_RED", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("ACCESSORY_LUGGAGE_SURFACE", "org.lgna.story.resources.prop.SuitcaseResource"),
          createMoreSpecificFieldReplacement("SUITCASE", "org.lgna.story.resources.prop.SuitcaseResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_NEDAR", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_LIGHT_WOOD", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_OAK", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_OAK", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_SEDAR", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_WOOD", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_DINING_MOROCCAN_TURQ", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TURQUOISE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_DINING_MOROCCAN_BLUE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_BLUE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_DINING_MOROCCAN_BLUE_LIGHT", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_LIGHT_BLUE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_TABLE_DINING_ORIENTAL_DRAGON_BROWN", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_DRAGON_BROWN", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_TABLE_DINING_ORIENTAL_DRAGON_RED", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_DRAGON_RED", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_TABLE_DINING_ORIENTAL_LOTUS_BLACK", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_LOTUS_BLACK", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_TABLE_DINING_ORIENTAL_LOTUS_ORANGE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_LOTUS_ORANGE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("OUTDOOR_TABLE_DINING_OUTDOOR_WOOD_ASH", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("OUTDOOR_ASH", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("OUTDOOR_TABLE_DINING_OUTDOOR_WOOD_REDOAK", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("OUTDOOR_OAK", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("OUTDOOR_TABLE_DINING_OUTDOOR_WOOD_REDWOOD", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("OUTDOOR_RED", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("OUTDOOR_TABLE_DINING_OUTDOOR_WOOD_WHITE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("OUTDOOR_WHITE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_DINING_QUAINT_RED", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_RED", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_DINING_QUAINT_GREEN", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_DINING_QUAINT_WHITE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_DINING_QUAINT_BLUE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_CURLY_REDWOOD", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("COLONIAL_REDWOOD", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("COLONIAL_QUILTED_DARK_WOOD", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("COLONIAL_DARK_WOOD", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("COLONIAL_CURLY_LIGHT_WOOD", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("COLONIAL_LIGHT_WOOD", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("BIRTHDAY_CAKE_MATERIALS", "org.lgna.story.resources.prop.CakeResource"),
          createMoreSpecificFieldReplacement("BIRTHDAY", "org.lgna.story.resources.prop.CakeResource"),

          createMoreSpecificFieldPattern("TRASHCAN_INDOOR_VALUE_CLEAN", "org.lgna.story.resources.prop.TrashcanResource"),
          createMoreSpecificFieldReplacement("TRASHCAN", "org.lgna.story.resources.prop.TrashcanResource"),

          createMoreSpecificFieldPattern("VEHICLE_HELICOPTER", "org.lgna.story.resources.prop.HelicopterResource"),
          createMoreSpecificFieldReplacement("HELICOPTER", "org.lgna.story.resources.prop.HelicopterResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_CHAIR_DINING_ART_NOUVEAU_LIGHT_CLEAN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("CLUB_CHAIR_DINING_CLUB_RED_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("CLUB_DARK_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("CLUB_CHAIR_DINING_CLUB_GREENLEATH", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("CLUB_DARK_GREEN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("CLUB_CHAIR_DINING_CLUB_LTGREENLEATH", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("CLUB_DARK_GREEN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("COLONIAL_CHAIR_DINING_COLONIAL1_GOLD_PATTERN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("COLONIAL_GOLD_PATTERN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("COLONIAL_CHAIR_DINING_COLONIAL1_STRIPES", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("COLONIAL_RED_STRIPES", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("COLONIAL_CHAIR_DINING_COLONIAL1_BLUEPATTERN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("COLONIAL_BLUE_PATTERN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("COLONIAL_CHAIR_DINING_COLONIAL1_DIAMONDS", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("COLONIAL_DIAMONDS", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("FANCY_COLONIAL_CHAIR_DINING_COLONIAL2_SRIPE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("FANCY_COLONIAL_RED_STRIPES", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("FANCY_COLONIAL_CHAIR_DINING_COLONIAL2_GOLDPATTERN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("FANCY_COLONIAL_GOLD_PATTERN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("FANCY_COLONIAL_CHAIR_DINING_COLONIAL2_BLUEPATTERN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("FANCY_COLONIAL_GOLD_PATTERN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("FANCY_COLONIAL_CHAIR_DINING_COLONIAL2_BLUESILK", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("FANCY_COLONIAL_RED_STRIPES", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("FANCY_COLONIAL_CHAIR_DINING_COLONIAL2_DIAMONDPATTERN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("FANCY_COLONIAL_GOLD_PATTERN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("PARK_LOVESEAT_PARK_BENCH_OAK", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("PARK_OAK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("PARK_LOVESEAT_PARK_BENCH_WALNUT", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("PARK_WALNUT", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("PARK_LOVESEAT_PARK_BENCH_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("PARK_RED", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("PARK_LOVESEAT_PARK_BENCH_OAKBLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("PARK_BLUE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("PARK_LOVESEAT_PARK_BENCH_IVORY", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("PARK_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("PARK_LOVESEAT_PARK_BENCH_CHESTNUT", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("PARK_CHESTNUT", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("DANISH_MODERN_CHAIR_DINING_DANISH_MODERN_CUSHIONS_GREEN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("DANISH_MODERN_GREEN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("DANISH_MODERN_CHAIR_DINING_DANISH_MODERN_CUSHIONS_BABY_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("DANISH_MODERN_BLUE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("DANISH_MODERN_CHAIR_DINING_DANISH_MODERN_CUSHIONS_WHITE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("DANISH_MODERN_WHITE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_SEAT_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_BLUE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_FORK_BASE_WOOD_LIGHT", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_FORK_BASE_WOOD_ORANGE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_ORANGE_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_FORK_BASE_WOOD_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_RED_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_SEAT_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_BLUE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_FORK_BASE_WOOD_LIGHT", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_FORK_BASE_WOOD_ORANGE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_ORANGE_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_FORK_BASE_WOOD_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_RED_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_SEAT_GRAY", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_GRAY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_SEAT_TEAL", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_TEAL", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_SEAT_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_BLUE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MOROCCAN_CHAIR_DINING_MOROCCAN_SURFACES_BLUE_ORANGE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_YELLOW", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MOROCCAN_CHAIR_DINING_MOROCCAN_SURFACES_RED_CIRCLES", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_RED", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MOROCCAN_CHAIR_DINING_MOROCCAN_SURFACES_RED_TAN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_WHITE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MOROCCAN_CHAIR_DINING_MOROCCAN_SURFACES_BLUE_STRIPES", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_BLUE_STRIPES", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("ORIENTAL_CHAIR_DINING_ORIENTAL_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("ORIENTAL_CHAIR_DINING_ORIENTAL_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("ORIENTAL_CHAIR_DINING_ORIENTAL_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_RED", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("ORIENTAL_CHAIR_DINING_ORIENTAL_ORANGE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_ORANGE_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_LOFT_BOOKCASE_BRUSHED", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("LOFT_METAL", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_BOOKCASE_ART_NOUVEAU_SURFACE", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CHEAP_BOOKCASE_CHEAP_OAK", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CHEAP_OAK", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CHEAP_BOOKCASE_CHEAP_MAHOGANY", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CHEAP_MAHOGANY", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CHEAP_BOOKCASE_CHEAP_PINE", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CHEAP_PINE", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CHEAP_BOOKCASE_CHEAP_BLACK_WASH", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CHEAP_BLACK", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CINDER_BLOCK_BOOKCASE_CINDERBLOCK_SHELVES_BLACKWASH", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CINDER_BLOCK_BLACK", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CINDER_BLOCK_BOOKCASE_CINDERBLOCK_SHELVES_OLDWOOD", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CINDER_BLOCK_PLANK", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("COLONIAL_BOOKCASE_COLONIAL_REDWOODCURLY", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("COLONIAL_REDWOOD", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("COLONIAL_BOOKCASE_COLONIAL_BROWNWOODCURLY", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("COLONIAL_DARK_WOOD", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("COLONIAL_BOOKCASE_COLONIAL_DARK_BROWN_WOODCURLY", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("COLONIAL_WOOD", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("VALUE_BOOKCASE_VALUE_PRESSEDPINE", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("VALUE_DARK_PINE", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("VALUE_BOOKCASE_VALUE_PINE", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("VALUE_PINE", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_ORANGESHADEON", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_ORANGE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_BOLLARD_LIGHTING_FLOOR_GARDEN_TIER_GREEN", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_BOLLARD_GREEN_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_TIER_LIGHTING_FLOOR_GARDEN_TIER_GREEN", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_TIER_GREEN_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_YELLOW_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_YELLOW_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_BLUE_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_BLUE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_GREEN_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_GREEN_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_BLUE_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_BLUE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_GOLD_BLUE_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_GOLD_BLUE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_ORANGE_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_ORANGE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_RED_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_RED_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_ORANGE_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_ORANGE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_PINK_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_PINK_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_PINK", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_PINK_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_YELLOW", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_YELLOW_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_GREEN", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("STUDIO_LIGHTING_FLOOR_STUDIO_LIGHTS_LIGHTS_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("STUDIO_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("VALUE_LIGHTING_FLOOR_VALUE_PAINTED_METAL_BLACKPAINT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("VALUE_BLACK", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("VALUE_LIGHTING_FLOOR_VALUE_PAINTED_METAL_REDPAINT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("VALUE_RED", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("VALUE_LIGHTING_FLOOR_VALUE_PAINTED_METAL_TANPAINT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("VALUE_TAN", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("VALUE_LIGHTING_FLOOR_VALUE_PAINTED_METAL_GREEN_PAINT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("VALUE_GREEN", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DARK_GREEN", "org.lgna.story.resources.prop.BambooThicketResource"),
          createMoreSpecificFieldReplacement("THICKET_DARK_GREEN", "org.lgna.story.resources.prop.BambooResource"),

          createMoreSpecificFieldPattern("LIGHT_GREEN", "org.lgna.story.resources.prop.BambooThicketResource"),
          createMoreSpecificFieldReplacement("THICKET_LIGHT_GREEN", "org.lgna.story.resources.prop.BambooResource"),

          "name=\"org.lgna.story.resources.prop.BambooThicketResource",
          "name=\"org.lgna.story.resources.prop.BambooResource",

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_WOOD_BLOND_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_WOOD_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_WOOD_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_WOOD_RED_LACQUER", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_RED_LAQUER", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_TABLE_TOP_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_TOP_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_TABLE_TOP_BLOND_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_TOP_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_TABLE_TOP_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_TOP_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_TABLE_TOP_RED_LACQUER", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_TOP_RED_LAQUER", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIAL_BIRDSRED", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_CURLY_REDWOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_MAHOG", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_MAHOGANY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_GUMWOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_GREEN", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_REDASH", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_REDWOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_BLEACHEDOAK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_WHITE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_END_COLONIAL2_TABLE_LIGHTWOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_LIGHT_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_END_COLONIAL2_TABLE_REDWOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_REDWOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_END_COLONIAL2_TABLE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("COLONIAL_TABLE_END_COLONIAL2_TABLE_DARKWOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("COLONIAL_DARK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("OCTAGONAL_TABLE_END_OCTAGONAL_WHITE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("OCTAGONAL_WHITE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("OCTAGONAL_TABLE_END_OCTAGONAL_DARK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("OCTAGONAL_DARK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("OCTAGONAL_TABLE_END_OCTAGONAL_YELLOW", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("OCTAGONAL_YELLOW", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("OCTAGONAL_TABLE_END_OCTAGONAL_GREEN", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("OCTAGONAL_GREEN", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_END_QUAINT_FABRIC_GREEN", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("UM_TABLE_END_UM_PURPLE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("UM_PURPLE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("UM_TABLE_END_UM_GREEN", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("UM_GREEN", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("UM_TABLE_END_UM_BLACK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("UM_BLACK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("UM_TABLE_END_UM_YELLOW", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("UM_YELLOW", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("UM_TABLE_END_UM_WHITE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("UM_WHITE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("UM_TABLE_END_UM_ORANGE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("UM_ORANGE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("VOLLEYBALL_LEATHER", "org.lgna.story.resources.prop.VolleyballResource"),
          createMoreSpecificFieldReplacement("VOLLEYBALL", "org.lgna.story.resources.prop.VolleyballResource"),

          createMoreSpecificFieldPattern("CAKE_WEDDING_FROSTING", "org.lgna.story.resources.prop.WeddingCakeResource"),
          createMoreSpecificFieldReplacement("WEDDING", "org.lgna.story.resources.prop.CakeResource"),

          createMoreSpecificFieldPattern("PLANT2", "org.lgna.story.resources.prop.SeaPlantResource"),
          createMoreSpecificFieldReplacement("SHORT", "org.lgna.story.resources.prop.SeaSpongeResource"),

          createMoreSpecificFieldPattern("PLANT3", "org.lgna.story.resources.prop.SeaPlantResource"),
          createMoreSpecificFieldReplacement("TALL", "org.lgna.story.resources.prop.SeaSpongeResource"),

          "name=\"org.lgna.story.resources.prop.SeaPlantResource",
          "name=\"org.lgna.story.resources.prop.SeaSpongeResource",

          createMoreSpecificFieldPattern("KITE", "org.lgna.story.resources.prop.KiteResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.KiteResource"),

          createMoreSpecificFieldPattern("BOULDER1_MARS", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER1_RED", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER2_MARS", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER2_RED", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER3_MARS", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER3_RED", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER4_MARS", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER4_RED", "org.lgna.story.resources.prop.BoulderResource"),

          createMoreSpecificFieldPattern("BOULDER5_MARS", "org.lgna.story.resources.prop.BoulderResource"),
          createMoreSpecificFieldReplacement("BOULDER5_RED", "org.lgna.story.resources.prop.BoulderResource"),

          "name=\"org.lgna.story.resources.prop.JungleShrubResource",
          "name=\"org.lgna.story.resources.prop.JunglePlantResource",

          createMoreSpecificFieldPattern("DRAGON_BABY", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("PINK", "org.lgna.story.resources.quadruped.BabyDragonResource"),

          createMoreSpecificFieldPattern("HARE", "org.lgna.story.resources.biped.HareResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.HareResource"),

          createMoreSpecificFieldPattern("RED", "org.lgna.story.resources.prop.ShortMushroomResource"),
          createMoreSpecificFieldReplacement("SHORT_RED", "org.lgna.story.resources.prop.MushroomResource"),

          createMoreSpecificFieldPattern("WHITE", "org.lgna.story.resources.prop.ShortMushroomResource"),
          createMoreSpecificFieldReplacement("SHORT_WHITE", "org.lgna.story.resources.prop.MushroomResource"),

          "name=\"org.lgna.story.resources.prop.ShortMushroomResource",
          "name=\"org.lgna.story.resources.prop.MushroomResource",

          createMoreSpecificFieldPattern("CARD01", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("ONE1", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD02", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("TWO2", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD04", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("FOUR4", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD05", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("FIVE5", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD06", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("SIX6", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD07", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("SEVEN7", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD08", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("EIGHT8", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("CARD09", "org.lgna.story.resources.biped.PlayingCardResource"),
          createMoreSpecificFieldReplacement("NINE9", "org.lgna.story.resources.biped.PlayingCardResource"),

          createMoreSpecificFieldPattern("MANGO_TREE", "org.lgna.story.resources.prop.MangoTreeResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.MangoTreeResource"),

          createMoreSpecificFieldPattern("ALIEN", "org.lgna.story.resources.biped.AlienResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.AlienResource"),

          createMoreSpecificFieldPattern("SNOWBOARD_YETI", "org.lgna.story.resources.prop.SnowboardResource"),
          createMoreSpecificFieldReplacement("ADULT_RED", "org.lgna.story.resources.prop.SnowboardResource"),

          createMoreSpecificFieldPattern("SNOWBOARD_YETI2", "org.lgna.story.resources.prop.SnowboardResource"),
          createMoreSpecificFieldReplacement("ADULT_GREEN", "org.lgna.story.resources.prop.SnowboardResource"),

          createMoreSpecificFieldPattern("SNOWBOARD_YETI_BABY", "org.lgna.story.resources.prop.SnowboardResource"),
          createMoreSpecificFieldReplacement("BABY_ORANGE", "org.lgna.story.resources.prop.SnowboardResource"),

          createMoreSpecificFieldPattern("SNOWBOARD_YETI_BABY2", "org.lgna.story.resources.prop.SnowboardResource"),
          createMoreSpecificFieldReplacement("BABY_PINK", "org.lgna.story.resources.prop.SnowboardResource"),

          createMoreSpecificFieldPattern("SPIRE1", "org.lgna.story.resources.prop.RockySpiresResource"),
          createMoreSpecificFieldReplacement("LARGE", "org.lgna.story.resources.prop.RockyOutcropResource"),

          createMoreSpecificFieldPattern("SPIRE2", "org.lgna.story.resources.prop.RockySpiresResource"),
          createMoreSpecificFieldReplacement("MEDIUM", "org.lgna.story.resources.prop.RockyOutcropResource"),

          createMoreSpecificFieldPattern("SPIRE3", "org.lgna.story.resources.prop.RockySpiresResource"),
          createMoreSpecificFieldReplacement("SMALL", "org.lgna.story.resources.prop.RockyOutcropResource"),

          "name=\"org.lgna.story.resources.prop.RockySpiresResource",
          "name=\"org.lgna.story.resources.prop.RockyOutcropResource",

          createMoreSpecificFieldPattern("ROUND", "org.lgna.story.resources.prop.LanternResource"),
          createMoreSpecificFieldReplacement("SHORT_AND_ROUND", "org.lgna.story.resources.prop.PaperLanternResource"),

          createMoreSpecificFieldPattern("OVAL", "org.lgna.story.resources.prop.LanternResource"),
          createMoreSpecificFieldReplacement("TALL_AND_ROUND", "org.lgna.story.resources.prop.PaperLanternResource"),

          createMoreSpecificFieldPattern("BOXY", "org.lgna.story.resources.prop.LanternResource"),
          createMoreSpecificFieldReplacement("SQUARE_HOURGLASS", "org.lgna.story.resources.prop.PaperLanternResource"),

          createMoreSpecificFieldPattern("POINTY", "org.lgna.story.resources.prop.LanternResource"),
          createMoreSpecificFieldReplacement("ROUND_HOURGLASS", "org.lgna.story.resources.prop.PaperLanternResource"),

          "name=\"org.lgna.story.resources.prop.LanternResource",
          "name=\"org.lgna.story.resources.prop.PaperLanternResource",

          createMoreSpecificFieldPattern("OAR", "org.lgna.story.resources.prop.OarResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.OarResource"),

          createMoreSpecificFieldPattern("SLED", "org.lgna.story.resources.prop.SledResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.SledResource"),

          createMoreSpecificFieldPattern("COW", "org.lgna.story.resources.quadruped.CowResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.quadruped.CowResource"),

          createMoreSpecificFieldPattern("DRAGON", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("PURPLE", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("DRAGON_BLUE", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("BLUE", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("DRAGON_GREEN", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("GREEN", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("ADIRONDACK_CHAIR_LIVING_ADIRONDACK_CUSHION_STRIPES", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ADIRONDACK_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ADIRONDACK_CHAIR_LIVING_ADIRONDACK_CUSHION_POLKA", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ADIRONDACK_RED", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_LOVESEAT_ART_NOUVEAU_FRAME_ANTIQUE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_DARK_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_WOOD_MAHOGONY", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_WOOD_RED", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_REDWOOD", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_WOOD_WHITE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_WHITE_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_CUSHION_PINK_VELOUR", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_PINK", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_CUSHION_BLUE_VELOUR", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_CUSHION_RED_VELOUR", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_RED", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LOVESEAT_EXPENSIVE_CAMEL_BACK_CUSHION_BLACK_VELOUR", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_BLACK", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_FABRIC_BEIGE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BROWN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_FABRIC_ORANGE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_ORANGE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_FABRIC_WHITE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_WHITE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_FABRIC_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BLUE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_FABRIC_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_CUSHIONS_TAN", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BROWN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_CUSHIONS_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_CUSHIONS_ORANGE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_ORANGE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_CUSHIONS_RED", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_RED", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_LOVSEATLOFT_MODERN_CUSHIONS_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_BEIGECROSS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_LIGHT_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_RED", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_RED", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("PARK_BENCH_LOVESEAT_PARK_BENCH_OAK", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("PARK_BENCH_OAK", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("PARK_BENCH_LOVESEAT_PARK_BENCH_RED", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("PARK_BENCH_RED", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("PARK_BENCH_LOVESEAT_PARK_BENCH_IVORY", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("PARK_BENCH_WHITE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_BLUE_FLOWERS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_PINK_FLOWERS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("QUAINT_PINK", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("VALUE_LOVESEAT_VALUE_RED_CHECKER", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("VALUE_RED_SQUARES", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("VALUE_LOVESEAT_VALUE_BLUE_CHECKER", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("VALUE_BLUE_SQUARES", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("VALUE_LOVESEAT_VALUE_FLOWER", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("VALUE_WHITE", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("VALUE1_SOFA_VALUE1_REDCHECKER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE1_RED_SQUARES", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("VALUE1_SOFA_VALUE1_BLUE_CHECKER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("VALUE1_BLUE_SQUARES", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL2_SOFA_COLONIAL2_NEONBLUE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL2_BLUE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL2_SOFA_COLONIAL2_GREEN_FLORAL", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL2_GREEN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("COLONIAL2_SOFA_COLONIAL2_WHITEDIAMOND", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("COLONIAL2_GRAY", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("STEEL_FRAME_SOFA_MODERN_STEEL_FRAME_FABRIC_LEATHER", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("STEEL_FRAME_BRWON_LEATHER", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MOROCCAN_SOFA_MOROCCAN_BEIGECROSS", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_LIGHT_BLUE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("QUAINT_SOFA_QUAINT_FABRIC_BEIGE_FLOWERS", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("QUAINT_TAN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_CUTOUT_SOFA_UM_CUTOUT_RED", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_CUTOUT_RED", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("MODERN_DIAMOND_SOFA_U_M_DIAMOND_BABYBLUE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("MODERN_DIAMOND_TURQUOISE", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("DESIGNER_DRESSER_DESIGNER_RED", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("DESIGNER_RED", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("JAPANESE_DRESSER_JAPANESE_TANSU_LIGHT", "org.lgna.story.resources.prop.DresserResource"),
          createMoreSpecificFieldReplacement("JAPANESE_LIGHT_WOOD", "org.lgna.story.resources.prop.DresserResource"),

          createMoreSpecificFieldPattern("ART_NOVEAU_TABLE_COFFEE_ART_NOUVEAU_TABLE1", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("ART_NOVEAU_LIGHT_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("ART_NOVEAU_TABLE_COFFEE_ART_NOUVEAU_TABLE2", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("ART_NOVEAU_DARK_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("ART_NOVEAU_TABLE_COFFEE_ART_NOUVEAU_TABLE3", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("ART_NOVEAU_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SMALL_CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_LTBLUE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SMALL_CLUB_BLUE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("DESIGNER_TABLE_COFFEE_END_DESIGNER_ASH", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("DESIGNER_ASH", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("LOFT_TABLE_COFFEE_LOFT_RED_METAL", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("LOFT_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_COFFEE_MOROCCAN_WOODS_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("PINE_TABLE_COFFEE_PINE_CEDAR_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("PINE_CEDAR", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("PINE_TABLE_COFFEE_PINE_BLONDE_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("PINE_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("PINE_TABLE_COFFEE_PINE_HONEY_PINE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("PINE_HONEY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("PINE_TABLE_COFFEE_PINE_WALNUT_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("PINE_WALNUT", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("PINE_TABLE_COFFEE_PINE_BIRCH_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("PINE_BIRCH", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_COFFEE_QUAINT_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_RED", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("SPINDLE_TABLE_COFFEE_SPINDLE_WOOD_OLDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("SPINDLE_OLD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DESK_CENTRAL_ASIAN_RED", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_RED", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DESK_CENTRAL_ASIAN_AVODIRE", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_REDWOOD", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("CLUB_DESK_CLUB_ASH", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("CLUB_ASH", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("CLUB_DESK_CLUB_REDWOOD", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("CLUB_REDWOOD", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("QUAINT_DESK_QUAINT_RED", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("QUAINT_RED", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("VALUE_DESK_VALUE_WOOD_METAL", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("VALUE_METAL", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("VALUE_DESK_VALUE_WOODMAPPLE", "org.lgna.story.resources.prop.DeskResource"),
          createMoreSpecificFieldReplacement("VALUE_MAPLE", "org.lgna.story.resources.prop.DeskResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_ROOT", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_CURLY_DARK_WOOD", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_RED", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_REDWOOD", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_REDDARK", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_MAHOGANY", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_WOOD", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_DARK_WOOD", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_DINING_CLUB_PINE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("CLUB_PINE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_DINING_MOROCCAN_GREEN", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_GREEN", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_TABLE_DINING_ORIENTAL_FISH_BROWN", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_FISH_BROWN", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_TABLE_DINING_ORIENTAL_FISH_RED", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_FISH_RED", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("OUTDOOR_TABLE_DINING_OUTDOOR_WOOD_CROSSPINE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("OUTDOOR_PINE", "org.lgna.story.resources.prop.DiningTableResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_CHAIR_DINING_ART_NOUVEAU_MID_CLEAN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_CHAIR_DINING_ART_NOUVEAU_DARK_CLEAN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_DARK_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("CLUB_CHAIR_DINING_CLUB_OAKCANE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("CLUB_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("COLONIAL_CHAIR_DINING_COLONIAL1_PURPLE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("COLONIAL_BLUE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("COLONIAL_CHAIR_DINING_COLONIAL1_GOLDEN2", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("COLONIAL_YELLOW", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("FANCY_COLONIAL_CHAIR_DINING_COLONIAL2_GOLDFLOWER", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("FANCY_COLONIAL_GOLD_PATTERN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("FANCY_COLONIAL_CHAIR_DINING_COLONIAL2_BEIGE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("FANCY_COLONIAL_TAN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("PARK_LOVESEAT_PARK_BENCH_OAKGREEN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("PARK_GREEN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("DANISH_MODERN_CHAIR_DINING_DANISH_MODERN_CUSHIONS_POLKA", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("DANISH_MODERN_PURPLE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("DANISH_MODERN_CHAIR_DINING_DANISH_MODERN_CUSHIONS_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("DANISH_MODERN_RED", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_SEAT_GREEN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_GREEN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_SEAT_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_RED", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_SEAT_TAN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_WHITE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_SEAT_ORANGE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_YELLOW", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_CHAIR_DINING_LOFT_FORK_BASE_IRON", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_SEAT_GREEN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_GREEN", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_SEAT_ORANGE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_YELLOW", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_SEAT_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_RED", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_SEAT_TAN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_WHITE", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_CHAIR_DINING_LOFT_FORK_BASE_IRON", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_BODY_BLACK", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_BODY_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_BODY_TEAL", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_TEAL_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_BODY_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_RED_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_BODY_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_BLUE_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_SEAT_BUMBLE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_YELLOW_STRIPES", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_SEAT_STRAWBERRY", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_RED", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_CHAIR_DINING_MODERATE_SEAT_YELLOW", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_YELLOW", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("ORIENTAL_CHAIR_DINING_ORIENTAL_BLACK", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_LOFT_BOOKCASE_WOOD_DARK", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("LOFT_DARK_WOOD", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("LOFT_LOFT_BOOKCASE_WOOD_LIGHT", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("LOFT_LIGHT_WOOD", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("LOFT_LOFT_BOOKCASE_WOOD_MEDIUM", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("LOFT_WOOD", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CHEAP_BOOKCASE_CHEAP_WOOD_PLANK", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CHEAP_PLANK", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("CINDER_BLOCK_BOOKCASE_CINDERBLOCK_SHELVES_KNOTTYPINE", "org.lgna.story.resources.prop.BookcaseResource"),
          createMoreSpecificFieldReplacement("CINDER_BLOCK_PINE", "org.lgna.story.resources.prop.BookcaseResource"),

          createMoreSpecificFieldPattern("SWING_ARM_LIGHTING_FLOOR_CLUB_LAMP_LAMP", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("SWING_ARM", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_BLUESHADE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_BLUE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_BLUESHADEON", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_BLUE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_PLAINSHADE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_WHITE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_PLAINSHADEON", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_WHITE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_OLIVESHADE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_GREEN_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_OLIVESHADEON", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_GREEN_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_ORANGESHADE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_ORANGE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_REDSHADE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_RED_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("DESIGNER_LIGHTING_FLOOR_DESIGNER_SHADE_REDSHADEON", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("DESIGNER_RED_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_BOLLARD_LIGHTING_FLOOR_GARDEN_TIER_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_BOLLARD_BLACK_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_BOLLARD_LIGHTING_FLOOR_GARDEN_TIER", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_BOLLARD_BLACK_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_BOLLARD_LIGHTING_FLOOR_GARDEN_TIER_GREEN_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_BOLLARD_GREEN_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_TIER_LIGHTING_FLOOR_GARDEN_TIER_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_TIER_BLACK_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_TIER_LIGHTING_FLOOR_GARDEN_TIER", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_TIER_BLACK_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("GARDEN_TIER_LIGHTING_FLOOR_GARDEN_TIER_GREEN_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("GARDEN_TIER_GREEN_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_ORANGE_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          NO_REPLACEMENT,

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_RED_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_RED_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_YELLOW_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_YELLOW_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_BLUE_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_BLUE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_RED_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_RED_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("LOFT_LIGHTING_FLOOR_LOFT_LAMP_SHADE_GREEN_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("LOFT_GREEN_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_YELLOW_UNLIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_YELLOW_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_BLUES_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_BLUE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_GOLD_BLUE_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_GOLD_BLUE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_RED_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_RED_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("MOROCCAN_LIGHTING_FLOOR_MOROCCAN_SHADE_YELLOW_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_YELLOW_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_BEIGE_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_ORANGE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_BEIGE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_ORANGE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_WHITE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_BLUE", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE_OFF", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_BLUE_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_GREEN_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_WHITE_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("QUAINT_LIGHTING_FLOOR_QUAINT_SHADE_YELLOW_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("QUAINT_YELLOW_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("STUDIO_LIGHTING_FLOOR_STUDIO_LIGHTS_LIGHTS_LIT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("STUDIO_ON", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("VALUE_LIGHTING_FLOOR_VALUE_PAINTED_METAL_WHITEPAINT", "org.lgna.story.resources.prop.LampResource"),
          createMoreSpecificFieldReplacement("VALUE_WHITE", "org.lgna.story.resources.prop.LampResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_WOOD_BROWN", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TABLE_END_CENTRAL_ASIAN_WOOD_DARK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_DARK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_WHITEOAK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CLUB_TABLE_COFFEE_CLUB1_X1_MATERIALS_LTBLUE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CLUB_BLUE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_END_MOROCCAN_END_TABLE_ALADDIN", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_YELLOW_INLAY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_END_MOROCCAN_END_TABLE_STAR", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_STARS_INLAY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_END_MOROCCAN_END_TABLE_TILE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TILE_INLAY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TABLE_END_MOROCCAN_END_TABLE_DETAIL", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_FANCY_INLAY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("OCTAGONAL_TABLE_END_OCTAGONAL_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("OCTAGONAL_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_END_QUAINT_FABRIC_BLUE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_END_QUAINT_FABRIC_PINK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_PINK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_END_QUAINT_FABRIC_BEIGE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_YELLOW", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_END_QUAINT_FABRIC_WHITE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_TABLE_END_QUAINT_FABRIC_WHITE_FLOWERS", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_FLOWERS", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_MARBLE_GREEN", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_GREEN_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_MARBLE_CREAM", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_CREAM_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_MARBLE_RED", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_RED_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_MARBLE_WHITE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_WHITE_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_MARBLE_BLACK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_BLACK_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_WOOD_SANTA_MARIA", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_DARK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_WOOD_BLACKWOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_BLACK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_WOOD_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_WOOD_WHITE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_WHITE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_TABLE_END_TRIANGULAR_TILE_WOOD_RED_OAK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRAINGULAR_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("UM_TABLE_END_UM_BLUE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("UM_BLUE", "org.lgna.story.resources.prop.EndTableResource"),

          "name=\"org.lgna.story.resources.prop.WeddingCakeResource",
          "name=\"org.lgna.story.resources.prop.CakeResource"
      ),
    };
  }

  // @formatter:on

  private TextMigrationRegistryV3159() {
  }
}
