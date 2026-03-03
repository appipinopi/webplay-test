# Usage
You can use this template repository to help you create your native bluemap-addon.
Simply clone this repository like explained [here](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template)
and replace all `example`'s you find with your own addons name and group.

To compile, run `./gradlew clean build` which will create the jars in the `./build/libs` folder.

You can then install your addon by placing the `.jar` in bluemaps `packs` folder next to the configuration files.

# Overview
Information for bluemap's addon loader can be found in `./src/main/java/resources/bluemap.addon.json`.
There you configure your addons id (must be unique) and the entrypoint (full class name of your main class).
When BlueMap loads your addon it will use the no-args-constructor of your main class to create an instance, 
and if your main class implements `Runnable` it will execute the `run()` method once.

BlueMap will also load your addon like a resource/data-pack. This means if you need any additional resources like block-models or
textures then you can add them inside the `./src/main/resources` folder just like in a resource/datapack. 

# More resources
- BlueMap-API Usage: https://github.com/BlueMap-Minecraft/BlueMapAPI/wiki
- Information about BlueMap-Specific resources: https://bluemap.bluecolored.de/wiki/customization/Mods.html
- Maven based Addon-Template by TechnicJelle: https://github.com/TechnicJelle/BlueMapNativeAddonTemplate 
