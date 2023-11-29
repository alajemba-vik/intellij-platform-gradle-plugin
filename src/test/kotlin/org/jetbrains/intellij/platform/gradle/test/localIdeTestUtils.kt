// Copyright 2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.test

import com.jetbrains.plugin.structure.base.utils.forceDeleteIfExists
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.DEFAULT_INTELLIJ_REPOSITORY
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Downloads and extracts IDE for the tests using local IDE installation. IDEs are downloaded from [DEFAULT_INTELLIJ_REPOSITORY].
 *
 * @param localIdesPath directory to store local IDE
 * @param releasePath IDE path relative to [DEFAULT_INTELLIJ_REPOSITORY]/releases, e.g. `"com/jetbrains/intellij/idea/ideaIC/2022.1.4/ideaIC-2022.1.4.zip"`
 */
fun createLocalIdeIfNotExists(localIdesPath: Path, releasePath: String): String {
    val fileName = releasePath.substringAfterLast('/')
    val localIdeZipPath = localIdesPath.resolve(fileName)
    val localIdeDirPathString = localIdeZipPath.toString().removeSuffix(".zip")
    if (Path(localIdeDirPathString).exists()) {
        return localIdeDirPathString
    }
    if (!localIdesPath.exists()) {
        localIdesPath.createDirectories()
    }

    URL("$DEFAULT_INTELLIJ_REPOSITORY/releases/$releasePath").openStream().use {
        Files.copy(it, localIdeZipPath, StandardCopyOption.REPLACE_EXISTING)
    }
    localIdeZipPath.toFile().unzip()
    localIdeZipPath.forceDeleteIfExists()

    return localIdeDirPathString
}

private fun File.unzip() {
    val unzipDirPath = this.path.removeSuffix(".zip")
    ZipFile(this).use { zip ->
        zip
            .entries()
            .asSequence()
            .mapNotNull {
                val outputFile = File("$unzipDirPath/${it.name}")
                outputFile.parentFile?.run {
                    if (!exists()) mkdirs()
                }
                if (!it.isDirectory) Pair(it, outputFile) else null
            }
            .forEach { (entry, output) ->
                zip.getInputStream(entry).use { input ->
                    output.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
    }
}
