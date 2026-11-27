package dev.mirzohidkhon.khonfitness

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class BackupOffTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val domains = setOf("root", "file", "database", "sharedpref", "external", "device_root", "device_file", "device_database", "device_sharedpref")

    @Test fun `Android makes no backup copy of the app data`() {
        assertEquals(0, app.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test fun `the cloud backup and the device transfer leave out every domain`() {
        val excluded = mutableMapOf<String, MutableSet<String>>()
        val xml = app.resources.getXml(R.xml.data_extraction_rules)
        var section = ""
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.eventType != XmlPullParser.START_TAG) continue
            when (xml.name) {
                "cloud-backup", "device-transfer" -> section = xml.name
                "exclude" -> if (xml.getAttributeValue(null, "path") == ".") excluded.getOrPut(section) { mutableSetOf() } += xml.getAttributeValue(null, "domain")
                "include" -> throw AssertionError("an include rule in $section")
            }
        }
        assertEquals(mapOf("cloud-backup" to domains, "device-transfer" to domains), excluded)
    }
}
