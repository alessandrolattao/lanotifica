srpm:
	# Fetch tags (shallow clone may not have them)
	git fetch --tags 2>/dev/null || true

	# Get version from git tag
	$(eval VERSION := $(shell git describe --tags --exact-match HEAD 2>/dev/null | sed 's/^v//' || echo "0.0.0"))

	# Setup rpmbuild directories
	mkdir -p $(outdir)
	mkdir -p /tmp/rpmbuild/{SOURCES,SPECS}

	# Create source tarball from current directory
	tar --transform "s,^,lanotifica-$(VERSION)/," \
	    -czf /tmp/rpmbuild/SOURCES/lanotifica-$(VERSION).tar.gz \
	    --exclude='.git' --exclude='.copr' \
	    .

	# Build SRPM
	rpmbuild -bs \
	    --define "_topdir /tmp/rpmbuild" \
	    --define "_srcrpmdir $(outdir)" \
	    --define "version $(VERSION)" \
	    packaging/rpm/lanotifica-copr.spec
