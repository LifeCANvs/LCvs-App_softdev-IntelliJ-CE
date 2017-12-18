/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.idea.devkit.kotlin.inspections

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.idea.devkit.inspections.UElementAsPsiInspection

class KtUElementAsPsiInspectionTest : LightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()

    myFixture.addClass("package org.jetbrains.uast; public interface UElement {}")
    myFixture.addClass("""package com.intellij.psi; public interface PsiElement {
      | PsiElement getParent();
      |}""".trimMargin())
    myFixture.addClass("package com.intellij.psi; public interface PsiClass extends PsiElement {}")
    myFixture.addClass("""package org.jetbrains.uast; public interface UClass extends UElement, com.intellij.psi.PsiClass {
      | void uClassMethod();
      |}""".trimMargin())

    myFixture.enableInspections(UElementAsPsiInspection())
  }

  fun testPassAsPsiClass() {
    myFixture.configureByText("UastUsage.kt", """
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.PsiClass;

      class UastUsage {

          fun processUClassCaller(uClass: UClass){
             processUClass(uClass);
          }

          fun processUClass(uClass: UClass){ processPsiClass(<warning descr="Usage of UElement-s as PsiElement-s is deprecated">uClass</warning>); }

          fun processPsiClass(psiClass: PsiClass){ psiClass.toString() }

      }
    """.trimIndent())
    myFixture.testHighlighting("UastUsage.kt")

  }

  fun testAssignment() {
    myFixture.configureByText("UastUsage.kt", """
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.PsiClass;

      class UastUsage {

          var psiClass:PsiClass;

          constructor(uClass: UClass){
             psiClass = <warning descr="Usage of UElement-s as PsiElement-s is deprecated">uClass</warning>;
             val localPsiClass:PsiClass = <warning descr="Usage of UElement-s as PsiElement-s is deprecated">uClass</warning>;
             localPsiClass.toString()
          }

      }
    """.trimIndent())
    myFixture.testHighlighting("UastUsage.kt")

  }

  fun testCallParentMethod() {
    myFixture.configureByText("UastUsage.kt", """
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.PsiClass;

      class UastUsage {

          fun foo(uClass: UClass){
             uClass.<warning descr="Usage of UElement-s as PsiElement-s is deprecated">getParent()</warning>;
             uClass.uClassMethod();
          }

      }
    """.trimIndent())
    myFixture.testHighlighting("UastUsage.kt")

  }

  fun testImplementAndCallParentMethod() {
    myFixture.configureByText("UastUsage.kt", """
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.*;

      class UastUsage {

          fun foo(){
            val impl = UClassImpl();
            impl.<warning descr="Usage of UElement-s as PsiElement-s is deprecated">getParent()</warning>;
            impl.uClassMethod();
          }

      }

      class UClassImpl: UClass {
        override fun getParent():PsiClass? = null
        override fun uClassMethod(){  }
      }

    """.trimIndent())
    myFixture.testHighlighting("UastUsage.kt")

  }

}