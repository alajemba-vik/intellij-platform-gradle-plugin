// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.tasks

import com.jetbrains.plugin.structure.base.utils.outputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.named
import org.jetbrains.intellij.platform.gradle.*
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.AndroidStudio
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.PLUGIN_GROUP_NAME
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.Tasks
import org.jetbrains.intellij.platform.gradle.model.AndroidStudioReleases
import org.jetbrains.intellij.platform.gradle.model.ProductsReleases
import org.jetbrains.intellij.platform.gradle.model.XmlExtractor
import org.jetbrains.intellij.platform.gradle.tasks.base.PlatformVersionAware
import java.util.*

/**
 * List all available IntelliJ-based IDE releases with their updates.
 * The result list is used for testing the plugin with Plugin Verifier using the [RunPluginVerifierTask] task.
 *
 * Plugin Verifier requires a list of the IDEs that will be used for verifying your plugin build against.
 * The availability of the releases may change in time, i.e., due to security issues in one version – which will be later removed and replaced with an updated IDE release.
 *
 * With the [ListProductsReleasesTask] task, it is possible to list the currently available IDEs matching given conditions, like platform types, since/until release versions.
 * Such a list is fetched from the remote updates file: `https://www.jetbrains.com/updates/updates.xml`, parsed and filtered considering the specified [ListProductsReleasesTask.types], [ListProductsReleasesTask.sinceVersion], [ListProductsReleasesTask.untilVersion] (or [ListProductsReleasesTask.sinceBuild], [ListProductsReleasesTask.untilBuild]) properties.
 *
 * The result list is stored within the [outputFile], which is used as a source for the Plugin Verifier if the [RunPluginVerifierTask] task has no [RunPluginVerifierTask.ideVersions] property specified, the output of the [ListProductsReleasesTask] task is used.
 *
 * @see [PrintProductsReleasesTask]
 */
@CacheableTask
abstract class ListProductsReleasesTask : DefaultTask(), PlatformVersionAware {

    /**
     * Path to the products releases update files. By default, one is downloaded from [IntelliJPluginConstants.IDEA_PRODUCTS_RELEASES_URL].
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ideaProductReleasesUpdateFiles: ConfigurableFileCollection

    /**
     * Path to the products releases update files. By default, one is downloaded from [IntelliJPluginConstants.ANDROID_STUDIO_PRODUCTS_RELEASES_URL].
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val androidStudioProductReleasesUpdateFiles: ConfigurableFileCollection

    /**
     * Path to the file, where the output list will be stored.
     *
     * Default value: `File("${project.buildDir}/listProductsReleases.txt")`
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * List the types of IDEs that will be listed in results.
     */
    @get:Input
    @get:Optional
    abstract val types: ListProperty<IntelliJPlatformType>

    /**
     * Lower boundary of the listed results in marketing product version format, like `2020.2.1`.
     * Takes the precedence over [sinceBuild] property.
     *
     * Default value: [IntelliJPluginExtension.version]
     */
    @get:Input
    @get:Optional
    abstract val sinceVersion: Property<String>

    /**
     * Upper boundary of the listed results in product marketing version format, like `2020.2.1`.
     * Takes the precedence over [untilBuild] property.
     *
     * Default value: `null`
     */
    @get:Input
    @get:Optional
    abstract val untilVersion: Property<String>

    /**
     * Lower boundary of the listed results in build number format, like `192`.
     *
     * Default value: [IntelliJPluginExtension.version]
     */
    @get:Input
    @get:Optional
    abstract val sinceBuild: Property<String>

    /**
     * Upper boundary of the listed results in build number format, like `192`.
     *
     * Default value: `null`
     */
    @get:Input
    @get:Optional
    abstract val untilBuild: Property<String>

    /**
     * Channels that product updates will be filtered with.
     *
     * Default value: `EnumSet.allOf(ListProductsReleasesTask.Channel)`
     */
    @get:Input
    @get:Optional
    abstract val releaseChannels: SetProperty<Channel>

    private val context = logCategory()

    init {
        group = PLUGIN_GROUP_NAME
        description = "List all available IntelliJ-based IDE releases with their updates."
    }

    @TaskAction
    fun listProductsReleases() {
        val releases = XmlExtractor<ProductsReleases>(context).let { extractor ->
            ideaProductReleasesUpdateFiles
                .files
                .map { it.toPath() }
                .mapNotNull(extractor::fetch)
        }
        val androidStudioReleases = XmlExtractor<AndroidStudioReleases>(context).let { extractor ->
            androidStudioProductReleasesUpdateFiles
                .files
                .map { it.toPath() }
                .mapNotNull(extractor::fetch)
        }

        val since = sinceVersion.orNull
            .or { sinceBuild.get() }
            .run(Version::parse)

        val until = untilVersion.orNull
            .or {
                untilBuild.orNull
                    .takeUnless { it.isNullOrBlank() || sinceVersion.isPresent }
            }
            ?.replace("*", "99999")
            ?.run(Version::parse)

        val channels = releaseChannels.get()

        fun testVersion(version: Version?, build: Version?): Boolean {
            val a = when (since.major) {
                in 100..999 -> build
                else -> version
            }
            val b = when (until?.major) {
                in 100..999 -> build
                else -> version
            }

            return a != null && b != null && a >= since && (until == null || b <= until)
        }

        val result = releases.map(ProductsReleases::products).flatten().asSequence()
            .flatMap { product -> product.codes.map { it to product }.asSequence() }
            .filter { (type) ->
                runCatching { IntelliJPlatformType.fromCode(type) }
                    .map { types.get().contains(it) }
                    .getOrElse { false }
            }
            .flatMap { (type, product) -> product.channels.map { type to it }.asSequence() }
            .filter { (_, channel) -> channels.contains(Channel.valueOf(channel.status.uppercase())) }
            .flatMap { (type, channel) ->
                channel.builds.map {
                    type to (it.version.run(Version::parse) to it.number.run(Version::parse))
                }.asSequence()
            }
            .filter { (_, version) -> testVersion(version.first, version.second) }
            .groupBy { (type, version) -> "$type-${version.first.major}.${version.first.minor}" }
            .mapNotNull {
                it.value.maxByOrNull { (_, version) ->
                    version.first.patch
                }?.let { (type, version) -> "$type-${version.first.asRelease()}" }
            }
            .distinct()
            .toList()

        val androidStudioResult = when (types.get().contains(AndroidStudio)) {
            true -> androidStudioReleases.flatMap { release ->
                release.items
                    .asSequence()
                    .filter { item ->
                        val version = item.platformVersion?.let(Version::parse)
                        val build = item.platformBuild?.let(Version::parse)
                        testVersion(version, build)
                    }
                    .filter { channels.contains(Channel.valueOf(it.channel.uppercase())) }
                    .groupBy { it.version.split('.').dropLast(1).joinToString(".") }
                    .mapNotNull { entry ->
                        entry.value.maxByOrNull {
                            it.version.split('.').last().toInt()
                        }
                    }
                    .map { "$AndroidStudio-${it.version}" }
                    .toList()
            }

            false -> emptyList()
        }

        outputFile.asPath.outputStream().use { os ->
            (result + androidStudioResult)
                .joinToString("\n")
                .apply {
                    os.write(toByteArray())
                }
        }
    }

    private fun Version.asRelease() = "$major.$minor" + (".$patch".takeIf { patch > 0 }.orEmpty())

    enum class Channel {
        EAP, MILESTONE, BETA, RELEASE, CANARY, PATCH, RC,
    }

    companion object {
        fun register(project: Project) =
            project.registerTask<ListProductsReleasesTask>(Tasks.LIST_PRODUCTS_RELEASES) {
                val downloadIdeaProductReleasesXmlTaskProvider =
                    project.tasks.named<DownloadIdeaProductReleasesXmlTask>(Tasks.DOWNLOAD_IDEA_PRODUCT_RELEASES_XML)
                val downloadAndroidStudioProductReleasesXmlTaskProvider =
                    project.tasks.named<DownloadAndroidStudioProductReleasesXmlTask>(Tasks.DOWNLOAD_ANDROID_STUDIO_PRODUCT_RELEASES_XML)
                val patchPluginXmlTaskProvider = project.tasks.named<PatchPluginXmlTask>(Tasks.PATCH_PLUGIN_XML)

                ideaProductReleasesUpdateFiles.from(downloadIdeaProductReleasesXmlTaskProvider.map {
                    it.outputs.files.asFileTree
                })
                androidStudioProductReleasesUpdateFiles.from(downloadAndroidStudioProductReleasesXmlTaskProvider.map {
                    it.outputs.files.asFileTree
                })
                outputFile.convention(
                    project.layout.buildDirectory.file("${Tasks.LIST_PRODUCTS_RELEASES}.txt")
                )
                types.convention(project.provider {
                    listOf(platformType)
                })
                sinceBuild.convention(patchPluginXmlTaskProvider.flatMap {
                    it.sinceBuild
                })
                untilBuild.convention(patchPluginXmlTaskProvider.flatMap {
                    it.untilBuild
                })
                releaseChannels.convention(EnumSet.allOf(Channel::class.java))

                dependsOn(downloadIdeaProductReleasesXmlTaskProvider)
                dependsOn(downloadAndroidStudioProductReleasesXmlTaskProvider)
                dependsOn(patchPluginXmlTaskProvider)
            }
    }
}
