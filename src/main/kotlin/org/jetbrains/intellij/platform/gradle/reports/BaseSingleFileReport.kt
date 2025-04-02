// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.reports

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.reporting.Report
import org.gradle.api.tasks.OutputFile
import javax.inject.Inject

interface BaseSingleFileReport : Report {
    @OutputFile
    override fun getOutputLocation(): RegularFileProperty
}



/**
 * Single report of the type file.
 * Reimplementation of [org.gradle.api.reporting.internal.DefaultSingleFileReport].
 */
abstract class DefaultSingleFileReport @Inject constructor(name: String) :
    BaseReport(name, Report.OutputType.FILE), BaseSingleFileReport

