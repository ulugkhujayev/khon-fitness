package dev.mirzohidkhon.khonfitness.update

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import dev.mirzohidkhon.khonfitness.MainActivity

/**
 * Status receiver for the self-update install session. The system starts this activity:
 * once when it needs the user's confirmation, and again when the new version is installed.
 * That second start is what relaunches the app after an update.
 */
class UpdateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); handle(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }

    private fun handle(intent: Intent?) {
        Log.i("KhonUpdate", "status=" + intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, -99) + " msg=" + intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        when (intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION") val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) runCatching { startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { Log.w("KhonUpdate", "confirm failed", it) }
                finish()
            }
            PackageInstaller.STATUS_SUCCESS -> {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
            else -> {
                val msg = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Update failed"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        }
    }
}
