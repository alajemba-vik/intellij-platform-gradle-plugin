// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.reports

import org.gradle.api.model.ObjectFactory
import org.gradle.api.reporting.ReportContainer
import org.gradle.api.tasks.Internal
import javax.inject.Inject

interface VerificationReports : ReportContainer<BaseReport> {
    @get:Internal
    val txt: BaseReport
}



open class VerificationReportContainer @Inject constructor(objectFactory: ObjectFactory) :
    BaseReportContainer<BaseReport>(objectFactory, BaseReport::class.java),
    VerificationReports {

    init {
        add(objectFactory.newInstance(DefaultSingleFileReport::class.java, "txt"))
    }

    @get:Internal
    override val txt: BaseReport
        get() = getByName("txt")

}