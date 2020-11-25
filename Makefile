install:
	./pack-patch
	./gradlew installDebug

release:
	./pack-patch
	./gradlew build
	cp ./build/outputs/apk/release/BeatGenerator-release.apk .
