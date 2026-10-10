# Entity Counter

Look at a block, press **F7**, and see exactly how many entities are standing in it.

![15 armadillos counted through glass](https://raw.githubusercontent.com/KCLucas/entity-counter/master/images/armadillos.png)

## Why

Some farms need an exact number of mobs packed into a single 1x1 space - for example a slime farm that needs exactly 15 armadillos. Once they overlap you cannot count them by eye. Entity Counter shows the number instantly.

## How to use

1. Press **F7** to turn the counter on or off.
2. Look at the block (or at a mob standing in it).
3. The panel in the top-left corner lists every entity type in that block with a picture, its name and how many there are. The counted block glows green in the world, so you always see which block is meant.

![Several entity types in one block](https://raw.githubusercontent.com/KCLucas/entity-counter/master/images/mixed.png)

## Which block is counted

You choose which block the counter uses:

| Mode | Counts |
| --- | --- |
| **Automatic** (default) | Sees through glass and other see-through blocks and picks the first block that contains entities. Best for closed farm chambers. |
| **Block you look at** | Exactly the block under your crosshair. |
| **Block in front** | The block in front of the one you look at. |
| **Block behind** | The block directly behind the one you look at. |

![Block in front mode](https://raw.githubusercontent.com/KCLucas/entity-counter/master/images/block-in-front.png)

## Settings

![Settings screen](https://raw.githubusercontent.com/KCLucas/entity-counter/master/images/settings.png)

- **Counted block** - the mode from the table above
- **Highlight counted block** - the green glow, on or off
- **Show coordinates** - show the position of the counted block in the panel
- **Display position** - drag the panel to any spot on the screen

Open the settings through [Mod Menu](https://modrinth.com/mod/modmenu), or bind a key to *Open Settings* under *Options → Controls → Key Binds → Entity Counter*. The F7 key can be changed there too. Mod Menu is optional.

### Moving the display

Click **Move...** in the settings, then drag the panel to where you want it. The background is dimmed and the panel shows an example with three mobs, so you can see how much room it takes.

![Dragging the display to a new position](https://raw.githubusercontent.com/KCLucas/entity-counter/master/images/move.png)

## Good to know

- An entity belongs to the block its feet are in, so every entity is counted exactly once, even if its hitbox reaches into neighbouring blocks.
- Dropped items are listed by their real name and count with their stack size, so a stack of 5 diamonds shows as "Diamond: 5".
- Works up to 32 blocks away.
- The panel hides while the F3 debug screen is open.
- Purely client-side: works on any server, nothing needs to be installed there.
- English and German translations.

Requires [Fabric API](https://modrinth.com/mod/fabric-api).
