srpm:
	# Get version from spec file
	$(eval VERSION := $(shell grep '^Version:' packaging/rpm/lanotifica-copr.spec | awk '{print $$2}'))

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
	    packaging/rpm/lanotifica-copr.spec
