// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.tasks

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension
import org.jetbrains.intellij.platform.gradle.reports.BaseReportContainer
import org.jetbrains.intellij.platform.gradle.reports.DefaultDirectoryReport
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.VerificationReportsFormats
import javax.inject.Inject

private const val REPORT_NAME = "report"

/**
 * Container for verification reports generated by the [VerifyPluginTask].
 *
 * Uses Gradle's [org.gradle.api.reporting.Reporting] interface to standardize report handling.
 *
 * It registers a single directory-based report under the name "report".
 * Output formats for this report, such as `html`, `plain`, and `markdown`, is configured through the [formats] property.
 *
 * @param objectFactory Gradle's object factory used to create and register report instances.
 *
 * @see org.jetbrains.intellij.platform.gradle.reports.BaseReportContainer.BaseReport
 * @see org.gradle.api.reporting.ReportContainer
 * @see DefaultDirectoryReport
 * @see VerifyPluginTask
 */
open class VerifyReportContainer @Inject constructor(objectFactory: ObjectFactory) :
    BaseReportContainer<DefaultDirectoryReport>(objectFactory, DefaultDirectoryReport::class.java) {

    init {
        add(objectFactory.newInstance(DefaultDirectoryReport::class.java, REPORT_NAME))
    }

    @get:Internal
     val report: DefaultDirectoryReport
        get() = getByName(REPORT_NAME)

    /**
     * Controls the output formats of the verification report.
     *
     * Default value: [IntelliJPlatformExtension.PluginVerification.verificationReportsFormats]
     *
     * @see VerificationReportsFormats
     * @see IntelliJPlatformExtension.PluginVerification.verificationReportsFormats
     */
    @get:Internal
    val formats: ListProperty<VerificationReportsFormats> =
        objectFactory.listProperty(VerificationReportsFormats::class.java)

}