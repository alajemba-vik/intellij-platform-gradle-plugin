// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.plugins.project

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.intellij.platform.gradle.Constants.Plugin.ID
import org.jetbrains.intellij.platform.gradle.tasks.*
import org.jetbrains.intellij.platform.gradle.tasks.companion.ProcessResourcesCompanion
import org.jetbrains.intellij.platform.gradle.utils.Logger

abstract class IntelliJPlatformPlugin : Plugin<Project> {

    private val log = Logger(javaClass)

    override fun apply(project: Project) {
        log.info("Configuring plugin: $ID")

        PatchPluginXmlTask.register(project)

        with(project.plugins) {
            apply(IntelliJPlatformBasePlugin::class)
            apply(IntelliJPlatformModulePlugin::class)
        }

        listOf(
            // Build
            PatchPluginXmlTask,
            ProcessResourcesCompanion,
            BuildSearchableOptionsTask,
            PrepareJarSearchableOptionsTask,
            JarSearchableOptionsTask,
            BuildPluginTask,

            // Test
            TestIdePerformanceTask,

            // Run
            RunIdeTask,

            // Verify
            VerifyPluginTask,
            VerifyPluginSignatureTask,
            VerifyPluginStructureTask,

            // Sign
            SignPluginTask,
            PublishPluginTask,
        ).forEach {
            it.register(project)
        }
    }
}
