// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.reports

import groovy.lang.Closure
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.provider.Property
import org.gradle.api.reporting.Report
import org.gradle.api.tasks.OutputFile
import org.gradle.util.internal.ConfigureUtil


/**
 * A copy of internal class [org.gradle.api.reporting.internal.SimpleReport]
 */
abstract class BaseReport(
    private val name: String,
    private val outputType: Report.OutputType
) : Report {
    override fun getDisplayName(): String = name

    override fun getName(): String = name

    @OutputFile
    abstract override fun getOutputLocation(): FileSystemLocationProperty<out FileSystemLocation>

    override fun getOutputType(): Report.OutputType = outputType

    override fun configure(cl: Closure<*>?): Report = ConfigureUtil.configureSelf(cl, this)
}