package com.example.exampleaddon;

import de.bluecolored.bluemap.api.BlueMapAPI;

@SuppressWarnings("unused")
public class ExampleAddon implements Runnable {

    @Override
    public void run() {
        // this is called by bluemaps addon-loader once right when the addon got loaded
        // any one-time early initialization goes here

        BlueMapAPI.onEnable(this::onEnable);
        BlueMapAPI.onDisable(this::onDisable);
    }

    public void onEnable(BlueMapAPI api) {
        // anything that should be done when bluemap gets enabled here
        // this can happen multiple times in the addons lifetime, e.g. when bluemap get reloaded
    }

    public void onDisable(BlueMapAPI api) {
        // anything that should be done right before bluemap gets disabled here
        // this can happen multiple times in the addons lifetime, e.g. when bluemap get reloaded
    }

}
