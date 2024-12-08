build:
	../gradlew :ctf-rewrite:build

debug:
	/c/Users/N/.jdks/jbr-17.0.5-x64-b469/bin/java.exe \
	-XX:+AllowEnhancedClassRedefinition -XX:HotswapAgent=fatjar -Xlog:redefine+class*=info \
	--add-opens java.base/java.time=ALL-UNNAMED \
	--add-opens java.base/java.util=ALL-UNNAMED \
	--add-opens java.base/java.lang=ALL-UNNAMED \
	-jar paper-1.8.8-445.jar