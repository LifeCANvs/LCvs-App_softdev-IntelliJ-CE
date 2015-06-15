/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 21-Mar-2008
 */
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.dataFlow.DataFlowInspection;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.IdeaTestUtil;
import org.jetbrains.annotations.NotNull;

public class ReplaceWithOfNullableFixTest extends LightQuickFixParameterizedTestCase {
  @NotNull
  @Override
  protected LocalInspectionTool[] configureLocalInspectionTools() {
    return new LocalInspectionTool[]{new DataFlowInspection()};
  }

  public void test() throws Exception {
     doAllTests();
   }

  @Override
  protected String getBasePath() {
    return "/codeInsight/daemonCodeAnalyzer/quickFix/replaceWithOfNullable";
  }

  @Override
  protected void beforeActionStarted(String testName, String contents) {
    if (testName.contains("Guava")) {
      ReplaceFromOfNullableFixTest.addGuavaOptional(myTestRootDisposable);
    }
    super.beforeActionStarted(testName, contents);
  }

  @Override
  protected void afterActionCompleted(String testName, String contents) {
    ReplaceFromOfNullableFixTest.cleanupGuava();
    super.afterActionCompleted(testName, contents);
  }

  @Override
  protected Sdk getProjectJDK() {
    return IdeaTestUtil.getMockJdk18();
  }
}