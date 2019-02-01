package com.sksamuel.kt.extensions.system

import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.system.SystemEnvironmentExtension
import io.kotlintest.extensions.system.withEnvironment
import io.kotlintest.inspectors.forAll
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.specs.ShouldSpec

class SystemEnvironmentExtensionFunctionsTest : FreeSpec() {
  
  init {
    "The system environment configured with a custom value" - {
      "Should contain the custom variable" - {
        val allResults = executeOnAllSystemEnvironmentOverloads("foo", "bar") {
          System.getenv("foo") shouldBe "bar"
          "RETURNED"
        }
        
        allResults.forAll { it shouldBe "RETURNED" }
      }
    }
    
    "The system environment already with a specified value" - {
      "Should become null when I set it to null" - {
        val allResults = executeOnAllSystemEnvironmentOverloads("PATH", null) {
          System.getenv("PATH") shouldBe null
          "RETURNED"
        }
        
        allResults.forAll { it shouldBe "RETURNED" }
      }
    }
  }
  
  private lateinit var originalPath: String
  
  override fun beforeTest(testCase: TestCase) {
    originalPath = System.getenv("PATH")
  }
  
  override fun afterTest(testCase: TestCase, result: TestResult) {
    verifyFooIsUnset()
    verifyPathIs(originalPath)
  }
  
  private suspend fun FreeSpecScope.executeOnAllSystemEnvironmentOverloads(key: String, value: String?, block: suspend () -> String): List<String> {
    val results = mutableListOf<String>()
    
    "String String overload" {
      results += withEnvironment(key, value, block)
    }
    
    "Pair overload" {
      results += withEnvironment(key to value, block)
    }
    
    "Map overload" {
      results += withEnvironment(mapOf(key to value), block)
    }
    
    return results
  }
}

class SystemEnvironmentExtensionTest : ShouldSpec() {
  
  override fun extensions() = listOf(SystemEnvironmentExtension("foo", "bar"))
  
  init {
    should("Get extra extension from environment") {
      System.getenv("foo") shouldBe "bar"
    }
  }
  
  override fun afterSpec(spec: Spec) {
    // The environment must be reset afterwards
    verifyFooIsUnset()
  }
}

private fun verifyFooIsUnset() {
  System.getenv("foo") shouldBe null
}

private fun verifyPathIs(originalPath: String) {
  System.getenv("PATH") shouldBeSameInstanceAs originalPath
}