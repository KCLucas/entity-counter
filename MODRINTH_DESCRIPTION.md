# Entity Counter

Look at a block, press **F7**, and see exactly how many entities are standing in it.

![15 armadillos counted through glass](https://raw.githubusercontent.com/KCLucas/entity-counter/master/images/armadillos.png)

## Why

Some farms need an exact number of mobs packed into a single 1x1 space - for example a slime farm that needs exactly 15 armadillos. Once they overlap you cannot count them by eye. Entity Counter shows the number instantly.

## Features

- Panel in the top-left corner with a **picture**, the **name** and the **count** of every entity type in the targeted block
- Total count when several types share the block
- Sees through glass, fences, hoppers and other see-through blocks, so closed chambers are no problem
- Works up to 32 blocks away
- Hotkey is rebindable under *Options → Controls → Key Binds → Entity Counter* (default: **F7**)
- English and German translations

![Several entity types in one block](https://raw.githubusercontent.com/KCLucas/entity-counter/master/images/mixed.png)

## Good to know

- An entity belongs to the block its feet are in, so every entity is counted exactly once.
- Purely client-side. It works on any server and nothing has to be installed there.
- Requires [Fabric API](https://modrinth.com/mod/fabric-api).
