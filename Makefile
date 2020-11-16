install:
	./pack-patch
	./gradlew installDebug

release:
	./pack-patch
	./gradlew release
