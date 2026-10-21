/**
 * Health Connect write permissions for debug builds only (the Kotlin app's src/debug/AndroidManifest.xml):
 * the Import screen's "Debug: write a test run" needs them, and a release build must not declare them.
 */
const fs = require('fs');
const path = require('path');
const { withDangerousMod } = require('expo/config-plugins');

const PERMISSIONS = ['WRITE_EXERCISE', 'WRITE_HEART_RATE', 'WRITE_DISTANCE'].map((p) => `android.permission.health.${p}`);

module.exports = function withDebugHealthWrite(config) {
  return withDangerousMod(config, [
    'android',
    async (cfg) => {
      const file = path.join(cfg.modRequest.platformProjectRoot, 'app', 'src', 'debug', 'AndroidManifest.xml');
      let xml = fs.readFileSync(file, 'utf8');
      const missing = PERMISSIONS.filter((p) => !xml.includes(`"${p}"`));
      if (missing.length > 0) {
        const lines = missing.map((p) => `    <uses-permission android:name="${p}"/>`).join('\n');
        xml = xml.replace(/(<manifest[^>]*>)/, `$1\n\n${lines}`);
        fs.writeFileSync(file, xml);
      }
      return cfg;
    },
  ]);
};
