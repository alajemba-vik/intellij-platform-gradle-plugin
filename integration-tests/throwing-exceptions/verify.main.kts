#!/usr/bin/env -S kotlin -J-ea

@file:Import("../verify.utils.kts")

with(__FILE__.toPath()) {
    runGradleTask("buildPlugin").let { logs ->
        logs containsText "FAILURE: Build failed with an exception."
        logs containsText " > Both 'intellij.localPath' and 'intellij.version' are specified, but one of these is allowed to be present."
        logs containsText "BUILD FAILED"
    }
}
