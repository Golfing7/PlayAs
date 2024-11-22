package com.golfing8.playas;

import com.golfing8.kcommon.KPlugin;
import com.golfing8.kcommon.module.ModuleInfo;
import lombok.Getter;

/**
 * Main controlling class for 'PlayAs'
 */
public class PlayAsPlugin extends KPlugin {
    @Getter
    private static PlayAsPlugin instance;

    @Override
    public void onEnableInner() {
        instance = this;
    }
}
