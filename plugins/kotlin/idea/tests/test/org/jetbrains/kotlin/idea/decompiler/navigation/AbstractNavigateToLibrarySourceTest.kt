/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.LinkedHashMap

abstract class AbstractNavigateToLibrarySourceTest : KotlinCodeInsightTestCase() {

    protected fun doTest(path: String): Unit = doTestEx(path)

    protected fun doWithJSModuleTest(path: String): Unit = doTestEx(path) {
        val jsModule = this.createModule("js-module")
        jsModule.configureAs(ModuleKind.KOTLIN_JAVASCRIPT)
    }

    protected fun doTestEx(path: String, additionalConfig: (() -> Unit)? = null) {
        configureByFile(path)
        module.configureAs(getProjectDescriptor())

        if (additionalConfig != null) {
            additionalConfig()
        }

        checkAnnotatedLibraryCode(false)
        checkAnnotatedLibraryCode(true)
    }

    override fun tearDown() {
        SourceNavigationHelper.setForceResolve(false)
        super.tearDown()
    }

    override fun getTestDataPath(): String =
            KotlinTestUtils.getHomeDirectory() + File.separator

    private fun checkAnnotatedLibraryCode(forceResolve: Boolean) {
        SourceNavigationHelper.setForceResolve(forceResolve)
        val actualCode = NavigationTestUtils.getNavigateElementsText(project, collectInterestingNavigationElements())
        val expectedCode = getExpectedAnnotatedLibraryCode()
        UsefulTestCase.assertSameLines(expectedCode, actualCode)
    }

    private fun collectInterestingReferences(): Collection<KtReference> {
        val psiFile = file
        val referenceContainersToReferences = LinkedHashMap<PsiElement, KtReference>()
        for (offset in 0..psiFile.textLength - 1) {
            val ref = psiFile.findReferenceAt(offset)
            val refs = when (ref) {
                is KtReference -> listOf(ref)
                is PsiMultiReference -> ref.references.filterIsInstance<KtReference>()
                else -> emptyList<KtReference>()
            }

            refs.forEach { referenceContainersToReferences.addReference(it) }
        }
        return referenceContainersToReferences.values
    }

    private fun MutableMap<PsiElement, KtReference>.addReference(ref: KtReference) {
        if (containsKey(ref.element)) return
        val target = ref.resolve() ?: return

        val targetNavPsiFile = target.navigationElement.containingFile ?: return

        val targetNavFile = targetNavPsiFile.virtualFile ?: return

        if (ProjectFileIndex.SERVICE.getInstance(project).isInLibrarySource(targetNavFile)) {
            put(ref.element, ref)
        }
    }

    private fun collectInterestingNavigationElements() =
            collectInterestingReferences().map {
                val target = it.resolve()
                TestCase.assertNotNull(target)
                target!!.navigationElement
            }

    private fun getExpectedAnnotatedLibraryCode(): String {
        val document = getDocument(file)
        TestCase.assertNotNull(document)
        return KotlinTestUtils.getLastCommentedLines(document)
    }

    private fun getProjectDescriptor(): KotlinLightProjectDescriptor =
            JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/library", true)
}