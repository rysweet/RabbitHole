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

import static org.lgna.project.migration.ProjectMigrationTextMigrationFactory.createVersion3_2_110TextMigration;
import static org.lgna.project.migration.ProjectMigrationTextSnippets.createJointIdPattern;
import static org.lgna.project.migration.ProjectMigrationTextSnippets.createJointIdReplacement;
import static org.lgna.project.migration.ProjectMigrationTextSnippets.createMoreSpecificFieldPattern;
import static org.lgna.project.migration.ProjectMigrationTextSnippets.createMoreSpecificFieldReplacement;

// @formatter:off
@Deprecated // Text migrations now load from migrations/text-migrations.json; retained for JSON regeneration and documentation.
final class TextMigrationRegistryLateVersions {

  static TextMigration[] create() {
    return new TextMigration[] {
      new TextMigration(
          new Version("3.1.68.0.0"),

          createMoreSpecificFieldPattern("STRAIGHT1_RIVERBANK2", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT1", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT1_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT1", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT2_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT2", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT2_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT2", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT3_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT3", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT3_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT3", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT4_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT4", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT4_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT4", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE1_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE1", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE1_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE1", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE2_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE2", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE2_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE2", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE3_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE3", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE3_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE3", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE4_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE4", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE4_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE4", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW1_RIVERBANK3", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW1", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW1_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW1", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW2_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW2", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW2_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW2", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW3_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW3", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW3_BROWN", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW3", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("PINK", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_PINK", "org.lgna.story.resources.quadruped.DragonBabyResource"),

          createMoreSpecificFieldPattern("AQUA", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_AQUA", "org.lgna.story.resources.quadruped.DragonBabyResource"),

          createMoreSpecificFieldPattern("BLUE", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_BLUE", "org.lgna.story.resources.quadruped.DragonBabyResource"),

          createMoreSpecificFieldPattern("GREEN", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_GREEN", "org.lgna.story.resources.quadruped.DragonBabyResource"),

          createMoreSpecificFieldPattern("RED", "org.lgna.story.resources.quadruped.BabyDragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_RED", "org.lgna.story.resources.quadruped.DragonBabyResource"),

          "name=\"org.lgna.story.resources.quadruped.BabyDragonResource",
          "name=\"org.lgna.story.resources.quadruped.DragonBabyResource",

          createMoreSpecificFieldPattern("WITH_SCARF", "org.lgna.story.resources.biped.BabyYetiResource"),
          createMoreSpecificFieldReplacement("WITH_SCARF", "org.lgna.story.resources.biped.YetiBabyResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.biped.BabyYetiResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.YetiBabyResource"),

          "name=\"org.lgna.story.resources.biped.BabyYetiResource",
          "name=\"org.lgna.story.resources.biped.YetiBabyResource",

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.marinemammal.BabyWalrusResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.marinemammal.WalrusBabyResource"),

          "name=\"org.lgna.story.resources.marinemammal.BabyWalrusResource",
          "name=\"org.lgna.story.resources.marinemammal.WalrusBabyResource",

          createMoreSpecificFieldPattern("ROSE_RED", "org.lgna.story.resources.prop.RoseResource"),
          createMoreSpecificFieldReplacement("RED", "org.lgna.story.resources.prop.RoseResource"),

          createMoreSpecificFieldPattern("ROSE_WHITE", "org.lgna.story.resources.prop.RoseResource"),
          createMoreSpecificFieldReplacement("WHITE", "org.lgna.story.resources.prop.RoseResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.IceFloeResource"),
          createMoreSpecificFieldReplacement("ICE_FLOE1", "org.lgna.story.resources.prop.IceFloeResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.CanyonSpiresResource"),
          createMoreSpecificFieldReplacement("DESERT", "org.lgna.story.resources.prop.CanyonSpiresResource"),

          createMoreSpecificFieldPattern("CANYON_SPIRES", "org.lgna.story.resources.prop.CanyonSpiresResource"),
          createMoreSpecificFieldReplacement("DESERT", "org.lgna.story.resources.prop.CanyonSpiresResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.CliffWallResource"),
          createMoreSpecificFieldReplacement("DEFAULT_DESERT", "org.lgna.story.resources.prop.CliffWallResource"),

          createMoreSpecificFieldPattern("CLIFF_WALL", "org.lgna.story.resources.prop.CliffWallResource"),
          createMoreSpecificFieldReplacement("DEFAULT_DESERT", "org.lgna.story.resources.prop.CliffWallResource"),

          createMoreSpecificFieldPattern("PURPLE", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_PURPLE", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("GREEN", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_GREEN", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("BLUE", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_BLUE", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("ORANGE", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_ORANGE", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("RED", "org.lgna.story.resources.quadruped.DragonResource"),
          createMoreSpecificFieldReplacement("DEFAULT_RED", "org.lgna.story.resources.quadruped.DragonResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_DARK_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_DARK_WOOD_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_OAK_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("ART_NOUVEAU_OAK", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("ART_NOUVEAU_OAK_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_WHITE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_WHITE_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_BLACK", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_BLACK_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_BLUE_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_PINK", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_PINK_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_RED", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_RED_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_LIGHT_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_WHITE_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_BLACK_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_REDWOOD", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_RED_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("CAMEL_BACK_WHITE_WOOD", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("CAMEL_BACK_WHITE_MAHOGANY", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_BLUE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BLUE_BLUE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_GREEN", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_GREEN_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_ORANGE", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_ORANGE_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_RED", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_ORANGE_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_BROWN", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_ORANGE_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_BROWN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BLUE_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_BLUE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BLUE_BLUE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_GREEN_GREEN_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_ORANGE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_ORANGE_ORANGE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("MODERN_LOFT_WHITE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),
          createMoreSpecificFieldReplacement("MODERN_LOFT_BLUE_BLUE_CUSHIONS", "org.lgna.story.resources.prop.LoveseatResource"),

          createMoreSpecificFieldPattern("QUAINT_WHITE", "org.lgna.story.resources.prop.SofaResource"),
          createMoreSpecificFieldReplacement("QUAINT_TAN", "org.lgna.story.resources.prop.SofaResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_FANCY_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_BLONDE_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_FANCY_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_CHERRY_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_FANCY_DARK_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_DARK_WOOD_DARK_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_FANCY_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_REDWOOD_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_BLONDE_BLONDE", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_CHERRY_CHERRY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DARK_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_DARK_WOOD_DARK_WOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_FANCY_REDWOOD_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_YELLOW_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_YELLOW_INLAY_BLACK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_FANCY_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_FANCY_INLAY_BLACK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_STARS_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_STARS_INLAY_BLACK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_TILE_INLAY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TILE_INLAY_BLACK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_BLACK", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TILE_INLAY_BLACK", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TILE_INLAY_REDWOOD", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_MAHOGANY", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_TILE_INLAY_MAHOGANY", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("MOROCCAN_YELLOW", "org.lgna.story.resources.prop.CoffeeTableResource"),
          createMoreSpecificFieldReplacement("MOROCCAN_FANCY_INLAY_YELLOW", "org.lgna.story.resources.prop.CoffeeTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_DRAGON_BROWN", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_DRAGON_BROWN", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_DRAGON_RED", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_DRAGON_RED", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_FISH_BROWN", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_FISH_BROWN", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_FISH_RED", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_FISH_RED", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_LOTUS_BLACK", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_LOTUS_BLACK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("ORIENTAL_LOTUS_ORANGE", "org.lgna.story.resources.prop.DiningTableResource"),
          createMoreSpecificFieldReplacement("ORIENTAL_LOTUS_ORANGE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("LOFT_BLACK_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_OAK_BLACK_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_DARK_HONEY_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_OAK_DARK_HONEY_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_SWIRLY_BROWN_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_DARK_WOOD_DARK_HONEY_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_DARK_RED_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_RED_FINISH_DARK_HONEY_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_MEDIUM_BROWN_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_DARK_WOOD_DARK_HONEY_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_RED_FINISH", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_RED_FINISH_BLACK_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_DARK_WOOD", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_DARK_WOOD_BLACK_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_OAK", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_OAK_DARK_HONEY_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_HONEY", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_OAK_DARK_HONEY_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("LOFT_MAPLE", "org.lgna.story.resources.prop.ArmoireResource"),
          createMoreSpecificFieldReplacement("LOFT_DARK_WOOD_BLACK_TRIM", "org.lgna.story.resources.prop.ArmoireResource"),

          createMoreSpecificFieldPattern("GREEN", "org.lgna.story.resources.prop.GrillResource"),
          createMoreSpecificFieldReplacement("GREEN_LIT", "org.lgna.story.resources.prop.GrillResource"),

          createMoreSpecificFieldPattern("BLACK", "org.lgna.story.resources.prop.GrillResource"),
          createMoreSpecificFieldReplacement("BLACK_LIT", "org.lgna.story.resources.prop.GrillResource"),

          createMoreSpecificFieldPattern("YELLOW", "org.lgna.story.resources.prop.GrillResource"),
          createMoreSpecificFieldReplacement("YELLOW_LIT", "org.lgna.story.resources.prop.GrillResource"),

          createMoreSpecificFieldPattern("RED", "org.lgna.story.resources.prop.GrillResource"),
          createMoreSpecificFieldReplacement("RED_LIT", "org.lgna.story.resources.prop.GrillResource"),

          createMoreSpecificFieldPattern("BLUE", "org.lgna.story.resources.prop.GrillResource"),
          createMoreSpecificFieldReplacement("BLUE_LIT", "org.lgna.story.resources.prop.GrillResource"),

          createMoreSpecificFieldPattern("HELICOPTER", "org.lgna.story.resources.prop.HelicopterResource"),
          createMoreSpecificFieldReplacement("MILITARY", "org.lgna.story.resources.prop.HelicopterResource"),

          createMoreSpecificFieldPattern("CANDY_FACTORY", "org.lgna.story.resources.prop.CandyFactoryResource"),
          createMoreSpecificFieldReplacement("LIGHT_OFF", "org.lgna.story.resources.prop.CandyFactoryResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_BLACK", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_WHITE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_RED_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_ORANGE_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_RED_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_RED_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_RED_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_BLUE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_GREEN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_GREEN_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_YELLOW", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_YELLOW_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_RED_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_FORK_WHITE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_WHITE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_BLACK", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_WHITE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_WHITE_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_ORANGE_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_YELLOW_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_RED_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_FORK_RED_LIGHT_WOOD", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_BLUE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_GREEN", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_GREEN_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_YELLOW", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_YELLOW_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_RED_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("LOFT_OFFICE_WHITE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("LOFT_OFFICE_WHITE_BLACK", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_BLACK", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_BLUE_BLACK_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_BLUE_BODY", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_BLUE_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_RED_BODY", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_RED_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_TEAL_BODY", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_TEAL_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_WOOD", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_BLUE_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_BLUE", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_BLUE_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_YELLOW_STRIPES", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_YELLOW_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_GRAY", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_GRAY_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_RED", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_RED_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_TEAL", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_TEAL_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("MODERATE_YELLOW", "org.lgna.story.resources.prop.ChairResource"),
          createMoreSpecificFieldReplacement("MODERATE_YELLOW_WOOD_BODY", "org.lgna.story.resources.prop.ChairResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TOP_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_WOOD_TOP_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TOP_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_WOOD_TOP_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TOP_RED_LAQUER", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_DARK_WOOD_TOP_RED_LAQUER", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_TOP_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_ROUGH_TOP_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_WOOD_TOP_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_DARK_WOOD_TOP_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_WOOD_TOP_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_DARK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_DARK_WOOD_TOP_BLONDE", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_RED_LAQUER", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_DARK_WOOD_TOP_RED_LAQUER", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("CENTRAL_ASIAN_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("CENTRAL_ASIAN_ROUGH_TOP_ROUGH", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_YELLOW", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_YELLOW_WHITE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_BLUE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_BLUE_BLUE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_GREEN", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_GREEN_GREEN_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_PINK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_PINK_RED_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_WHITE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_WHITE_WHITE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("QUAINT_FLOWERS", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("QUAINT_FLOWERS_WHITE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_BLACK_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_BLACK_MARBLE_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_CREAM_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_GREEN_MARBLE_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_GREEN_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_GREEN_MARBLE_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_RED_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_GREEN_MARBLE_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_WHITE_MARBLE", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_WHITE_MARBLE_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_BLACK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_BLACK_MARBLE_BLACK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_CHERRY", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_GREEN_MARBLE_WHITE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_OAK", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_GREEN_MARBLE_OAK", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_DARK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_BLACK_MARBLE_BLACK_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          createMoreSpecificFieldPattern("TRAINGULAR_WHITE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),
          createMoreSpecificFieldReplacement("TRIANGULAR_WHITE_MARBLE_WHITE_WOOD", "org.lgna.story.resources.prop.EndTableResource"),

          //sims person changes
          createMoreSpecificFieldPattern("BLACK", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldReplacement("BLACK_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldPattern("BLOND", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldReplacement("BLOND_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldPattern("BROWN", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldReplacement("BROWN_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldPattern("GREY", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldReplacement("GREY_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldPattern("RED", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),
          createMoreSpecificFieldReplacement("RED_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairTopHat"),

          createMoreSpecificFieldPattern("BLACK", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldReplacement("BLACK_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldPattern("BLOND", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldReplacement("BLOND_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldPattern("BROWN", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldReplacement("BROWN_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldPattern("GREY", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldReplacement("GREY_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldPattern("RED", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),
          createMoreSpecificFieldReplacement("RED_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedoraCasual"),

          //map both KINKY and STRAIGHT to replacement
          createMoreSpecificFieldPattern("KINKY_BLACK", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BLACK_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("KINKY_BLOND", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BLOND_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("KINKY_BROWN", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BROWN_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("KINKY_GREY", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("GREY_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("KINKY_RED", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("RED_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),

          createMoreSpecificFieldPattern("STRAIGHT_BLACK", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BLACK_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_BLOND", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BLOND_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_BROWN", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BROWN_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_GREY", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("GREY_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_RED", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("RED_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),

          createMoreSpecificFieldPattern("STRAIGHT_BLACK", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BLACK_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_BLOND", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BLOND_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_BROWN", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("BROWN_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_GREY", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("GREY_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldPattern("STRAIGHT_RED", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),
          createMoreSpecificFieldReplacement("RED_BLACK_HAT", "org.lgna.story.resources.sims2.MaleAdultHairHatFedora"),

          createMoreSpecificFieldPattern("STUBBLE_BLACK", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldReplacement("BLACK", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldPattern("STUBBLE_BLOND", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldReplacement("BLOND", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldPattern("STUBBLE_BROWN", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldReplacement("BROWN", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldPattern("STUBBLE_GREY", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldReplacement("GREY", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldPattern("STUBBLE_RED", "org.lgna.story.resources.sims2.MaleAdultHairBald"),
          createMoreSpecificFieldReplacement("RED", "org.lgna.story.resources.sims2.MaleAdultHairBald"),

          createMoreSpecificFieldPattern("BLUEPINSTRIPE", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),
          createMoreSpecificFieldReplacement("FORMAL_BLUE_PINSTRIPE", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),
          createMoreSpecificFieldPattern("BROWNTWEED", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),
          createMoreSpecificFieldReplacement("FORMAL_BROWN_TWEED", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),
          createMoreSpecificFieldPattern("GREYPINSTRIPE", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),
          createMoreSpecificFieldReplacement("FORMAL_GREY_PINSTRIPE", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),
          createMoreSpecificFieldPattern("GREYTWEED", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),
          createMoreSpecificFieldReplacement("FORMAL_GREY_TWEED", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitOpenCoatLongPants"),

          //map removed swimwear to existing swimwear
          createMoreSpecificFieldPattern("BLUEBIKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLUE_WAVE_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("BLUESINGLE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLUE_DIVING", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("GRAYSINGLE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLUE_DIVING", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("GREENSINGLE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLUE_DIVING", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("LIMEBIKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("LIME_WAVE_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("PINKBIKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("PINK_WAVE_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("PINKSINGLE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("PINK_DIVING", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("REDBIKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("RED_WAVE_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("REDSINGLE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("RED_DIVING", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("YELLOWBIKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("YELLOW_WAVE_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("BLUEPAISLEY", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLUE_FLOWER", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("CLASSICBIKINIBLACK", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLACK_RAINBOW_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("SPORTBIKINIBLUE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLUE_WAVE_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("SPORTBIKINIVIOLET", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLUE_WAVE_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("STRINGBIKINIBROWN", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLACK_RAINBOW_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldPattern("STRINGBIKINIBURGANDY", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),
          createMoreSpecificFieldReplacement("BLACK_RAINBOW_TANKINI", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSwimwear"),

          //map removed dresses to existing dresses
          createMoreSpecificFieldPattern("REDHOLE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitDressLongTwo"),
          createMoreSpecificFieldReplacement("RED", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitDressLongTwo"),
          createMoreSpecificFieldPattern("BLACKHOLE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitDressLongTwo"),
          createMoreSpecificFieldReplacement("BLACK", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitDressLongTwo"),
          createMoreSpecificFieldPattern("LEOPARD", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitDressLongTwo"),
          createMoreSpecificFieldReplacement("CREAM", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitDressLongTwo"),

          //Name change
          createMoreSpecificFieldPattern("BLUE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitPowerSuit"),
          createMoreSpecificFieldReplacement("BLUE_PINSTRIPE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitPowerSuit"),

          //Name change...Where did SOCIALWORKER come from?
          createMoreSpecificFieldPattern("SOCIALWORKER", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSuit"),
          createMoreSpecificFieldReplacement("BLACK", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitSuit"),

          //map removed swimwear to existing swimwear
          createMoreSpecificFieldPattern("TANKINIGREEN", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),
          createMoreSpecificFieldReplacement("TROPIC_SEA_SWIM", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),
          createMoreSpecificFieldPattern("TANKINIPINK", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),
          createMoreSpecificFieldReplacement("TROPIC_BERRY_SWIM", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),
          createMoreSpecificFieldPattern("TANKINISTRIPES", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),
          createMoreSpecificFieldReplacement("TROPIC_FIRE_SWIM", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),
          createMoreSpecificFieldPattern("WHITEUNDER", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),
          createMoreSpecificFieldReplacement("WHITE_CAMISOLE", "org.lgna.story.resources.sims2.ChildFullBodyOutfitNaked"),

          //Name change
          createMoreSpecificFieldPattern("FRIED", "org.lgna.story.resources.sims2.ChildHairShocked"),
          createMoreSpecificFieldReplacement("BLACK", "org.lgna.story.resources.sims2.ChildHairShocked"),

          //other fields handled by underscore migration
          createMoreSpecificFieldPattern("GREENPANTSFLOWERS", "org.lgna.story.resources.sims2.FemaleChildFullBodyOutfitTShirtPants"),
          createMoreSpecificFieldReplacement("GREEN_PANTS_SUNFLOWER", "org.lgna.story.resources.sims2.FemaleChildFullBodyOutfitTShirtPants"),

          //mail delivery changes class name as well as constant
          createMoreSpecificFieldPattern("STANDARDBLUE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitMailDelivery"),
          createMoreSpecificFieldReplacement("BLUE", "org.lgna.story.resources.sims2.FemaleAdultFullBodyOutfitDeliveryPerson"),
          createMoreSpecificFieldPattern("STANDARDBLUE", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitMailDelivery"),
          createMoreSpecificFieldReplacement("BLUE", "org.lgna.story.resources.sims2.MaleAdultFullBodyOutfitDeliveryPerson")

      ),

      new TextMigration(
          new Version("3.1.69.0.0"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.CaveResource"),
          createMoreSpecificFieldReplacement("DEFAULT_UNDERWATER", "org.lgna.story.resources.prop.CaveResource"),

          createMoreSpecificFieldPattern("DESERT", "org.lgna.story.resources.prop.CliffWallResource"),
          createMoreSpecificFieldReplacement("DEFAULT_DESERT", "org.lgna.story.resources.prop.CliffWallResource"),

          createMoreSpecificFieldPattern("MARS", "org.lgna.story.resources.prop.CliffWallResource"),
          createMoreSpecificFieldReplacement("DEFAULT_MARS", "org.lgna.story.resources.prop.CliffWallResource")
      ),

      new TextMigration(
          new Version("3.1.70.0.0"),

          createMoreSpecificFieldPattern("STRAIGHT1", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT1_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT2", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT2_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT3", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT3_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT4", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT4_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE1", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE1_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE2", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE2_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE3", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE3_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("CURVE4", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("CURVE4_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW1", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW1_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW2", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW2_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("BOW3", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW3_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("SHARP_BEND", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("SHARP_BEND_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          //note: possible duplicate
          createMoreSpecificFieldPattern("BOW1_RIVERBANK3", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("BOW1_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),

          createMoreSpecificFieldPattern("STRAIGHT1_RIVERBANK2", "org.lgna.story.resources.prop.RiverPieceResource"),
          createMoreSpecificFieldReplacement("STRAIGHT1_BLUE", "org.lgna.story.resources.prop.RiverPieceResource"),
          //

          createMoreSpecificFieldPattern("WOODEN_BOAT", "org.lgna.story.resources.aircraft.WoodenBoatResource"),
          createMoreSpecificFieldReplacement("WOODEN_BOAT", "org.lgna.story.resources.watercraft.WoodenBoatResource"),

          "name=\"org.lgna.story.resources.aircraft.WoodenBoatResource",
          "name=\"org.lgna.story.resources.watercraft.WoodenBoatResource",

          createMoreSpecificFieldPattern("SAILBOAT", "org.lgna.story.resources.aircraft.SailboatResource"),
          createMoreSpecificFieldReplacement("SAILBOAT", "org.lgna.story.resources.watercraft.SailboatResource"),

          "name=\"org.lgna.story.resources.aircraft.SailboatResource",
          "name=\"org.lgna.story.resources.watercraft.SailboatResource"
      ),

      new TextMigration(
          new Version("3.1.85.0.0"),

          //<note: moved from 68 - 69 migration when a world with the UFO discovered with old resource>
          "name=\"org.lgna.story.resources.prop.HelicopterResource",
          "name=\"org.lgna.story.resources.prop.HelicopterPropResource",

          createMoreSpecificFieldPattern("UFO", "org.lgna.story.resources.prop.UFOResource"),
          createMoreSpecificFieldReplacement("U_F_O_PROP", "org.lgna.story.resources.prop.UFOPropResource"),

          //added for older projects
          //todo: do others require this?
          "name=\"org.lgna.story.resources.prop.UFOResource",
          "name=\"org.lgna.story.resources.prop.UFOPropResource",
          //

          "name=\"org.lgna.story.resources.prop.PirateShipResource",
          "name=\"org.lgna.story.resources.prop.PirateShipPropResource",

          "name=\"org.lgna.story.resources.prop.FishingBoatResource",
          "name=\"org.lgna.story.resources.prop.FishingBoatPropResource",

          "name=\"org.lgna.story.resources.prop.SubmarineResource",
          "name=\"org.lgna.story.resources.prop.SubmarinePropResource",

          //</note>

          "org.lgna.story.event.ComesIntoViewEvent",
          "org.lgna.story.event.EnterViewEvent",

          "org.lgna.story.event.LeavesViewEvent",
          "org.lgna.story.event.ExitViewEvent",

          "getForegroundMovable",
          "getForegroundModel",

          "getBackgroundMovable",
          "getBackgroundModel",

          "edu.cmu.cs.dennisc.matt.EndOcclusionEvent",
          "org.lgna.story.event.EndOcclusionEvent"

      ),

      new TextMigration(
          new Version("3.1.92.0.0")
      ),

      new TextMigration(
          new Version("3.1.93.0.0"),

          createMoreSpecificFieldPattern("WALNUT_DOOR_WALNUT_WALNUT", "org.lgna.story.resources.prop.BiotechStationResource"),
          createMoreSpecificFieldReplacement("BIOTECH_STATION", "org.lgna.story.resources.prop.BiotechStationResource"),

          createMoreSpecificFieldPattern("WALNUT_DOOR_WALNUT_LIGHT_WOOD", "org.lgna.story.resources.prop.BiotechStationResource"),
          createMoreSpecificFieldReplacement("BIOTECH_STATION", "org.lgna.story.resources.prop.BiotechStationResource"),

          createMoreSpecificFieldPattern("WALNUT_DOOR_WALNUT_ORANGE", "org.lgna.story.resources.prop.BiotechStationResource"),
          createMoreSpecificFieldReplacement("BIOTECH_STATION", "org.lgna.story.resources.prop.BiotechStationResource"),

          createMoreSpecificFieldPattern("WALNUT_DOOR_WALNUT_BLUE", "org.lgna.story.resources.prop.BiotechStationResource"),
          createMoreSpecificFieldReplacement("BIOTECH_STATION", "org.lgna.story.resources.prop.BiotechStationResource"),

          createMoreSpecificFieldPattern("WALNUT_DOOR_WALNUT_PINK", "org.lgna.story.resources.prop.BiotechStationResource"),
          createMoreSpecificFieldReplacement("BIOTECH_STATION", "org.lgna.story.resources.prop.BiotechStationResource"),

          createMoreSpecificFieldPattern("BASIC", "org.lgna.story.resources.prop.BiotechStationResource"),
          createMoreSpecificFieldReplacement("BIOTECH_STATION", "org.lgna.story.resources.prop.BiotechStationResource"),

          createMoreSpecificFieldPattern("FANCY", "org.lgna.story.resources.prop.BiotechStationResource"),
          createMoreSpecificFieldReplacement("BIOTECH_STATION", "org.lgna.story.resources.prop.BiotechStationResource"),

          //The textbook version needs to not remove these models, so for this branch leave this commented out.
          //          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.biped.BlackCatResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.PandaResource"),
          //
          //          "name=\"org.lgna.story.resources.biped.BlackCatResource",
          //          "name=\"org.lgna.story.resources.biped.AliceResource",
          //
          //          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.biped.PumpkinHeadResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.PandaResource"),
          //
          //          createMoreSpecificFieldPattern("HEADLESS", "org.lgna.story.resources.biped.PumpkinHeadResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.PandaResource"),
          //
          //          "name=\"org.lgna.story.resources.biped.PumpkinHeadResource",
          //          "name=\"org.lgna.story.resources.biped.PandaResource",
          //

          //note: this is a doomed migration
          ////"name=\"org.lgna.story.resources.prop.TrainEngineResource",
          ////"name=\"org.lgna.story.resources.train.TrainEngineResource",
          //
          //          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.biped.GhostResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.ThorResource"),
          //
          //          createMoreSpecificFieldPattern("SHEET_GHOST", "org.lgna.story.resources.biped.GhostResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.ThorResource"),
          //
          //          createMoreSpecificFieldPattern("SHEET_GHOST_SHEET_TRANSPARENT", "org.lgna.story.resources.biped.GhostResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.ThorResource"),
          //
          //          "name=\"org.lgna.story.resources.biped.GhostResource",
          //          "name=\"org.lgna.story.resources.biped.ThorResource",
          //

          //note: this is a doomed migration
          ////"name=\"org.lgna.story.resources.prop.TrainCarResource",
          ////"name=\"org.lgna.story.resources.train.TrainCarResource",

          //
          //          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.TunnelResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TentResource"),
          //
          //          "name=\"org.lgna.story.resources.prop.TunnelResource",
          //          "name=\"org.lgna.story.resources.prop.TentResource",
          //
          //          createMoreSpecificFieldPattern("WITH_HAT", "org.lgna.story.resources.biped.SkeletonResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.YetiResource"),
          //
          //          createMoreSpecificFieldPattern("DEFAULT_SKELETON", "org.lgna.story.resources.biped.SkeletonResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.YetiResource"),
          //
          //          createMoreSpecificFieldPattern("DEFAULT_TOP_HAT", "org.lgna.story.resources.biped.SkeletonResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.YetiResource"),
          //
          //          "name=\"org.lgna.story.resources.biped.SkeletonResource",
          //          "name=\"org.lgna.story.resources.biped.YetiResource",
          //
          //          createMoreSpecificFieldPattern("DIFFUSE", "org.lgna.story.resources.prop.FirTreeTrunkResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TreeTrunkResource"),
          //
          //          createMoreSpecificFieldPattern("SKELETON", "org.lgna.story.resources.prop.FirTreeTrunkResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TreeTrunkResource"),
          //
          //          createMoreSpecificFieldPattern("TOP_HAT", "org.lgna.story.resources.prop.FirTreeTrunkResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.TreeTrunkResource"),
          //
          //          "name=\"org.lgna.story.resources.prop.FirTreeTrunkResource",
          //          "name=\"org.lgna.story.resources.prop.TreeTrunkResource",
          //
          //          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.biped.BatResource"),
          //          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.YetiResource"),
          //
          //          "name=\"org.lgna.story.resources.biped.BatResource",
          //          "name=\"org.lgna.story.resources.biped.YetiResource",

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.biped.AsuraResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.biped.AlienResource"),

          "name=\"org.lgna.story.resources.biped.AsuraResource",
          "name=\"org.lgna.story.resources.biped.AlienResource",

          createMoreSpecificFieldPattern("CHEAP", "org.lgna.story.resources.prop.TelevisionRemoteResource"),
          createMoreSpecificFieldReplacement("TELEVISION_REMOTE", "org.lgna.story.resources.prop.TelevisionRemoteResource"),

          createJointIdPattern("TAIL", "biped.BigBadWolfResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.BigBadWolfResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "biped.BigBadWolfResource"),
          createJointIdReplacement("TAIL_2"),
          createJointIdPattern("TAIL_4", "biped.BigBadWolfResource"),
          createJointIdReplacement("TAIL_3"),

          createJointIdPattern("TAIL", "biped.BunnyResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.BunnyResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "biped.BunnyResource"),
          createJointIdReplacement("TAIL_2"),

          createJointIdPattern("TAIL", "biped.CheshireCatResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.CheshireCatResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "biped.CheshireCatResource"),
          createJointIdReplacement("TAIL_2"),
          createJointIdPattern("TAIL_4", "biped.CheshireCatResource"),
          createJointIdReplacement("TAIL_3"),
          createJointIdPattern("TAIL_5", "biped.CheshireCatResource"),
          createJointIdReplacement("TAIL_4"),

          createJointIdPattern("TAIL", "biped.GoldenMonkeyResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.GoldenMonkeyResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "biped.GoldenMonkeyResource"),
          createJointIdReplacement("TAIL_2"),
          createJointIdPattern("JOINT_4", "biped.GoldenMonkeyResource"),
          createJointIdReplacement("TAIL_3"),

          createJointIdPattern("TAIL", "biped.HareResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.HareResource"),
          createJointIdReplacement("TAIL_1"),

          createJointIdPattern("TAIL", "biped.MandrilResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.MandrilResource"),
          createJointIdReplacement("TAIL_1"),

          createJointIdPattern("TAIL", "biped.MonkeyKingResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.MonkeyKingResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "biped.MonkeyKingResource"),
          createJointIdReplacement("TAIL_2"),
          createJointIdPattern("TAIL_4", "biped.MonkeyKingResource"),
          createJointIdReplacement("TAIL_3"),

          createJointIdPattern("JAW_1", "biped.PandaResource"),
          createJointIdReplacement("MOUTH_TIP"),

          createJointIdPattern("TAIL", "biped.StuffedTigerResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "biped.StuffedTigerResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "biped.StuffedTigerResource"),
          createJointIdReplacement("TAIL_2"),
          createJointIdPattern("TAIL_4", "biped.StuffedTigerResource"),
          createJointIdReplacement("TAIL_3"),
          createJointIdPattern("TAIL_5", "biped.StuffedTigerResource"),
          createJointIdReplacement("TAIL_4"),

          createJointIdPattern("NECK", "FlyerResource"),
          createJointIdReplacement("NECK_0"),
          createJointIdPattern("NECK_2", "FlyerResource"),
          createJointIdReplacement("NECK_1"),

          createJointIdPattern("TAIL", "FlyerResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "FlyerResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "FlyerResource"),
          createJointIdReplacement("TAIL_2"),

          createJointIdPattern("LEFT_PLUMAGE_1", "flyer.PeacockResource"),
          createJointIdReplacement("PLUMAGE_LEFT_TIP"),

          createJointIdPattern("RIGHT_PLUMAGE_1", "flyer.PeacockResource"),
          createJointIdReplacement("PLUMAGE_RIGHT_TIP"),

          createJointIdPattern("TAIL", "QuadrupedResource"),
          createJointIdReplacement("TAIL_0"),
          createJointIdPattern("TAIL_2", "QuadrupedResource"),
          createJointIdReplacement("TAIL_1"),
          createJointIdPattern("TAIL_3", "QuadrupedResource"),
          createJointIdReplacement("TAIL_2"),
          createJointIdPattern("TAIL_4", "QuadrupedResource"),
          createJointIdReplacement("TAIL_3"),

          createJointIdPattern("JAW_1", "quadruped.AbyssinianCatResource"),
          createJointIdReplacement("LOWER_LIP"),
          createJointIdPattern("JAW_1", "quadruped.AlienRobotResource"),
          createJointIdReplacement("MOUTH_TIP"),
          createJointIdPattern("JAW_1", "quadruped.CaimanResource"),
          createJointIdReplacement("MOUTH_TIP"),
          createJointIdPattern("JAW_1", "quadruped.CowResource"),
          createJointIdReplacement("MOUTH_TIP"),
          createJointIdPattern("JAW_1", "quadruped.DragonResource"),
          createJointIdReplacement("MOUTH_TIP"),
          createJointIdPattern("JAW_1", "quadruped.DragonBabyResource"),
          createJointIdReplacement("MOUTH_TIP"),
          createJointIdPattern("JAW_1", "quadruped.PeccaryResource"),
          createJointIdReplacement("MOUTH_TIP"),
          createJointIdPattern("JAW_1", "quadruped.YaliResource"),
          createJointIdReplacement("MOUTH_TIP"),

          createJointIdPattern("TONGUE", "quadruped.CoyoteResource"),
          createJointIdReplacement("TONGUE_0"),
          createJointIdPattern("TONGUE_2", "quadruped.CoyoteResource"),
          createJointIdReplacement("TONGUE_1"),
          createJointIdPattern("TONGUE_3", "quadruped.CoyoteResource"),
          createJointIdReplacement("TONGUE_2"),
          createJointIdPattern("TONGUE_4", "quadruped.CoyoteResource"),
          createJointIdReplacement("TONGUE_3"),

          createJointIdPattern("TRUNK_1", "quadruped.ElephantResource"),
          createJointIdReplacement("TRUNK_0"),
          createJointIdPattern("TRUNK_2", "quadruped.ElephantResource"),
          createJointIdReplacement("TRUNK_1"),
          createJointIdPattern("TRUNK_3", "quadruped.ElephantResource"),
          createJointIdReplacement("TRUNK_2"),
          createJointIdPattern("TRUNK_4", "quadruped.ElephantResource"),
          createJointIdReplacement("TRUNK_3"),
          createJointIdPattern("TRUNK_5", "quadruped.ElephantResource"),
          createJointIdReplacement("TRUNK_4"),
          createJointIdPattern("TRUNK_6", "quadruped.ElephantResource"),
          createJointIdReplacement("TRUNK_5"),

          createJointIdPattern("TONGUE_1", "quadruped.HornedLizardResource"),
          createJointIdReplacement("TONGUE_0"),
          createJointIdPattern("TONGUE_2", "quadruped.HornedLizardResource"),
          createJointIdReplacement("TONGUE_1"),
          createJointIdPattern("TONGUE_3", "quadruped.HornedLizardResource"),
          createJointIdReplacement("TONGUE_2"),
          createJointIdPattern("TONGUE_4", "quadruped.HornedLizardResource"),
          createJointIdReplacement("TONGUE_3"),

          createJointIdPattern("TONGUE", "quadruped.YakResource"),
          createJointIdReplacement("TONGUE_0"),
          createJointIdPattern("TONGUE_2", "quadruped.YakResource"),
          createJointIdReplacement("TONGUE_1"),
          createJointIdPattern("TONGUE_3", "quadruped.YakResource"),
          createJointIdReplacement("TONGUE_2"),
          createJointIdPattern("TONGUE_4", "quadruped.YakResource"),
          createJointIdReplacement("TONGUE_3"),

          createJointIdPattern("TRUNK", "quadruped.YaliResource"),
          createJointIdReplacement("TRUNK_0"),
          createJointIdPattern("TRUNK_2", "quadruped.YaliResource"),
          createJointIdReplacement("TRUNK_1"),
          createJointIdPattern("TRUNK_3", "quadruped.YaliResource"),
          createJointIdReplacement("TRUNK_2"),
          createJointIdPattern("TRUNK_4", "quadruped.YaliResource"),
          createJointIdReplacement("TRUNK_3"),
          createJointIdPattern("TRUNK_5", "quadruped.YaliResource"),
          createJointIdReplacement("TRUNK_4"),
          createJointIdPattern("TRUNK_6", "quadruped.YaliResource"),
          createJointIdReplacement("TRUNK_5"),

          createJointIdPattern("LEFT_1", "prop.NavajoBlanketResource"),
          createJointIdReplacement("LEFT_0"),
          createJointIdPattern("LEFT_2", "prop.NavajoBlanketResource"),
          createJointIdReplacement("LEFT_1"),
          createJointIdPattern("LEFT_3", "prop.NavajoBlanketResource"),
          createJointIdReplacement("LEFT_2"),
          createJointIdPattern("RIGHT_1", "prop.NavajoBlanketResource"),
          createJointIdReplacement("RIGHT_0"),
          createJointIdPattern("RIGHT_2", "prop.NavajoBlanketResource"),
          createJointIdReplacement("RIGHT_1"),
          createJointIdPattern("RIGHT_3", "prop.NavajoBlanketResource"),
          createJointIdReplacement("RIGHT_2"),

          //createJointIdPattern("STRING_1", "prop.PrayerFlagsResource"),
          //createJointIdReplacement("STRING_1"),
          createJointIdPattern("FLAG_5", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_05"),
          createJointIdPattern("FLAG_6", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_06"),
          createJointIdPattern("FLAG_7", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_07"),
          createJointIdPattern("FLAG_8", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_08"),
          createJointIdPattern("FLAG_9", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_09"),

          createJointIdPattern("STRING_2", "prop.PrayerFlagsResource"),
          createJointIdReplacement("STRING_0"),
          createJointIdPattern("FLAG", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_00"),
          createJointIdPattern("FLAG_1", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_01"),
          createJointIdPattern("FLAG_2", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_02"),
          createJointIdPattern("FLAG_3", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_03"),
          createJointIdPattern("FLAG_4", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_04"),

          createJointIdPattern("STRING_3", "prop.PrayerFlagsResource"),
          createJointIdReplacement("STRING_2"),
          createJointIdPattern("FLAG_10", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_10"),
          createJointIdPattern("FLAG_11", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_11"),
          createJointIdPattern("FLAG_12", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_12"),
          createJointIdPattern("FLAG_13", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_13"),
          createJointIdPattern("FLAG_14", "prop.PrayerFlagsResource"),
          createJointIdReplacement("FLAG_14"),

          createJointIdPattern("TAIL_5", "quadruped.YaliResource"),
          createJointIdReplacement("TAIL_4"),

          createJointIdPattern("TAIL_5", "quadruped.DalmatianResource"),
          createJointIdReplacement("TAIL_4")
      ),
      new TextMigration(
          new Version("3.2.108.0.0")

          ),
      createVersion3_2_110TextMigration(),
      new TextMigration(
          new Version("3.2.111.0.0"),

          createMoreSpecificFieldPattern("BLEACHERS", "org.lgna.story.resources.prop.CircusBleachersResource"),
          createMoreSpecificFieldReplacement("DEFAULT_BLEACHERS", "org.lgna.story.resources.prop.CircusBleachersResource"),

          createMoreSpecificFieldPattern("BONE_PILE", "org.lgna.story.resources.prop.BonesResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.BonePileResource"),

          "org.lgna.story.resources.prop.BonesResource",
          "org.lgna.story.resources.prop.BonePileResource",

          createMoreSpecificFieldPattern("SHORT", "org.lgna.story.resources.prop.WychElmResource"),
          createMoreSpecificFieldReplacement("DEFAULT", "org.lgna.story.resources.prop.WychElmResource")
          ),
      new TextMigration(
          new Version("3.2.112.0.0")
          ),
      new TextMigration(
          new Version("3.2.113.0.0"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.FirTreeTrunkMirrorResource"),
          createMoreSpecificFieldReplacement("MIRROR", "org.lgna.story.resources.prop.FirTreeTrunkResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.FirTreeTrunkTallResource"),
          createMoreSpecificFieldReplacement("TALL", "org.lgna.story.resources.prop.FirTreeTrunkResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.FirTreeTrunkTallMirrorResource"),
          createMoreSpecificFieldReplacement("TALL_MIRROR", "org.lgna.story.resources.prop.FirTreeTrunkResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.FirTreeTrunkSnowResource"),
          createMoreSpecificFieldReplacement("SNOW", "org.lgna.story.resources.prop.FirTreeTrunkResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.FirTreeTrunkSnowMirrorResource"),
          createMoreSpecificFieldReplacement("SNOW_MIRROR", "org.lgna.story.resources.prop.FirTreeTrunkResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.FirTreeTrunkSnowTallResource"),
          createMoreSpecificFieldReplacement("SNOW_TALL", "org.lgna.story.resources.prop.FirTreeTrunkResource"),

          createMoreSpecificFieldPattern("DEFAULT", "org.lgna.story.resources.prop.FirTreeTrunkSnowTallMirrorResource"),
          createMoreSpecificFieldReplacement("SNOW_TALL_MIRROR", "org.lgna.story.resources.prop.FirTreeTrunkResource"),


          "org.lgna.story.resources.prop.FirTreeTrunkMirrorResource",
          "org.lgna.story.resources.prop.FirTreeTrunkResource",

          "org.lgna.story.resources.prop.FirTreeTrunkTallResource",
          "org.lgna.story.resources.prop.FirTreeTrunkResource",

          "org.lgna.story.resources.prop.FirTreeTrunkTallMirrorResource",
          "org.lgna.story.resources.prop.FirTreeTrunkResource",

          "org.lgna.story.resources.prop.FirTreeTrunkSnowResource",
          "org.lgna.story.resources.prop.FirTreeTrunkResource",

          "org.lgna.story.resources.prop.FirTreeTrunkSnowMirrorResource",
          "org.lgna.story.resources.prop.FirTreeTrunkResource",

          "org.lgna.story.resources.prop.FirTreeTrunkSnowTallResource",
          "org.lgna.story.resources.prop.FirTreeTrunkResource",

          "org.lgna.story.resources.prop.FirTreeTrunkSnowTallMirrorResource",
          "org.lgna.story.resources.prop.FirTreeTrunkResource",

          "FirTreeTrunkSnowTallMirror",
          "FirTreeTrunk",

          "FirTreeTrunkSnowTall",
          "FirTreeTrunk",

          "FirTreeTrunkTallMirror",
          "FirTreeTrunk",

          "FirTreeTrunkSnowMirror",
          "FirTreeTrunk",

          "FirTreeTrunkSnow",
          "FirTreeTrunk",

          "FirTreeTrunkMirror",
          "FirTreeTrunk",

          "FirTreeTrunkTall",
          "FirTreeTrunk",



          createMoreSpecificFieldPattern("SQUARE", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("SQUARE_DESERT", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("CRESCENT", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("CRESCENT_DESERT", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("OVAL", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("OVAL_DESERT", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("BLOB", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("BLOB_DESERT", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("SQUARE_DRYGRASS", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("SQUARE_DRY_GRASS", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("SQUARE_FORESTFLOOR", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("SQUARE_FOREST_FLOOR", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("SQUARE_FORESTFLOORBROWN", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("SQUARE_FOREST_FLOOR_BROWN", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("SQUARE_FORESTFLOORRED", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("SQUARE_FOREST_FLOOR_RED", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("CRESCENT_DRYGRASS", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("CRESCENT_DRY_GRASS", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("CRESCENT_FORESTFLOOR", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("CRESCENT_FOREST_FLOOR", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("CRESCENT_FORESTFLOORBROWN", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("CRESCENT_FOREST_FLOOR_BROWN", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("CRESCENT_FORESTFLOORRED", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("CRESCENT_FOREST_FLOOR_RED", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("BLOB_DRYGRASS", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("BLOB_DRY_GRASS", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("BLOB_FORESTFLOOR", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("BLOB_FOREST_FLOOR", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("BLOB_FORESTFLOORBROWN", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("BLOB_FOREST_FLOOR_BROWN", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("BLOB_FORESTFLOORRED", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("BLOB_FOREST_FLOOR_RED", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("OVAL_DRYGRASS", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("OVAL_DRY_GRASS", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("OVAL_FORESTFLOOR", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("OVAL_FOREST_FLOOR", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("OVAL_FORESTFLOORBROWN", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("OVAL_FOREST_FLOOR_BROWN", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("OVAL_FORESTFLOORRED", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("OVAL_FOREST_FLOOR_RED", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_DRYGRASS", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_DRY_GRASS", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_FORESTFLOOR", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_FOREST_FLOOR", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_FORESTFLOORBROWN", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_FOREST_FLOOR_BROWN", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_FORESTFLOORRED", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_FOREST_FLOOR_RED", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_OCEANNIGHT", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_OCEAN_NIGHT", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_OVAL_DRYGRASS", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_OVAL_DRY_GRASS", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_OVAL_FORESTFLOOR", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_OVAL_FOREST_FLOOR", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_OVAL_FORESTFLOORBROWN", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_OVAL_FOREST_FLOOR_BROWN", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_OVAL_FORESTFLOORRED", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_OVAL_FOREST_FLOOR_RED", "org.lgna.story.resources.prop.SandDunesResource"),

          createMoreSpecificFieldPattern("FLAT_OVAL_OCEANNIGHT", "org.lgna.story.resources.prop.SandDunesResource"),
          createMoreSpecificFieldReplacement("FLAT_OVAL_OCEAN_NIGHT", "org.lgna.story.resources.prop.SandDunesResource"),

          "org.lgna.story.resources.prop.SandDunesResource",
          "org.lgna.story.resources.prop.TerrainResource"

          ),

      new TextMigration(
          new Version("3.3.0.0.0"),

          "INDIA_BRICK_D",
          "GRAY",

          "INDIA_LIGHT_BRICK_D",
          "GOLD",

          "INDIA_LIGHTEST_BRICK_D",
          "SAND",

          "INDIA_MED_BRICK_D",
          "RED",


          "INDIA_WATER_TANK_LIGHTEST",
          "SAND",

          "INDIA_WATER_TANK_LIGHT",
          "GOLD",

          "INDIA_WATER_TANK_MED",
          "RED",

          "INDIA_WATER_TANK",
          "GRAY"
          ),

      new TextMigration(new Version("3.4.0.0"),
          "<method isVarArgs=\"false\" name=\"getModelAtMouseLocation\"><declaringClass name=\"org.lgna.story.event.MouseClickEvent\"/><parameters/></method>",
          "<method isVarArgs=\"false\" name=\"getModelAtMouseLocation\"><declaringClass name=\"org.lgna.story.event.MouseClickOnObjectEvent\"/><parameters/></method>"
          ),

      new TextMigration(new Version("3.9.0.0"),
          "<method isVarArgs=\"true\" name=\"getDistanceTo\"><declaringClass name=\"org.lgna.story.STurnable\"/><parameters><type name=\"org.lgna.story.STurnable\"/>",
          "<method isVarArgs=\"true\" name=\"getDistanceTo\"><declaringClass name=\"org.lgna.story.STurnable\"/><parameters><type name=\"org.lgna.story.SThing\"/>"
      )

    };
  }

  // @formatter:on

  private TextMigrationRegistryLateVersions() {
  }
}
