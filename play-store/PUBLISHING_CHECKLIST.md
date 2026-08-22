# Nirmalam Chant publishing checklist

## Before creating the bundle

- [ ] Create and securely back up an upload key. Do not share its passwords or commit it.
- [ ] Copy `keystore.properties.example` to `keystore.properties` and enter the upload-key values.
- [ ] Replace `versionCode` / `versionName` for every Play upload.
- [ ] Build `:app:bundleRelease` and upload the signed `.aab`, not the debug APK.
- [ ] Install and test the release build on a physical phone, including microphone permission, start/stop listening, manual count, a plan, and a reminder.

## Play Console declarations

- [ ] Enroll in Play App Signing and protect the upload key.
- [ ] Publish `PRIVACY_POLICY.md` at an active public HTTPS URL; link it in Play Console and from the in-app Settings page.
- [ ] Data safety: declare no data collected or shared, provided the released build remains local-only.
- [ ] App access: no special login instructions required.
- [ ] Ads: declare no ads.
- [ ] Content rating: complete the questionnaire accurately.
- [ ] Target audience: select the intended age groups; do not select Families unless the app is deliberately prepared for that program.
- [ ] Complete the microphone permission declaration: it is used for user-initiated on-device chant counting; audio is not retained or transmitted.

## Store listing

- [ ] Copy the supplied short and full descriptions from `PLAY_LISTING.md`.
- [ ] Upload the 512 × 512 icon and feature graphic.
- [ ] Add current screenshots from a release build.
- [ ] Start with an internal test track, then closed/open testing, before production.
