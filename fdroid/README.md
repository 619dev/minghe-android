# F-Droid submission

This directory contains the proposed [fdroiddata recipe](metadata/app.fm619.minghe.yml) for `app.fm619.minghe`. The application repository provides eight localized store listings and v1.0.3 release notes under [Fastlane](../fastlane/metadata/android). The English and Chinese listings include a launcher icon and phone screenshots. The recipe uses the existing `linphone-sdk` entry in fdroiddata, pins its source commit, and builds an unsigned release APK with R8 enabled. F-Droid supplies the distribution signature.

1. Publish the reviewed source in the public upstream repository and create the `v1.0.3` tag at that revision. Confirm the tag resolves before running the recipe.
2. Copy `metadata/app.fm619.minghe.yml` into an up-to-date fdroiddata checkout. From that checkout, run `fdroid readmeta` and `fdroid lint app.fm619.minghe`.
3. Run `fdroid build --server --test app.fm619.minghe:10003` in a configured F-Droid build environment. Check the scanner output, then verify SIP registration, both media modes, and theme switching on the APK from that build.
4. Submit the metadata through the [F-Droid submission process](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

The release recipe does not use the locally modified prebuilt Linphone used in earlier Android debug APKs. It fetches the pinned Linphone sources, removes optional Firebase integration, and builds from source with NDK r27. Node dependencies come from the committed npm lockfile. The recipe does not include scanner bypasses or bundled signing credentials.

The source build requires access to Linphone's GitLab submodules. The public release tag, F-Droid build-server run, and submission remain pending. The existing screenshots show the account and dial-pad screens. They predate the appearance selector.
