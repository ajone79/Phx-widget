# Getting this on Google Play

1. Create a Google Play developer account (25 USD once, ID check): https://play.google.com/console
2. Make a signing key (one time, keep the file and passwords safe, losing it means you cannot update the app):
   keytool -genkeypair -v -keystore release.jks -alias phx -keyalg RSA -keysize 2048 -validity 10000
3. In this GitHub repo: Settings > Secrets and variables > Actions > add four secrets:
   RELEASE_KEYSTORE_BASE64 (the release.jks file as base64), RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS (phx), RELEASE_KEY_PASSWORD
4. Actions tab > "Play Store bundle" > Run workflow. Download the .aab from the run.
5. Play Console > Create app. Paste the text from store/listing.md. Upload store/graphics/icon-512.png and store/graphics/feature-1024x500.png, plus 2 to 8 phone screenshots.
6. Set the privacy policy URL, content rating, target audience and data safety (answers are in store/listing.md).
7. Closed testing: create a closed test, upload the .aab, add at least 12 testers (Google Groups or email list), and keep them opted in for 14 days (required for new personal accounts).
8. After 14 days apply for production access, then submit for review.
