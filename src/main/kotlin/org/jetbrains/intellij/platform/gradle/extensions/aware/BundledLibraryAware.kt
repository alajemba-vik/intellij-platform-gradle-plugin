// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.extensions.aware

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.get
import org.jetbrains.intellij.platform.gradle.Constants.Configurations
import org.jetbrains.intellij.platform.gradle.extensions.DependencyAction
import org.jetbrains.intellij.platform.gradle.extensions.createBundledLibraryDependency
import org.jetbrains.intellij.platform.gradle.utils.Logger

interface BundledLibraryAware : DependencyAware, IntelliJPlatformAware {
    val objects: ObjectFactory
}

/**
 * Adds a dependency on a Jar bundled within the current IntelliJ Platform.
 *
 * @param pathProvider Path to the bundled Jar file, like `lib/testFramework.jar`
 * @param configurationName The name of the configuration to add the dependency to.
 * @param action The action to be performed on the dependency. Defaults to an empty action.
 */
internal fun BundledLibraryAware.addBundledLibrary(
    pathProvider: Provider<String>,
    configurationName: String = Configurations.INTELLIJ_PLATFORM_DEPENDENCIES,
    action: DependencyAction = {},
) = configurations[configurationName].dependencies.addLater(
    pathProvider.map { path ->
        dependencies
            .createBundledLibraryDependency(path, objects, platformPath)
            .apply(action)
    }
).also {
    val log = Logger(javaClass)
    log.warn(
        """
        Do not use `bundledLibrary()` in production, as direct access to the IntelliJ Platform libraries is not recommended.
        
        It should only be used as a workaround in case the IntelliJ Platform Gradle Plugin is not aligned with the latest IntelliJ Platform classpath changes.
        """.trimIndent()
    )
}
