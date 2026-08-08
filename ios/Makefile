# Convenience wrapper around install.sh / xcodegen.
# Requires macOS + Xcode. `brew install xcodegen` if you don't have it.

.PHONY: project open build clean

project:        ## Generate ClaudeUsage.xcodeproj from project.yml
	xcodegen generate

open: project   ## Generate and open in Xcode
	open ClaudeUsage.xcodeproj

build:          ## Build & install to a connected device (set TEAM=XXXXXXXXXX)
	./install.sh --build --team $(TEAM)

clean:          ## Remove the generated project
	rm -rf ClaudeUsage.xcodeproj
