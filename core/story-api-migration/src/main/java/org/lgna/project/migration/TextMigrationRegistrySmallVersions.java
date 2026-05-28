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

// @formatter:off
@Deprecated // Text migrations now load from migrations/text-migrations.json; retained for JSON regeneration and documentation.
final class TextMigrationRegistrySmallVersions {

  static TextMigration[] createEarly() {
    return new TextMigration[] {
      new TextMigration(
          new Version("3.1.8.0.0")),

      new TextMigration(
          new Version("3.1.9.0.0"),

          "ARMOIRE_CLOTHING", NO_REPLACEMENT,

          "org.lgna.story.resources.armoire.ArmoireArtNouveau",
          NO_REPLACEMENT,

          "PINK_POODLE", NO_REPLACEMENT,

          "org.lgna.story.resources.quadruped.Poodle", NO_REPLACEMENT),

      new TextMigration(
          new Version("3.1.11.0.0")),

      new TextMigration(
          new Version("3.1.14.0.0"),

          "CAMEL", NO_REPLACEMENT,

          "org.lgna.story.resources.quadruped.Camel", NO_REPLACEMENT,

          "FALCON", NO_REPLACEMENT,

          "org.lgna.story.resources.flyer.Falcon", NO_REPLACEMENT,

          "LION", NO_REPLACEMENT,

          "org.lgna.story.resources.quadruped.Lion", NO_REPLACEMENT,

          "WOLF", NO_REPLACEMENT,

          "org.lgna.story.resources.quadruped.Wolf", NO_REPLACEMENT),

      new TextMigration(
          new Version("3.1.15.1.0")),

      new TextMigration(
          new Version("3.1.20.0.0"),

          "org.lgna.story.resources.dresser.DresserCentralAsian",
          "org.lgna.story.resources.prop.DresserCentralAsian",

          "org.lgna.story.resources.dresser.DresserColonial",
          "org.lgna.story.resources.prop.DresserColonial",

          "org.lgna.story.resources.dresser.DresserDesigner",
          "org.lgna.story.resources.prop.DresserDesigner"),

      new TextMigration(
          new Version("3.1.33.0.0")),
    };
  }

  static TextMigration[] createMid() {
    return new TextMigration[] {
      new TextMigration(
          new Version("3.1.35.0.0"),

          "org.lgna.story.resources.biped.Alien",
          "org.lgna.story.resources.biped.AlienResource",

          "org.lgna.story.resources.biped.BabyYeti",
          "org.lgna.story.resources.biped.BabyYetiResource",

          "org.lgna.story.resources.biped.BigBadWolf",
          "org.lgna.story.resources.biped.BigBadWolfResource",

          "org.lgna.story.resources.biped.Bunny",
          "org.lgna.story.resources.biped.BunnyResource",

          "org.lgna.story.resources.biped.CheshireCat",
          "org.lgna.story.resources.biped.CheshireCatResource",

          "org.lgna.story.resources.biped.Hare",
          "org.lgna.story.resources.biped.HareResource",

          "org.lgna.story.resources.biped.MadHatter",
          "org.lgna.story.resources.biped.MadHatterResource",

          "org.lgna.story.resources.biped.MarchHare",
          "org.lgna.story.resources.biped.MarchHareResource",

          "org.lgna.story.resources.biped.Ogre",
          "org.lgna.story.resources.biped.OgreResource",

          "org.lgna.story.resources.biped.Pig",
          "org.lgna.story.resources.biped.PigResource",

          "org.lgna.story.resources.biped.Pixie",
          "org.lgna.story.resources.biped.PixieResource",

          "org.lgna.story.resources.biped.PlayingCard",
          "org.lgna.story.resources.biped.PlayingCardResource",

          "org.lgna.story.resources.biped.QueenOfHearts",
          "org.lgna.story.resources.biped.QueenOfHeartsResource",

          "org.lgna.story.resources.biped.StuffedTiger",
          "org.lgna.story.resources.biped.StuffedTigerResource",

          "org.lgna.story.resources.biped.Tortoise",
          "org.lgna.story.resources.biped.TortoiseResource",

          "org.lgna.story.resources.biped.Troll",
          "org.lgna.story.resources.biped.TrollResource",

          "org.lgna.story.resources.biped.WhiteRabbit",
          "org.lgna.story.resources.biped.WhiteRabbitResource",

          "org.lgna.story.resources.biped.Witch",
          "org.lgna.story.resources.biped.WitchResource",

          "org.lgna.story.resources.biped.Yeti",
          "org.lgna.story.resources.biped.YetiResource",

          "org.lgna.story.resources.flyer.Chicken",
          "org.lgna.story.resources.flyer.ChickenResource",

          "org.lgna.story.resources.flyer.Falcon",
          "org.lgna.story.resources.flyer.FalconResource",

          "org.lgna.story.resources.flyer.Flamingo",
          "org.lgna.story.resources.flyer.FlamingoResource",

          "org.lgna.story.resources.flyer.Owl",
          "org.lgna.story.resources.flyer.OwlResource",

          "org.lgna.story.resources.flyer.Penguin",
          "org.lgna.story.resources.flyer.PenguinResource",

          "org.lgna.story.resources.flyer.Seagull",
          "org.lgna.story.resources.flyer.SeagullResource",

          "org.lgna.story.resources.flyer.Toucan",
          "org.lgna.story.resources.flyer.ToucanResource",

          "org.lgna.story.resources.prop.Armoire",
          "org.lgna.story.resources.prop.ArmoireResource",

          "org.lgna.story.resources.prop.Bookcase",
          "org.lgna.story.resources.prop.BookcaseResource",

          "org.lgna.story.resources.prop.Boulder",
          "org.lgna.story.resources.prop.BoulderResource",

          "org.lgna.story.resources.prop.BowlingPin",
          "org.lgna.story.resources.prop.BowlingPinResource",

          "org.lgna.story.resources.prop.Cake",
          "org.lgna.story.resources.prop.CakeResource",

          "org.lgna.story.resources.prop.CandyFactory",
          "org.lgna.story.resources.prop.CandyFactoryResource",

          "org.lgna.story.resources.prop.CastleGate",
          "org.lgna.story.resources.prop.CastleGateResource",

          "org.lgna.story.resources.prop.CastleTowerBase",
          "org.lgna.story.resources.prop.CastleTowerBaseResource",

          "org.lgna.story.resources.prop.CastleTowerMiddle",
          "org.lgna.story.resources.prop.CastleTowerMiddleResource",

          "org.lgna.story.resources.prop.CastleTowerTop",
          "org.lgna.story.resources.prop.CastleTowerTopResource",

          "org.lgna.story.resources.prop.CastleWall",
          "org.lgna.story.resources.prop.CastleWallResource",

          "declaringClass name=\"org.lgna.story.resources.prop.Cauldron\"",
          "declaringClass name=\"org.lgna.story.resources.prop.CauldronResource\"",

          "declaringClass name=\"org.lgna.story.resources.prop.CauldronLid\"",
          "declaringClass name=\"org.lgna.story.resources.prop.CauldronLidResource\"",

          "org.lgna.story.resources.prop.Cave",
          "org.lgna.story.resources.prop.CaveResource",

          "org.lgna.story.resources.prop.Chair",
          "org.lgna.story.resources.prop.ChairResource",

          "org.lgna.story.resources.prop.CoffeeTable",
          "org.lgna.story.resources.prop.CoffeeTableResource",

          "org.lgna.story.resources.prop.ColaBottle",
          "org.lgna.story.resources.prop.ColaBottleResource",

          "org.lgna.story.resources.prop.CoralShelf",
          "org.lgna.story.resources.prop.CoralShelfResource",

          "org.lgna.story.resources.prop.Desk",
          "org.lgna.story.resources.prop.DeskResource",

          "org.lgna.story.resources.prop.DiningTable",
          "org.lgna.story.resources.prop.DiningTableResource",

          "org.lgna.story.resources.prop.Dresser",
          "org.lgna.story.resources.prop.DresserResource",

          "org.lgna.story.resources.prop.EndTable",
          "org.lgna.story.resources.prop.EndTableResource",

          "org.lgna.story.resources.prop.Grill",
          "org.lgna.story.resources.prop.GrillResource",

          "org.lgna.story.resources.prop.Hedge",
          "org.lgna.story.resources.prop.HedgeResource",

          "org.lgna.story.resources.prop.Helicopter",
          "org.lgna.story.resources.prop.HelicopterResource",

          "org.lgna.story.resources.prop.Iceberg",
          "org.lgna.story.resources.prop.IcebergResource",

          "org.lgna.story.resources.prop.IceFlow",
          "org.lgna.story.resources.prop.IceFlowResource",

          "org.lgna.story.resources.prop.IceMountain",
          "org.lgna.story.resources.prop.IceMountainResource",

          "org.lgna.story.resources.prop.Lamp",
          "org.lgna.story.resources.prop.LampResource",

          "org.lgna.story.resources.prop.Loveseat",
          "org.lgna.story.resources.prop.LoveseatResource",

          "org.lgna.story.resources.prop.MagicSpoon",
          "org.lgna.story.resources.prop.MagicSpoonResource",

          "org.lgna.story.resources.prop.MagicStaff",
          "org.lgna.story.resources.prop.MagicStaffResource",

          "org.lgna.story.resources.prop.MagicStone",
          "org.lgna.story.resources.prop.MagicStoneResource",

          "org.lgna.story.resources.prop.MagicWand",
          "org.lgna.story.resources.prop.MagicWandResource",

          "org.lgna.story.resources.prop.PirateShip",
          "org.lgna.story.resources.prop.PirateShipResource",

          "org.lgna.story.resources.prop.Plateau",
          "org.lgna.story.resources.prop.PlateauResource",

          "org.lgna.story.resources.prop.PocketWatch",
          "org.lgna.story.resources.prop.PocketWatchResource",

          "org.lgna.story.resources.prop.Potion",
          "org.lgna.story.resources.prop.PotionResource",

          "org.lgna.story.resources.prop.PrayerFlags",
          "org.lgna.story.resources.prop.PrayerFlagsResource",

          "org.lgna.story.resources.prop.RedRover",
          "org.lgna.story.resources.prop.RedRoverResource",

          "org.lgna.story.resources.prop.Rose",
          "org.lgna.story.resources.prop.RoseResource",

          "org.lgna.story.resources.prop.Saucer",
          "org.lgna.story.resources.prop.SaucerResource",

          "org.lgna.story.resources.prop.SeaPlant",
          "org.lgna.story.resources.prop.SeaPlantResource",

          "org.lgna.story.resources.prop.Seaweed",
          "org.lgna.story.resources.prop.SeaweedResource",

          "org.lgna.story.resources.prop.ShortMushroom",
          "org.lgna.story.resources.prop.ShortMushroomResource",

          "org.lgna.story.resources.prop.Sled",
          "org.lgna.story.resources.prop.SledResource",

          "org.lgna.story.resources.prop.Snowboard",
          "org.lgna.story.resources.prop.SnowboardResource",

          "org.lgna.story.resources.prop.Sofa",
          "org.lgna.story.resources.prop.SofaResource",

          "org.lgna.story.resources.prop.SpellBook",
          "org.lgna.story.resources.prop.SpellBookResource",

          "org.lgna.story.resources.prop.StoneBridge",
          "org.lgna.story.resources.prop.StoneBridgeResource",

          "org.lgna.story.resources.prop.Stove",
          "org.lgna.story.resources.prop.StoveResource",

          "org.lgna.story.resources.prop.Submarine",
          "org.lgna.story.resources.prop.SubmarineResource",

          "org.lgna.story.resources.prop.Suitcase",
          "org.lgna.story.resources.prop.SuitcaseResource",

          "org.lgna.story.resources.prop.TallMushroom",
          "org.lgna.story.resources.prop.TallMushroomResource",

          "org.lgna.story.resources.prop.Teacup",
          "org.lgna.story.resources.prop.TeacupResource",

          "org.lgna.story.resources.prop.Teapot",
          "org.lgna.story.resources.prop.TeapotResource",

          "org.lgna.story.resources.prop.TeaTable",
          "org.lgna.story.resources.prop.TeaTableResource",

          "org.lgna.story.resources.prop.TeaTray",
          "org.lgna.story.resources.prop.TeaTrayResource",

          "org.lgna.story.resources.prop.Tent",
          "org.lgna.story.resources.prop.TentResource",

          "org.lgna.story.resources.prop.Trashcan",
          "org.lgna.story.resources.prop.TrashcanResource",

          "org.lgna.story.resources.prop.TreasureChest",
          "org.lgna.story.resources.prop.TreasureChestResource",

          "org.lgna.story.resources.prop.UFO",
          "org.lgna.story.resources.prop.UFOResource",

          "org.lgna.story.resources.prop.Volleyball",
          "org.lgna.story.resources.prop.VolleyballResource",

          "org.lgna.story.resources.prop.WeddingCake",
          "org.lgna.story.resources.prop.WeddingCakeResource",

          "org.lgna.story.resources.prop.WonderlandTree",
          "org.lgna.story.resources.prop.WonderlandTreeResource",

          "org.lgna.story.resources.quadruped.AbyssinianCat",
          "org.lgna.story.resources.quadruped.AbyssinianCatResource",

          "org.lgna.story.resources.quadruped.AlienRobot",
          "org.lgna.story.resources.quadruped.AlienRobotResource",

          "org.lgna.story.resources.quadruped.BabyDragon",
          "org.lgna.story.resources.quadruped.BabyDragonResource",

          "org.lgna.story.resources.quadruped.BillyGoat",
          "org.lgna.story.resources.quadruped.BillyGoatResource",

          "org.lgna.story.resources.quadruped.Camel",
          "org.lgna.story.resources.quadruped.CamelResource",

          "org.lgna.story.resources.quadruped.Cow",
          "org.lgna.story.resources.quadruped.CowResource",

          "org.lgna.story.resources.quadruped.Dalmatian",
          "org.lgna.story.resources.quadruped.DalmatianResource",

          "org.lgna.story.resources.quadruped.Dragon",
          "org.lgna.story.resources.quadruped.DragonResource",

          "org.lgna.story.resources.quadruped.Lioness",
          "org.lgna.story.resources.quadruped.LionessResource",

          "org.lgna.story.resources.quadruped.ManxCat",
          "org.lgna.story.resources.quadruped.ManxCatResource",

          "org.lgna.story.resources.quadruped.Poodle",
          "org.lgna.story.resources.quadruped.PoodleResource",

          "org.lgna.story.resources.quadruped.ScottyDog",
          "org.lgna.story.resources.quadruped.ScottyDogResource",

          "org.lgna.story.resources.quadruped.ShortHairCat",
          "org.lgna.story.resources.quadruped.ShortHairCatResource",

          "org.lgna.story.resources.quadruped.Wolf",
          "org.lgna.story.resources.quadruped.WolfResource",

          "org.lgna.story.resources.fish.BlueTang",
          "org.lgna.story.resources.fish.BlueTangResource",

          "org.lgna.story.resources.fish.ClownFish",
          "org.lgna.story.resources.fish.ClownFishResource",

          "org.lgna.story.resources.fish.PajamaFish",
          "org.lgna.story.resources.fish.PajamaFishResource",

          "org.lgna.story.resources.fish.Shark",
          "org.lgna.story.resources.fish.SharkResource",

          "org.lgna.story.resources.marinemammal.BabyWalrus",
          "org.lgna.story.resources.marinemammal.BabyWalrusResource",

          "org.lgna.story.resources.marinemammal.Dolphin",
          "org.lgna.story.resources.marinemammal.DolphinResource",

          "org.lgna.story.resources.marinemammal.Orca",
          "org.lgna.story.resources.marinemammal.OrcaResource",

          "org.lgna.story.resources.marinemammal.Walrus",
          "org.lgna.story.resources.marinemammal.WalrusResource"
      ),
      new TextMigration(
          new Version("3.1.38.0.0")
      ),
      new TextMigration(
          new Version("3.1.39.0.0"),

          "org.lgna.story.event.MouseClickOnScreenListener\"/><type name=\"\\[Lorg.lgna.story.AddMouseButtonListener",
          "org.lgna.story.event.MouseClickOnScreenListener\"/><type name=\"\\[Lorg.lgna.story.AddMouseClickOnScreenListener",

          "org.lgna.story.event.MouseClickOnObjectListener\"/><type name=\"\\[Lorg.lgna.story.AddMouseButtonListener",
          "org.lgna.story.event.MouseClickOnObjectListener\"/><type name=\"\\[Lorg.lgna.story.AddMouseClickOnObjectListener"
      ),
      new TextMigration(
          new Version("3.1.48.0.0"),

          "BILLY_GOAT",
          "BIG_HORNS"
      ),
      new TextMigration(
          new Version("3.1.58.0.0"),

          "name=\"ICE_FLOW",
          "name=\"ICE_FLOE",

          "name=\"org.lgna.story.resources.prop.IceFlowResource",
          "name=\"org.lgna.story.resources.prop.IceFloeResource",

          "name=\"PIXIE_GREEN",
          "name=\"GREEN",

          "name=\"PIXIE_PINK",
          "name=\"PINK",

          "name=\"PIXIE_BLUE",
          "name=\"BLUE"
      ),
    };
  }

  // @formatter:on

  private TextMigrationRegistrySmallVersions() {
  }
}
