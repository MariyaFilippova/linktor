package io.github.linktor

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinktorPluginTest {

    private fun projectDir(buildScript: String): File {
        val dir = File.createTempFile("linktor-test", "").apply {
            delete()
            mkdirs()
        }
        dir.resolve("settings.gradle").writeText("")
        dir.resolve("build.gradle").writeText(buildScript)
        return dir
    }

    private fun runner(projectDir: File, vararg args: String) =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args)

    @Test
    fun `plugin registers checkLinks task`() {
        val dir = projectDir("""
            plugins { id 'io.github.linktor' }
            linktor { url = 'https://example.com' }
        """.trimIndent())

        val result = runner(dir, "tasks", "--group=verification").build()
        assertTrue(result.output.contains("checkLinks"), "checkLinks task should be listed")
    }

    @Test
    fun `checkLinks fails when url is not set`() {
        val dir = projectDir("""
            plugins { id 'io.github.linktor' }
        """.trimIndent())

        val result = runner(dir, "checkLinks").buildAndFail()
        assertEquals(TaskOutcome.FAILED, result.task(":checkLinks")?.outcome)
        assertTrue(result.output.contains("url"), "Error should mention 'url'")
    }

    @Test
    fun `extension defaults are applied`() {
        val dir = projectDir(
            $$"""
            plugins { id 'io.github.linktor' }

            tasks.named('checkLinks') {
                doFirst {
                    assert maxDepth == 5 : "expected maxDepth=5, got $maxDepth"
                    assert concurrency == 15 : "expected concurrency=15, got $concurrency"
                    assert timeoutMs == 5000L : "expected timeoutMs=5000, got $timeoutMs"
                    assert checkExternal == false
                    assert failOnBroken == false
                    assert maxCrawlTimeMs == 0L
                    assert ignorePaths == []
                    println 'DEFAULTS_OK'
                    throw new org.gradle.api.tasks.StopExecutionException('checked')
                }
            }

            linktor { url = 'https://example.com' }
        """.trimIndent())

        val result = runner(dir, "checkLinks").build()
        assertTrue(result.output.contains("DEFAULTS_OK"), "Default values should match CrawlConfig defaults")
    }

    @Test
    fun `extension properties are configurable`() {
        val dir = projectDir("""
            plugins { id 'io.github.linktor' }

            linktor {
                url = 'https://example.com'
                maxDepth = 2
                concurrency = 5
                timeoutMs = 1000
                checkExternal = true
                failOnBroken = true
                maxCrawlTimeMs = 10000
                ignorePaths = ['/skip*']
            }

            tasks.named('checkLinks') {
                doFirst {
                    assert url == 'https://example.com'
                    assert maxDepth == 2
                    assert concurrency == 5
                    assert timeoutMs == 1000L
                    assert checkExternal == true
                    assert failOnBroken == true
                    assert maxCrawlTimeMs == 10000L
                    assert ignorePaths == ['/skip*']
                    println 'CONFIG_OK'
                    throw new org.gradle.api.tasks.StopExecutionException('checked')
                }
            }
        """.trimIndent())

        val result = runner(dir, "checkLinks").build()
        assertTrue(result.output.contains("CONFIG_OK"), "Extension properties should be wired to task")
    }
}
