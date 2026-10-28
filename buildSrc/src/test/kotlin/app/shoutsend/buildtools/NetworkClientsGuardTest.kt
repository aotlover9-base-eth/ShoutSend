package app.shoutsend.buildtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NetworkClientsGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun flagsHttpUrlConnection() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/Test.kt")
        file.writeText("""
            package test
            import java.net.HttpURLConnection
            fun test() {}
        """.trimIndent())

        val violations = NetworkClientsGuard.scan(listOf(srcDir))
        assertEquals(1, violations.size)
        assertTrue(violations[0].detail.contains("HttpURLConnection"))
    }

    @Test
    fun flagsKtorAndRetrofit() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/Client.kt")
        file.writeText("""
            package test
            import io.ktor.client.HttpClient
            import retrofit2.Retrofit
        """.trimIndent())

        val violations = NetworkClientsGuard.scan(listOf(srcDir))
        assertEquals(2, violations.size)
    }

    @Test
    fun flagsWebView() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/Web.kt")
        file.writeText("""
            package test
            import android.webkit.WebView
        """.trimIndent())

        val violations = NetworkClientsGuard.scan(listOf(srcDir))
        assertEquals(1, violations.size)
        assertTrue(violations[0].detail.contains("WebView"))
    }

    @Test
    fun flagsDynamicInetAddress() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/Dns.kt")
        file.writeText("""
            package test
            import java.net.InetAddress
            fun lookup(host: String) {
                InetAddress.getByName(host)
            }
        """.trimIndent())

        val violations = NetworkClientsGuard.scan(listOf(srcDir))
        assertEquals(1, violations.size)
        assertTrue(violations[0].detail.contains("non-literal"))
    }

    @Test
    fun allowsLiteralInetAddress() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/Dns.kt")
        file.writeText("""
            package test
            import java.net.InetAddress
            fun lookup() {
                InetAddress.getByName("192.168.49.1")
            }
        """.trimIndent())

        val violations = NetworkClientsGuard.scan(listOf(srcDir))
        assertEquals(0, violations.size)
    }

    @Test
    fun flagsSocketOutsideTransferPackage() {
        val srcDir = tempFolder.newFolder("src")
        val file = tempFolder.newFile("src/NotTransfer.kt")
        file.writeText("""
            package test
            import java.net.Socket
            val s = Socket()
        """.trimIndent())

        val violations = NetworkClientsGuard.scan(listOf(srcDir))
        assertEquals(2, violations.size) // import + usage
    }

    @Test
    fun allowsSocketInsideTransferPackage() {
        val srcDir = tempFolder.newFolder("src")
        tempFolder.newFolder("src/core", "transfer")
        val file = tempFolder.newFile("src/core/transfer/TransferSocket.kt")
        file.writeText("""
            package app.shoutsend.core.transfer
            import java.net.Socket
            import java.net.ServerSocket
            val s: Socket? = null
            val ss: ServerSocket? = null
        """.trimIndent())

        val violations = NetworkClientsGuard.scan(listOf(srcDir))
        assertEquals(0, violations.size)
    }
}
