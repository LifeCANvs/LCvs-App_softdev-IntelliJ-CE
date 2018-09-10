/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.ide.konan.decompiler.KotlinNativeLoadingMetadataCache
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.library.MetadataReader
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultPackageFragmentsFactory
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.storage.StorageManager

const val KOTLIN_NATIVE_CURRENT_ABI_VERSION = 1

fun createFileStub(project: Project, text: String): PsiFileStub<*> {
    val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, text)
    virtualFile.language = KotlinLanguage.INSTANCE
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile)

    val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val file = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, false, false)!!
    return KtStubElementTypes.FILE.builder.buildStubTree(file) as PsiFileStub<*>
}

fun createLoggingErrorReporter(log: Logger) = LoggingErrorReporter(log)

fun LibraryInfo.createPackageFragmentProviderForLibraryModule(
    storageManager: StorageManager,
    languageVersionSettings: LanguageVersionSettings,
    moduleDescriptor: ModuleDescriptor
): List<PackageFragmentProvider> {

    if (this.platform != KonanPlatform) return emptyList()

    val libraryRoots = this.getLibraryRoots()

    return libraryRoots.mapNotNull { libraryRoot -> File(libraryRoot).takeIf { it.exists } }.map { libraryRoot ->
        val konanLibrary =
            createKonanLibrary(
                libraryRoot,
                KOTLIN_NATIVE_CURRENT_ABI_VERSION,
                metadataReader = CachingIdeMetadataReaderImpl
            )
        konanLibrary.createPackageFragmentProvider(storageManager, languageVersionSettings, moduleDescriptor)
    }
}

private fun KonanLibrary.createPackageFragmentProvider(
    storageManager: StorageManager,
    languageVersionSettings: LanguageVersionSettings,
    moduleDescriptor: ModuleDescriptor
): PackageFragmentProvider {

    val library = this

    val libraryProto = library.moduleHeaderData

    //TODO: Is it required somehow?
    //val moduleName = Name.special(libraryProto.moduleName)
    //val moduleOrigin = DeserializedKonanModuleOrigin(library)

    val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    return DefaultPackageFragmentsFactory.createPackageFragmentProvider(
        library,
        null,
        libraryProto.packageFragmentNameList,
        storageManager,
        moduleDescriptor,
        deserializationConfiguration
    )
}

internal object CachingIdeMetadataReaderImpl : MetadataReader {

    override fun loadSerializedModule(libraryLayout: KonanLibraryLayout): KonanProtoBuf.LinkDataLibrary =
        cache.getCachedModuleHeader(libraryLayout.moduleHeaderFile.virtualFile)

    override fun loadSerializedPackageFragment(
        libraryLayout: KonanLibraryLayout,
        packageFqName: String
    ): KonanProtoBuf.LinkDataPackageFragment =
        cache.getCachedPackageFragment(libraryLayout.packageFragmentFile(packageFqName).virtualFile)

    private val File.virtualFile: VirtualFile
        get() = LocalFileSystem.getInstance().refreshAndFindFileByPath(path) ?: error("File not found: $path")

    private val cache
        get() = KotlinNativeLoadingMetadataCache.getInstance()
}
