package io.kotlintest.extensions.system

import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TestCaseExtension
import java.lang.reflect.Field

suspend fun <T> withEnvironment(key: String, value: String?, block: suspend () -> T): T {
  return withEnvironment(key to value, block)
}

suspend fun <T> withEnvironment(environment: Pair<String, String?>, block: suspend () -> T): T {
  return withEnvironment(mapOf(environment), block)
}

suspend fun <T> withEnvironment(environment: Map<String, String?>, block: suspend () -> T): T {
  val originalEnvironment = System.getenv().toMap() // Using to map to guarantee it's not modified
  
  setEnvironmentMap(originalEnvironment overridenWith environment)
  
  try {
    return block()
  } finally {
    setEnvironmentMap(originalEnvironment)
  }
}

private infix fun Map<String,String>.overridenWith(map: Map<String, String?>): MutableMap<String, String> {
  return toMutableMap().apply { putReplacingNulls(map) }
}

// Implementation inspired from https://github.com/stefanbirkner/system-rule
private fun setEnvironmentMap(map: Map<String, String?>) {
  val envMapOfVariables = getEditableMapOfVariables()
  val caseInsensitiveEnvironment = getCaseInsensitiveEnvironment()
  
  envMapOfVariables.clear()
  caseInsensitiveEnvironment?.clear()
  
  envMapOfVariables.putReplacingNulls(map)
  caseInsensitiveEnvironment?.putReplacingNulls(map)
}

private fun getEditableMapOfVariables(): MutableMap<String, String> {
  val systemEnv = System.getenv()
  val classOfMap = systemEnv::class.java
  
  return classOfMap.getDeclaredField("m").asAccessible().get(systemEnv) as MutableMap<String, String>
}

private fun getCaseInsensitiveEnvironment(): MutableMap<String, String>? {
  val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
  
  return try {
    processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment").asAccessible().get(null) as MutableMap<String, String>?
  } catch (e: NoSuchFieldException) {
    // Only available in Windows, ok to return null if it's not found
    null
  }
}

private fun  Field.asAccessible(): Field {
  return apply { isAccessible = true }
}

private fun MutableMap<String,String>.putReplacingNulls(map: Map<String, String?>) {
  map.forEach { key, value ->
    if(value == null) remove(key) else put(key, value)
  }
}

class SystemEnvironmentExtension(private val environment: Map<String, String>) : TestCaseExtension {
  
  constructor(key: String, value: String) : this(key to value)
  constructor(environment: Pair<String, String>) : this(mapOf(environment))
  
  
  override suspend fun intercept(
          testCase: TestCase,
          execute: suspend (TestCase, suspend (TestResult) -> Unit) -> Unit,
          complete: suspend (TestResult) -> Unit
  ) {
    withEnvironment(environment) {
      execute(testCase, complete)
    }
  }
}